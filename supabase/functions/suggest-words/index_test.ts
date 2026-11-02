import { assertEquals, assertStringIncludes } from "jsr:@std/assert@^1";
import { type SupabaseClient } from "npm:@supabase/supabase-js@2";
import {
  type Consumption,
  CreditsUnavailableError,
  type GenerationEvent,
  snapshot,
} from "../_shared/credits.ts";
import { type ResponseMeta } from "../_shared/envelope.ts";
import {
  type GenerateStructuredArgs,
  type GenerationResult,
  ProvidersExhaustedError,
} from "../_shared/providers.ts";
import { type WordSuggestionResponse } from "../_shared/schema.ts";
import { handle, type SuggestWordsDeps } from "./index.ts";

const NOW: Date = new Date("2026-09-10T10:00:00.000Z");
const NEXT_MIDNIGHT: string = "2026-09-11T00:00:00.000Z";
const ALLOWANCE: number = 50;

type SuggestionBody = {
  situation?: string;
  words?: { word: string; translation: string }[];
  error?: { code?: string; reset_at?: string };
  meta: ResponseMeta | null;
};

function suggestions(): WordSuggestionResponse {
  return {
    situation: "Ordering at a coffee shop",
    words: [{ word: "refill", translation: "recarga" }],
  } as unknown as WordSuggestionResponse;
}

function baseDeps(
  calls: string[],
  overrides: Partial<SuggestWordsDeps> = {},
): SuggestWordsDeps {
  const deps: SuggestWordsDeps = {
    createSupabaseContext: ((): Promise<unknown> =>
      Promise.resolve({
        data: {
          supabaseAdmin: {} as unknown as SupabaseClient,
          userClaims: { id: "user-1", role: "authenticated" },
        },
        error: null,
      })) as unknown as SuggestWordsDeps["createSupabaseContext"],
    verifyAppCheckToken: (): Promise<string> => {
      calls.push("verifyAppCheckToken");
      return Promise.resolve("app-1");
    },
    generateStructured: <T>(): Promise<GenerationResult<T>> => {
      calls.push("generateStructured");
      return Promise.resolve({
        value: suggestions() as unknown as T,
        provider: "groq",
        model: "oss",
      });
    },
    consumeCredit: (): Promise<Consumption> => {
      calls.push("consumeCredit");
      return Promise.resolve({
        consumed: true,
        credits: snapshot(ALLOWANCE, 8, NOW),
      });
    },
    recordGenerationEvent: (
      _client: SupabaseClient,
      event: GenerationEvent,
    ): Promise<void> => {
      calls.push("event:" + event.outcome);
      return Promise.resolve();
    },
    readDailyAllowance: (): number => ALLOWANCE,
    now: (): Date => NOW,
    env: (name: string): string | undefined =>
      name === "FIREBASE_PROJECT_NUMBER" ? "123456789" : undefined,
  };
  return { ...deps, ...overrides };
}

function suggestRequest(
  body: Record<string, unknown> = { recent_words: ["give up"] },
): Request {
  return new Request("https://hello.test/suggest-words", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-Firebase-AppCheck": "app-check-token",
    },
    body: JSON.stringify(body),
  });
}

Deno.test("the credit is taken before the provider is called", async () => {
  const calls: string[] = [];
  const response: Response = await handle(suggestRequest(), baseDeps(calls));
  const body: SuggestionBody = await response.json();

  assertEquals(response.status, 200);
  assertEquals(
    calls.indexOf("consumeCredit") < calls.indexOf("generateStructured"),
    true,
  );
  assertEquals(body.situation, "Ordering at a coffee shop");
  assertEquals(body.meta?.credits_remaining, 42);
  assertEquals(body.meta?.reset_at, NEXT_MIDNIGHT);
});

