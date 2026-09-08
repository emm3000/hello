import {
  assertEquals,
  assertRejects,
  assertStringIncludes,
} from "jsr:@std/assert@^1";
import { z } from "npm:zod@^4";
import {
  generateStructured,
  PROVIDER_CHAIN,
  type ProviderConfig,
  ProvidersExhaustedError,
  type ProviderStateStore,
  resetProviderState,
} from "./providers.ts";

type Probe = { ok: string };

type FetchCall = { url: string; body: Record<string, unknown> };

const NOW: Date = new Date("2026-09-07T10:00:00.000Z");

const JSON_SCHEMA: Record<string, unknown> = {
  type: "object",
  properties: { ok: { type: "string" } },
};

function testChain(): ProviderConfig[] {
  return [
    {
      id: "first",
      baseUrl: "https://first.test/v1/",
      apiKeyEnv: "FIRST_KEY",
      model: "first-model",
      outputMode: "json_schema",
      timeoutMs: 1000,
      cooldownAfterRateLimit: (now: Date): Date =>
        new Date(now.getTime() + 3_600_000),
    },
    {
      id: "second",
      baseUrl: "https://second.test/v1/",
      apiKeyEnv: "SECOND_KEY",
      model: "second-model",
      outputMode: "json_object",
      timeoutMs: 1000,
      cooldownAfterRateLimit: (now: Date): Date =>
        new Date(now.getTime() + 3_600_000),
    },
  ];
}

function env(name: string): string | undefined {
  if (name === "FIRST_KEY") return "first-key";
  if (name === "SECOND_KEY") return "second-key";
  return undefined;
}

function completion(content: string): Response {
  return new Response(JSON.stringify({ choices: [{ message: { content } }] }), {
    status: 200,
    headers: { "Content-Type": "application/json" },
  });
}

function failure(status: number, headers?: HeadersInit): Response {
  return new Response("upstream failure", { status, headers });
}

function embeddedError(body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { "Content-Type": "application/json" },
  });
}

function recordingFetch(
  responses: Response[],
  calls: FetchCall[],
): typeof fetch {
  let index = 0;
  return (
    input: string | URL | Request,
    init?: RequestInit,
  ): Promise<Response> => {
    const response: Response | undefined = responses[index];
    index += 1;
    calls.push({
      url: String(input),
      body: JSON.parse(String(init?.body)) as Record<string, unknown>,
    });
    if (response === undefined) {
      return Promise.reject(new Error("unexpected provider call"));
    }
    return Promise.resolve(response);
  };
}

function parseProbe(raw: unknown): Probe {
  const candidate: { ok?: unknown } = raw as { ok?: unknown };
  if (typeof candidate.ok !== "string") {
    throw new Error("probe payload is invalid");
  }
  return { ok: candidate.ok };
}

function run(
  fetchFn: typeof fetch,
  providers: ProviderConfig[],
): Promise<{ value: Probe; provider: string; model: string }> {
  return generateStructured<Probe>({
    prompt: "PROMPT",
    schemaName: "probe",
    jsonSchema: JSON_SCHEMA,
    parse: parseProbe,
    providers,
    fetchFn,
    env,
    now: (): Date => NOW,
  });
}

Deno.test("a rate limited provider is skipped on the next request", async () => {
  resetProviderState();
  const providers: ProviderConfig[] = testChain();
  const firstCalls: FetchCall[] = [];
  const first = await run(
    recordingFetch([failure(429), completion(`{"ok":"yes"}`)], firstCalls),
    providers,
  );
  assertEquals(first.provider, "second");
  assertEquals(first.model, "second-model");
  assertEquals(first.value, { ok: "yes" });
  assertEquals(firstCalls.length, 2);

  const secondCalls: FetchCall[] = [];
  const second = await run(
    recordingFetch([completion(`{"ok":"again"}`)], secondCalls),
    providers,
  );
  assertEquals(second.provider, "second");
  assertEquals(secondCalls.length, 1);
  assertEquals(secondCalls[0].url, "https://second.test/v1/chat/completions");
});

