import {
  assertEquals,
  assertRejects,
  assertStringIncludes,
} from "jsr:@std/assert@^1";
import {
  generateStructured,
  PROVIDER_CHAIN,
  type ProviderConfig,
  ProvidersExhaustedError,
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

function failure(status: number): Response {
  return new Response("upstream failure", { status });
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
  const gemini: ProviderConfig = PROVIDER_CHAIN[0];
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
  const openrouter: ProviderConfig = PROVIDER_CHAIN[1];
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