Deno.test("an exhausted day is refused before the provider", async () => {
  const calls: string[] = [];
  const response: Response = await handle(
    suggestRequest(),
    baseDeps(calls, {
      consumeCredit: (): Promise<Consumption> =>
        Promise.resolve({
          consumed: false,
          credits: snapshot(ALLOWANCE, ALLOWANCE, NOW),
        }),
    }),
  );
  const body: SuggestionBody = await response.json();

  assertEquals(response.status, 402);
  assertEquals(body.error?.code, "credits_exhausted");
  assertEquals(body.error?.reset_at, NEXT_MIDNIGHT);
  assertEquals(body.meta?.credits_remaining, 0);
  assertEquals(calls.includes("generateStructured"), false);
});

Deno.test("an exhausted provider chain keeps the credit spent", async () => {
  const calls: string[] = [];
  const response: Response = await handle(
    suggestRequest(),
    baseDeps(calls, {
      generateStructured: <T>(): Promise<GenerationResult<T>> =>
        Promise.reject(new ProvidersExhaustedError(45)),
    }),
  );
  const body: SuggestionBody = await response.json();

  assertEquals(response.status, 503);
  assertEquals(body.error?.code, "providers_exhausted");
  assertEquals(response.headers.get("Retry-After"), "45");
  assertEquals(body.meta?.credits_remaining, 42);
  assertEquals(calls.includes("event:providers_exhausted"), true);
});

Deno.test("an unreadable ledger fails closed with an unknown balance", async () => {
  const calls: string[] = [];
  const response: Response = await handle(
    suggestRequest(),
    baseDeps(calls, {
      consumeCredit: (): Promise<Consumption> =>
        Promise.reject(new CreditsUnavailableError("down")),
    }),
  );
  const body: SuggestionBody = await response.json();

  assertEquals(response.status, 503);
  assertEquals(body.error?.code, "credits_unavailable");
  assertEquals(body.meta, null);
  assertEquals(calls.includes("generateStructured"), false);
});

Deno.test("the provider receives a prompt carrying the sent exclusions", async () => {
  const calls: string[] = [];
  const prompts: string[] = [];
  const response: Response = await handle(
    suggestRequest({
      recent_words: ["give up"],
      excluded_situations: ["Checking in at a hotel"],
      excluded_words: ["room key"],
    }),
    baseDeps(calls, {
      generateStructured: <T>(
        args: GenerateStructuredArgs<T>,
      ): Promise<GenerationResult<T>> => {
        prompts.push(args.prompt);
        return Promise.resolve({
          value: suggestions() as unknown as T,
          provider: "groq",
          model: "oss",
        });
      },
    }),
  );

  assertEquals(response.status, 200);
  assertEquals(prompts.length, 1);
  assertStringIncludes(prompts[0], `["Checking in at a hotel"]`);
  assertStringIncludes(prompts[0], `["room key"]`);
});

async function assertRejectedBeforeCredit(
  body: Record<string, unknown>,
  field: string,
): Promise<void> {
  const calls: string[] = [];
  const response: Response = await handle(
    suggestRequest(body),
    baseDeps(calls),
  );
  const responseBody: { error?: { code?: string; message?: string } } =
    await response.json();

  assertEquals(response.status, 400);
  assertEquals(responseBody.error?.code, "invalid_request");
  assertStringIncludes(responseBody.error?.message ?? "", field);
  assertEquals(calls.includes("consumeCredit"), false);
  assertEquals(calls.includes("generateStructured"), false);
}

Deno.test("31 excluded situations are an invalid request", async () => {
  await assertRejectedBeforeCredit(
    {
      recent_words: [],
      excluded_situations: new Array(31).fill("Ordering at a coffee shop"),
    },
    "excluded_situations",
  );
});

Deno.test("121 excluded words are an invalid request", async () => {
  await assertRejectedBeforeCredit(
    { recent_words: [], excluded_words: new Array(121).fill("refill") },
    "excluded_words",
  );
});

Deno.test("an empty excluded situation is an invalid request", async () => {
  await assertRejectedBeforeCredit(
    { recent_words: [], excluded_situations: [""] },
    "excluded_situations.0",
  );
});