Deno.test("an unavailable provider is retried on the next request", async () => {
  resetProviderState();
  const providers: ProviderConfig[] = testChain();
  const firstCalls: FetchCall[] = [];
  await run(
    recordingFetch([failure(503), completion(`{"ok":"yes"}`)], firstCalls),
    providers,
  );
  const secondCalls: FetchCall[] = [];
  const second = await run(
    recordingFetch([completion(`{"ok":"direct"}`)], secondCalls),
    providers,
  );
  assertEquals(second.provider, "first");
  assertEquals(secondCalls.length, 1);
  assertEquals(secondCalls[0].url, "https://first.test/v1/chat/completions");
});

Deno.test("an unparsable provider payload falls through", async () => {
  resetProviderState();
  const calls: FetchCall[] = [];
  const result = await run(
    recordingFetch(
      [completion(`{"nope":1}`), completion(`{"ok":"yes"}`)],
      calls,
    ),
    testChain(),
  );
  assertEquals(result.provider, "second");
  assertEquals(calls.length, 2);
});

Deno.test("an unauthorized provider falls through", async () => {
  resetProviderState();
  const calls: FetchCall[] = [];
  const result = await run(
    recordingFetch([failure(401), completion(`{"ok":"yes"}`)], calls),
    testChain(),
  );
  assertEquals(result.provider, "second");
  assertEquals(calls.length, 2);
});

Deno.test("a provider without an api key is never called", async () => {
  resetProviderState();
  const calls: FetchCall[] = [];
  const result = await generateStructured<Probe>({
    prompt: "PROMPT",
    schemaName: "probe",
    jsonSchema: JSON_SCHEMA,
    parse: parseProbe,
    providers: testChain(),
    fetchFn: recordingFetch([completion(`{"ok":"yes"}`)], calls),
    env: (name: string): string | undefined =>
      name === "SECOND_KEY" ? "second-key" : undefined,
    now: (): Date => NOW,
  });
  assertEquals(result.provider, "second");
  assertEquals(calls.length, 1);
  assertEquals(calls[0].url, "https://second.test/v1/chat/completions");
});

Deno.test("every provider failing throws with a default retry delay", async () => {
  resetProviderState();
  const calls: FetchCall[] = [];
  const error = await assertRejects(
    () => run(recordingFetch([failure(503), failure(503)], calls), testChain()),
    ProvidersExhaustedError,
  );
  assertEquals(error.retryAfterSeconds, 60);
  assertEquals(calls.length, 2);
});

Deno.test("every provider rate limited throws with the cooldown delay", async () => {
  resetProviderState();
  const calls: FetchCall[] = [];
  const error = await assertRejects(
    () => run(recordingFetch([failure(429), failure(429)], calls), testChain()),
    ProvidersExhaustedError,
  );
  assertEquals(error.retryAfterSeconds, 3600);
});

Deno.test("a retry-after header overrides the provider cooldown", async () => {
  resetProviderState();
  const calls: FetchCall[] = [];
  const error = await assertRejects(
    () =>
      run(
        recordingFetch(
          [failure(429, { "retry-after": "6" }), failure(429)],
          calls,
        ),
        testChain(),
      ),
    ProvidersExhaustedError,
  );
  assertEquals(error.retryAfterSeconds, 6);
  assertEquals(calls.length, 2);
});

Deno.test("a 429 without a retry-after header keeps the provider cooldown", async () => {
  resetProviderState();
  const calls: FetchCall[] = [];
  const error = await assertRejects(
    () => run(recordingFetch([failure(429), failure(429)], calls), testChain()),
    ProvidersExhaustedError,
  );
  assertEquals(error.retryAfterSeconds, 3600);
  assertEquals(calls.length, 2);
});

Deno.test("an unparseable retry-after falls back to the provider cooldown", async () => {
  resetProviderState();
  const calls: FetchCall[] = [];
  const error = await assertRejects(
    () =>
      run(
        recordingFetch(
          [failure(429, { "retry-after": "soon" }), failure(429)],
          calls,
        ),
        testChain(),
      ),
    ProvidersExhaustedError,
  );
  assertEquals(error.retryAfterSeconds, 3600);
  assertEquals(calls.length, 2);
});

