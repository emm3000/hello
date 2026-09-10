import { assertEquals } from "jsr:@std/assert@^1";
import { type SupabaseClient } from "npm:@supabase/supabase-js@2";
import { type NoteCacheRow } from "../_shared/cache.ts";
import {
  type Consumption,
  type CreditsSnapshot,
  CreditsUnavailableError,
  type GenerationEvent,
  snapshot,
} from "../_shared/credits.ts";
import { type ResponseMeta } from "../_shared/envelope.ts";
import {
  type GenerationResult,
  ProvidersExhaustedError,
} from "../_shared/providers.ts";
import { type LearningNoteResponse } from "../_shared/schema.ts";
import { type GenerateNoteDeps, handle } from "./index.ts";

const NOW: Date = new Date("2026-09-10T10:00:00.000Z");
const NEXT_MIDNIGHT: string = "2026-09-11T00:00:00.000Z";
const ALLOWANCE: number = 50;

type Envelope = {
  success: boolean;
  data: unknown;
  error:
    | { code?: string; message?: string; reset_at?: string }
    | null;
  meta: ResponseMeta | null;
};

function generatedNote(): LearningNoteResponse {
  return {
    success: true,
    data: { note_id: "generated" },
  } as unknown as LearningNoteResponse;
}

function cacheHit(): NoteCacheRow {
  return {
    cache_key: "cache-key",
    response: {
      success: true,
      data: { note_id: "cached" },
    } as unknown as LearningNoteResponse,
    success: true,
    provider: "gemini",
    model: "flash-lite",
    prompt_version: 2,
    schema_version: 1,
    hits: 1,
    expires_at: null,
  };
}

