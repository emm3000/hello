import { assertEquals } from "jsr:@std/assert@^1";
import { type SupabaseClient } from "npm:@supabase/supabase-js@2";
import {
  type Consumption,
  CreditsUnavailableError,
  type GenerationEvent,
  snapshot,
} from "../_shared/credits.ts";
import { type ResponseMeta } from "../_shared/envelope.ts";
import {
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

function suggestRequest(): Request {
  return new Request("https://hello.test/suggest-words", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-Firebase-AppCheck": "app-check-token",
    },
    body: JSON.stringify({ recent_words: ["give up"] }),
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