Deno.test("a retry-after above the cap is clamped to one day", async () => {
  resetProviderState();
  const marked: { providerId: string; untilMs: number }[] = [];
  const providerState: ProviderStateStore = {
    load: (): Promise<Map<string, number>> =>
      Promise.resolve(new Map<string, number>()),
    markExhausted: (providerId: string, untilMs: number): Promise<void> => {
      marked.push({ providerId, untilMs });
      return Promise.resolve();
    },
  };
  const calls: FetchCall[] = [];
  const result = await generateStructured<Probe>({
    prompt: "PROMPT",
    schemaName: "probe",
    jsonSchema: JSON_SCHEMA,
    parse: parseProbe,
    providers: testChain(),
    fetchFn: recordingFetch(
      [
        failure(429, { "retry-after": "999999" }),
        completion(`{"ok":"yes"}`),
      ],
      calls,
    ),
    env,
    now: (): Date => NOW,
    providerState,
  });
  assertEquals(result.provider, "second");
  assertEquals(marked.length, 1);
  assertEquals(marked[0].providerId, "first");
  assertEquals(marked[0].untilMs, NOW.getTime() + 86_400_000);
});

Deno.test("a zero or negative retry-after keeps the provider cooldown", async () => {
  resetProviderState();
  const zeroCalls: FetchCall[] = [];
  const zero = await assertRejects(
    () =>
      run(
        recordingFetch(
          [failure(429, { "retry-after": "0" }), failure(429)],
          zeroCalls,
        ),
        testChain(),
      ),
    ProvidersExhaustedError,
  );
  assertEquals(zero.retryAfterSeconds, 3600);
  assertEquals(zeroCalls.length, 2);

  resetProviderState();
  const negativeCalls: FetchCall[] = [];
  const negative = await assertRejects(
    () =>
      run(
        recordingFetch(
          [failure(429, { "retry-after": "-5" }), failure(429)],
          negativeCalls,
        ),
        testChain(),
      ),
    ProvidersExhaustedError,
  );
  assertEquals(negative.retryAfterSeconds, 3600);
  assertEquals(negativeCalls.length, 2);
});

Deno.test("an empty, whitespace or infinite retry-after keeps the provider cooldown", async () => {
  for (const value of ["", "   ", "Infinity"]) {
    resetProviderState();
    const calls: FetchCall[] = [];
    const error = await assertRejects(
      () =>
        run(
          recordingFetch(
            [failure(429, { "retry-after": value }), failure(429)],
            calls,
          ),
          testChain(),
        ),
      ProvidersExhaustedError,
    );
    assertEquals(error.retryAfterSeconds, 3600);
    assertEquals(calls.length, 2);
  }
});

Deno.test("an http-date retry-after sets the cooldown", async () => {
  resetProviderState();
  const calls: FetchCall[] = [];
  const error = await assertRejects(
    () =>
      run(
        recordingFetch(
          [
            failure(429, { "retry-after": "Mon, 07 Sep 2026 10:02:00 GMT" }),
            failure(429),
          ],
          calls,
        ),
        testChain(),
      ),
    ProvidersExhaustedError,
  );
  assertEquals(error.retryAfterSeconds, 120);
  assertEquals(calls.length, 2);
});

Deno.test("an http-date retry-after in the past keeps the provider cooldown", async () => {
  resetProviderState();
  const calls: FetchCall[] = [];
  const error = await assertRejects(
    () =>
      run(
        recordingFetch(
          [
            failure(429, { "retry-after": "Mon, 07 Sep 2026 09:59:00 GMT" }),
            failure(429),
          ],
          calls,
        ),
        testChain(),
      ),
    ProvidersExhaustedError,
  );
  assertEquals(error.retryAfterSeconds, 3600);
  assertEquals(calls.length, 2);
});