function baseDeps(
  calls: string[],
  overrides: Partial<GenerateNoteDeps> = {},
): GenerateNoteDeps {
  const deps: GenerateNoteDeps = {
    createSupabaseContext: ((): Promise<unknown> =>
      Promise.resolve({
        data: {
          supabaseAdmin: {} as unknown as SupabaseClient,
          userClaims: { id: "user-1", role: "authenticated" },
        },
        error: null,
      })) as unknown as GenerateNoteDeps["createSupabaseContext"],
    verifyAppCheckToken: (): Promise<string> => {
      calls.push("verifyAppCheckToken");
      return Promise.resolve("app-1");
    },
    readNoteCache: (): Promise<NoteCacheRow | null> => {
      calls.push("readNoteCache");
      return Promise.resolve(null);
    },
    writeNoteCache: (): Promise<void> => {
      calls.push("writeNoteCache");
      return Promise.resolve();
    },
    generateStructured: <T>(): Promise<GenerationResult<T>> => {
      calls.push("generateStructured");
      return Promise.resolve({
        value: generatedNote() as unknown as T,
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
    readCredits: (): Promise<CreditsSnapshot> => {
      calls.push("readCredits");
      return Promise.resolve(snapshot(ALLOWANCE, 3, NOW));
    },
    recordGenerationEvent: (
      _client: SupabaseClient,
      event: GenerationEvent,
    ): Promise<void> => {
      calls.push("event:" + event.outcome + ":" + String(event.cached));
      return Promise.resolve();
    },
    readDailyAllowance: (): number => ALLOWANCE,
    now: (): Date => NOW,
    env: (name: string): string | undefined =>
      name === "FIREBASE_PROJECT_NUMBER" ? "123456789" : undefined,
  };
  return { ...deps, ...overrides };
}

function noteRequest(
  previousIssues: { code: string; field: string }[] = [],
): Request {
  return new Request("https://hello.test/generate-note", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-Firebase-AppCheck": "app-check-token",
    },
    body: JSON.stringify({
      input_type: "Word",
      user_text: "give up",
      previous_issues: previousIssues,
    }),
  });
}

Deno.test("the credit is taken before the provider is called, once", async () => {
  const calls: string[] = [];
  const response: Response = await handle(noteRequest(), baseDeps(calls));
  const body: Envelope = await response.json();

  assertEquals(response.status, 200);
  assertEquals(body.success, true);
  assertEquals(
    calls.indexOf("consumeCredit") < calls.indexOf("generateStructured"),
    true,
  );
  assertEquals(
    calls.filter((call: string): boolean => call === "consumeCredit").length,
    1,
  );
  assertEquals(body.meta?.credits_remaining, 42);
  assertEquals(body.meta?.cached, false);
  assertEquals(body.meta?.provider, "groq");
});

Deno.test("a cache hit costs no credit and calls no provider", async () => {
  const calls: string[] = [];
  const response: Response = await handle(
    noteRequest(),
    baseDeps(calls, {
      readNoteCache: (): Promise<NoteCacheRow | null> => {
        calls.push("readNoteCache");
        return Promise.resolve(cacheHit());
      },
    }),
  );
  const body: Envelope = await response.json();

  assertEquals(response.status, 200);
  assertEquals(calls.includes("consumeCredit"), false);
  assertEquals(calls.includes("generateStructured"), false);
  assertEquals(calls.includes("event:success:true"), true);
  assertEquals(body.meta?.cached, true);
  assertEquals(body.meta?.credits_remaining, 47);
  assertEquals(body.meta?.provider, "gemini");
});

Deno.test("a cache hit is served even when the balance cannot be read", async () => {
  const calls: string[] = [];
  const response: Response = await handle(
    noteRequest(),
    baseDeps(calls, {
      readNoteCache: (): Promise<NoteCacheRow | null> =>
        Promise.resolve(cacheHit()),
      readCredits: (): Promise<CreditsSnapshot> =>
        Promise.reject(new CreditsUnavailableError("down")),
    }),
  );
  const body: Envelope = await response.json();

  assertEquals(response.status, 200);
  assertEquals(body.meta?.credits_remaining, null);
  assertEquals(body.meta?.reset_at, NEXT_MIDNIGHT);
  assertEquals(body.meta?.cached, true);
});

Deno.test("an exhausted day is refused before the provider", async () => {
  const calls: string[] = [];
  const response: Response = await handle(
    noteRequest(),
    baseDeps(calls, {
      consumeCredit: (): Promise<Consumption> => {
        calls.push("consumeCredit");
        return Promise.resolve({
          consumed: false,
          credits: snapshot(ALLOWANCE, ALLOWANCE, NOW),
        });
      },
    }),
  );
  const body: Envelope = await response.json();

  assertEquals(response.status, 402);
  assertEquals(body.error?.code, "credits_exhausted");
  assertEquals(body.error?.reset_at, NEXT_MIDNIGHT);
  assertEquals(body.meta?.credits_remaining, 0);
  assertEquals(calls.includes("generateStructured"), false);
  assertEquals(
    calls.some((call: string): boolean => call.startsWith("event:")),
    false,
  );
});

Deno.test("an unreadable ledger fails closed with an unknown balance", async () => {
  const calls: string[] = [];
  const response: Response = await handle(
    noteRequest(),
    baseDeps(calls, {
      consumeCredit: (): Promise<Consumption> =>
        Promise.reject(new CreditsUnavailableError("down")),
    }),
  );
  const body: Envelope = await response.json();

  assertEquals(response.status, 503);
  assertEquals(body.error?.code, "credits_unavailable");
  assertEquals(body.meta, null);
  assertEquals(calls.includes("generateStructured"), false);
});

Deno.test("an exhausted provider chain keeps the credit spent", async () => {
  const calls: string[] = [];
  const response: Response = await handle(
    noteRequest(),
    baseDeps(calls, {
      generateStructured: <T>(): Promise<GenerationResult<T>> => {
        calls.push("generateStructured");
        return Promise.reject(new ProvidersExhaustedError(60));
      },
    }),
  );
  const body: Envelope = await response.json();

  assertEquals(response.status, 503);
  assertEquals(body.error?.code, "providers_exhausted");
  assertEquals(response.headers.get("Retry-After"), "60");
  assertEquals(body.meta?.credits_remaining, 42);
  assertEquals(
    calls.filter((call: string): boolean => call === "consumeCredit").length,
    1,
  );
  assertEquals(calls.includes("event:providers_exhausted:false"), true);
});

Deno.test("an unexpected provider failure keeps the credit spent", async () => {
  const calls: string[] = [];
  const response: Response = await handle(
    noteRequest(),
    baseDeps(calls, {
      generateStructured: <T>(): Promise<GenerationResult<T>> =>
        Promise.reject(new Error("socket hang up")),
    }),
  );
  const body: Envelope = await response.json();

  assertEquals(response.status, 500);
  assertEquals(body.error?.code, "internal");
  assertEquals(body.meta?.credits_remaining, 42);
  assertEquals(calls.includes("event:error:false"), true);
});

Deno.test("a retry with previous issues never reads the cache", async () => {
  const calls: string[] = [];
  const response: Response = await handle(
    noteRequest([{ code: "missing_usage_pattern", field: "usage_pattern" }]),
    baseDeps(calls),
  );

  assertEquals(response.status, 200);
  assertEquals(calls.includes("readNoteCache"), false);
  assertEquals(calls.includes("consumeCredit"), true);
  assertEquals(calls.includes("writeNoteCache"), true);
});