Deno.test("a fenced json body is accepted", async () => {
  resetProviderState();
  const calls: FetchCall[] = [];
  const result = await run(
    recordingFetch(
      [completion('```json\n{"ok":"fenced"}\n```')],
      calls,
    ),
    testChain(),
  );
  assertEquals(result.value, { ok: "fenced" });
  assertEquals(result.provider, "first");
});

Deno.test("each output mode shapes its own request", async () => {
  resetProviderState();
  const calls: FetchCall[] = [];
  await run(
    recordingFetch([failure(503), completion(`{"ok":"yes"}`)], calls),
    testChain(),
  );
  const strict: Record<string, unknown> = calls[0].body;
  const messages = strict.messages as { role: string; content: string }[];
  assertEquals(messages[0].content, "PROMPT");
  assertEquals(strict.model, "first-model");
  assertEquals(strict.temperature, 0);
  assertEquals(strict.top_p, 0.95);
  assertEquals(strict.response_format, {
    type: "json_schema",
    json_schema: { name: "probe", strict: true, schema: JSON_SCHEMA },
  });

  const loose: Record<string, unknown> = calls[1].body;
  const looseMessages = loose.messages as { role: string; content: string }[];
  assertEquals(loose.response_format, { type: "json_object" });
  assertStringIncludes(
    looseMessages[0].content,
    "PROMPT\n\nRespond with a single JSON object",
  );
  assertEquals(
    looseMessages[0].content.endsWith(JSON.stringify(JSON_SCHEMA)),
    true,
  );
});

Deno.test("the gemini cooldown runs to the next midnight in Los Angeles", () => {
  const gemini: ProviderConfig = PROVIDER_CHAIN[1];
  assertEquals(gemini.id, "gemini");
  assertEquals(gemini.model, "gemini-3.1-flash-lite");
  assertEquals(gemini.outputMode, "json_schema");
  assertEquals(gemini.timeoutMs, 30000);
  assertEquals(
    gemini.cooldownAfterRateLimit(new Date("2026-09-07T10:00:00.000Z"))
      .toISOString(),
    "2026-09-08T07:00:00.000Z",
  );
  assertEquals(
    gemini.cooldownAfterRateLimit(new Date("2026-12-07T10:00:00.000Z"))
      .toISOString(),
    "2026-12-08T08:00:00.000Z",
  );
});

Deno.test("the openrouter cooldown runs for one hour", () => {
  const openrouter: ProviderConfig = PROVIDER_CHAIN[2];
  assertEquals(openrouter.id, "openrouter");
  assertEquals(openrouter.model, "minimax/minimax-m3:free");
  assertEquals(openrouter.outputMode, "json_object");
  assertEquals(openrouter.timeoutMs, 45000);
  assertEquals(
    openrouter.cooldownAfterRateLimit(new Date("2026-09-07T10:00:00.000Z"))
      .toISOString(),
    "2026-09-07T11:00:00.000Z",
  );
});

Deno.test("groq leads the chain and cools down for one minute", () => {
  const groq: ProviderConfig = PROVIDER_CHAIN[0];
  assertEquals(PROVIDER_CHAIN.length, 3);
  assertEquals(groq.id, "groq");
  assertEquals(groq.model, "openai/gpt-oss-120b");
  assertEquals(groq.outputMode, "json_object");
  assertEquals(groq.timeoutMs, 15000);
  assertEquals(
    groq.cooldownAfterRateLimit(new Date("2026-09-07T10:00:00.000Z"))
      .toISOString(),
    "2026-09-07T10:01:00.000Z",
  );
});

Deno.test("a schema failure logs an invalid_output line without the payload", async () => {
  resetProviderState();
  const probeSchema = z.object({ ok: z.string() });
  const calls: FetchCall[] = [];
  const captured: string[] = [];
  const originalLog: (...data: unknown[]) => void = console.log;
  console.log = (...data: unknown[]): void => {
    captured.push(data.map((item: unknown): string => String(item)).join(" "));
  };
  try {
    await generateStructured<{ ok: string }>({
      prompt: "PROMPT",
      schemaName: "probe",
      jsonSchema: JSON_SCHEMA,
      parse: probeSchema.parse,
      providers: testChain(),
      fetchFn: recordingFetch(
        [completion(`{"ok":123}`), completion(`{"ok":"yes"}`)],
        calls,
      ),
      env,
      now: (): Date => NOW,
    });
  } finally {
    console.log = originalLog;
  }

  const events: Record<string, unknown>[] = captured.map(
    (line: string): Record<string, unknown> =>
      JSON.parse(line) as Record<string, unknown>,
  );
  const invalidOutput: Record<string, unknown>[] = events.filter(
    (event: Record<string, unknown>): boolean =>
      event.event === "invalid_output",
  );
  assertEquals(invalidOutput.length, 1);
  assertEquals(invalidOutput[0].provider, "first");
  assertEquals(invalidOutput[0].model, "first-model");
  assertEquals(invalidOutput[0].reason, "schema");
  assertEquals(String(invalidOutput[0].detail).startsWith("ok "), true);
  assertEquals(String(invalidOutput[0].detail).includes("123"), false);
  assertEquals(String(invalidOutput[0].detail).length <= 160, true);
});

Deno.test("a malformed array envelope still yields the first object", async () => {
  resetProviderState();
  const envelopeSchema = z.object({
    success: z.boolean(),
    data: z.object({ ok: z.string() }),
  });
  const malformed: string =
    '[\n{"success":true,"data":{"ok":"yes"}},"error",":null"\n]';
  const calls: FetchCall[] = [];
  const result = await generateStructured<
    { success: boolean; data: { ok: string } }
  >({
    prompt: "PROMPT",
    schemaName: "probe",
    jsonSchema: JSON_SCHEMA,
    parse: envelopeSchema.parse,
    providers: testChain(),
    fetchFn: recordingFetch([completion(malformed)], calls),
    env,
    now: (): Date => NOW,
  });
  assertEquals(result.value, { success: true, data: { ok: "yes" } });
  assertEquals(result.provider, "first");
  assertEquals(calls.length, 1);
});

Deno.test("a malformed array envelope logs an array_envelope_unwrapped line", async () => {
  resetProviderState();
  const envelopeSchema = z.object({
    success: z.boolean(),
    data: z.object({ ok: z.string() }),
  });
  const malformed: string =
    '[\n{"success":true,"data":{"ok":"yes"}},"error",":null"\n]';
  const calls: FetchCall[] = [];
  const captured: string[] = [];
  const originalLog: (...data: unknown[]) => void = console.log;
  console.log = (...data: unknown[]): void => {
    captured.push(data.map((item: unknown): string => String(item)).join(" "));
  };
  try {
    await generateStructured<{ success: boolean; data: { ok: string } }>({
      prompt: "PROMPT",
      schemaName: "probe",
      jsonSchema: JSON_SCHEMA,
      parse: envelopeSchema.parse,
      providers: testChain(),
      fetchFn: recordingFetch([completion(malformed)], calls),
      env,
      now: (): Date => NOW,
    });
  } finally {
    console.log = originalLog;
  }

  const events: Record<string, unknown>[] = captured.map(
    (line: string): Record<string, unknown> =>
      JSON.parse(line) as Record<string, unknown>,
  );
  const unwrapped: Record<string, unknown>[] = events.filter(
    (event: Record<string, unknown>): boolean =>
      event.event === "array_envelope_unwrapped",
  );
  assertEquals(unwrapped.length, 1);
  assertEquals(unwrapped[0].provider, "first");
  assertEquals(unwrapped[0].model, "first-model");
  assertEquals(unwrapped[0].discarded, 2);

  const attempts: Record<string, unknown>[] = events.filter(
    (event: Record<string, unknown>): boolean =>
      event.event === "provider_attempt",
  );
  assertEquals(attempts.length, 1);
  assertEquals(attempts[0].provider, "first");
  assertEquals(attempts[0].outcome, "ok");
});

Deno.test("a json parse failure logs a reason without the provider content", async () => {
  resetProviderState();
  const calls: FetchCall[] = [];
  const captured: string[] = [];
  const originalLog: (...data: unknown[]) => void = console.log;
  console.log = (...data: unknown[]): void => {
    captured.push(data.map((item: unknown): string => String(item)).join(" "));
  };
  try {
    await run(
      recordingFetch(
        [
          completion("no json here, just the learner word pordiosear"),
          completion(`{"ok":"yes"}`),
        ],
        calls,
      ),
      testChain(),
    );
  } finally {
    console.log = originalLog;
  }

  const invalidOutput: Record<string, unknown>[] = captured
    .map((line: string): Record<string, unknown> =>
      JSON.parse(line) as Record<string, unknown>
    )
    .filter((event: Record<string, unknown>): boolean =>
      event.event === "invalid_output"
    );
  assertEquals(invalidOutput.length, 1);
  assertEquals(invalidOutput[0].reason, "json_parse");
  const detail: string = String(invalidOutput[0].detail);
  assertEquals(detail.includes("pordiosear"), false);
  assertEquals(detail.includes("no json"), false);
  assertEquals(detail.includes('"'), true);
  assertEquals(detail.length > 0, true);
  assertEquals(detail.length <= 160, true);
});

Deno.test("a rate limit embedded in a 200 body cools the provider down", async () => {
  resetProviderState();
  const providers: ProviderConfig[] = testChain();
  const firstCalls: FetchCall[] = [];
  const first = await run(
    recordingFetch(
      [
        embeddedError({
          error: {
            code: 429,
            message: "Rate limit exceeded",
            metadata: { error_type: "rate_limit_exceeded" },
          },
        }),
        completion(`{"ok":"yes"}`),
      ],
      firstCalls,
    ),
    providers,
  );
  assertEquals(first.provider, "second");
  assertEquals(first.value, { ok: "yes" });
  assertEquals(firstCalls.length, 2);

  const secondCalls: FetchCall[] = [];
  const second = await run(
    recordingFetch([completion(`{"ok":"again"}`)], secondCalls),
    providers,
  );
  assertEquals(second.provider, "second");
  assertEquals(secondCalls.length, 1);
  assertEquals(secondCalls[0].url, "https://second.test/v1/chat/completions");
});

Deno.test("a rate limit embedded in a choice cools the provider down", async () => {
  resetProviderState();
  const providers: ProviderConfig[] = testChain();
  const firstCalls: FetchCall[] = [];
  const first = await run(
    recordingFetch(
      [
        embeddedError({
          choices: [
            {
              message: { role: "assistant", content: '{"ok":"par' },
              finish_reason: "error",
              error: {
                code: 429,
                message: "Rate limit exceeded",
                metadata: { error_type: "rate_limit_exceeded" },
              },
            },
          ],
        }),
        completion(`{"ok":"yes"}`),
      ],
      firstCalls,
    ),
    providers,
  );
  assertEquals(first.provider, "second");
  assertEquals(first.value, { ok: "yes" });
  assertEquals(firstCalls.length, 2);

  const secondCalls: FetchCall[] = [];
  const second = await run(
    recordingFetch([completion(`{"ok":"again"}`)], secondCalls),
    providers,
  );
  assertEquals(second.provider, "second");
  assertEquals(secondCalls.length, 1);
  assertEquals(secondCalls[0].url, "https://second.test/v1/chat/completions");
});

Deno.test("an embedded rate limit is persisted to the provider state store", async () => {
  resetProviderState();
  const marked: { providerId: string; untilMs: number }[] = [];
  const providerState: ProviderStateStore = {
    load: (): Promise<Map<string, number>> =>
      Promise.resolve(new Map<string, number>()),
    markExhausted: (providerId: string, untilMs: number): Promise<void> => {
      marked.push({ providerId, untilMs });
      return Promise.resolve();
    },
  };
  const calls: FetchCall[] = [];
  const result = await generateStructured<Probe>({
    prompt: "PROMPT",
    schemaName: "probe",
    jsonSchema: JSON_SCHEMA,
    parse: parseProbe,
    providers: testChain(),
    fetchFn: recordingFetch(
      [
        embeddedError({
          error: { code: 429, message: "Rate limit exceeded" },
        }),
        completion(`{"ok":"yes"}`),
      ],
      calls,
    ),
    env,
    now: (): Date => NOW,
    providerState,
  });
  assertEquals(result.provider, "second");
  assertEquals(marked.length, 1);
  assertEquals(marked[0].providerId, "first");
  assertEquals(marked[0].untilMs, NOW.getTime() + 3_600_000);
});

Deno.test("an embedded upstream failure is retried on the next request", async () => {
  resetProviderState();
  const providers: ProviderConfig[] = testChain();
  const firstCalls: FetchCall[] = [];
  const first = await run(
    recordingFetch(
      [
        embeddedError({
          choices: [
            {
              message: { role: "assistant", content: '{"ok":"par' },
              finish_reason: "error",
              error: {
                code: 502,
                message: "Provider disconnected mid-stream",
                metadata: { error_type: "provider_unavailable" },
              },
            },
          ],
        }),
        completion(`{"ok":"yes"}`),
      ],
      firstCalls,
    ),
    providers,
  );
  assertEquals(first.provider, "second");
  assertEquals(firstCalls.length, 2);

  const secondCalls: FetchCall[] = [];
  const second = await run(
    recordingFetch(
      [completion(`{"ok":"direct"}`), completion(`{"ok":"spare"}`)],
      secondCalls,
    ),
    providers,
  );
  assertEquals(second.provider, "first");
  assertEquals(secondCalls[0].url, "https://first.test/v1/chat/completions");
});

Deno.test("an embedded unauthorized code falls through without a cooldown", async () => {
  resetProviderState();
  const providers: ProviderConfig[] = testChain();
  const firstCalls: FetchCall[] = [];
  const first = await run(
    recordingFetch(
      [
        embeddedError({
          error: {
            code: 401,
            message: "No auth credentials found",
            metadata: { error_type: "unauthorized" },
          },
        }),
        completion(`{"ok":"yes"}`),
      ],
      firstCalls,
    ),
    providers,
  );
  assertEquals(first.provider, "second");
  assertEquals(firstCalls.length, 2);

  const secondCalls: FetchCall[] = [];
  const second = await run(
    recordingFetch([completion(`{"ok":"direct"}`)], secondCalls),
    providers,
  );
  assertEquals(second.provider, "first");
  assertEquals(secondCalls[0].url, "https://first.test/v1/chat/completions");
});

Deno.test("an embedded error logs its code as the attempt status", async () => {
  resetProviderState();
  const calls: FetchCall[] = [];
  const captured: string[] = [];
  const originalLog: (...data: unknown[]) => void = console.log;
  console.log = (...data: unknown[]): void => {
    captured.push(data.map((item: unknown): string => String(item)).join(" "));
  };
  try {
    await run(
      recordingFetch(
        [
          embeddedError({
            error: {
              code: 429,
              message: "Rate limit exceeded",
              metadata: { error_type: "rate_limit_exceeded" },
            },
          }),
          completion(`{"ok":"yes"}`),
        ],
        calls,
      ),
      testChain(),
    );
  } finally {
    console.log = originalLog;
  }

  const attempts: Record<string, unknown>[] = captured
    .map((line: string): Record<string, unknown> =>
      JSON.parse(line) as Record<string, unknown>
    )
    .filter((event: Record<string, unknown>): boolean =>
      event.event === "provider_attempt" && event.provider === "first"
    );
  assertEquals(attempts.length, 1);
  assertEquals(attempts[0].status, 429);
  assertEquals(attempts[0].outcome, "rate_limited");
});

Deno.test("a body without an embedded error is unchanged", async () => {
  resetProviderState();
  const calls: FetchCall[] = [];
  const result = await run(
    recordingFetch([completion(`{"ok":"plain"}`)], calls),
    testChain(),
  );
  assertEquals(result.provider, "first");
  assertEquals(result.model, "first-model");
  assertEquals(result.value, { ok: "plain" });
  assertEquals(calls.length, 1);
});
