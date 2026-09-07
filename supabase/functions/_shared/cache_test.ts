import { assertEquals, assertNotEquals } from "jsr:@std/assert@^1";
import { type SupabaseClient } from "npm:@supabase/supabase-js@2";
import {
  buildCacheKey,
  cacheExpiry,
  commitNoteCache,
  type NoteCacheRow,
  refusalExpiresAt,
} from "./cache.ts";
import {
  type GenerateNoteRequest,
  generateNoteRequestSchema,
  type LearningNoteResponse,
} from "./schema.ts";

const NOW: Date = new Date("2026-09-07T10:00:00.000Z");

function request(overrides: Record<string, unknown>): GenerateNoteRequest {
  return generateNoteRequestSchema.parse({
    input_type: "Phrase",
    user_text: "give up",
    ...overrides,
  });
}

type QueryReply = { data: unknown; error: { message: string } | null };

function row(key: string): Omit<NoteCacheRow, "hits"> {
  const response: LearningNoteResponse = { success: false };
  return {
    cache_key: key,
    response,
    success: false,
    provider: "gemini",
    model: "flash-lite",
    prompt_version: 1,
    schema_version: 1,
    expires_at: null,
  };
}

function storeClient(
  store: Map<string, unknown>,
  calls: string[],
): SupabaseClient {
  let deleting: boolean = false;
  const builder: Record<string, unknown> = {
    upsert(entry: Record<string, unknown>): unknown {
      calls.push("upsert:" + String(entry.cache_key));
      store.set(String(entry.cache_key), entry);
      return builder;
    },
    delete(): unknown {
      calls.push("delete");
      deleting = true;
      return builder;
    },
    eq(column: string, value: unknown): unknown {
      calls.push("eq:" + column + ":" + String(value));
      if (deleting && column === "cache_key") {
        store.delete(String(value));
        deleting = false;
      }
      return builder;
    },
    then(resolve: (value: QueryReply) => unknown): unknown {
      return resolve({ data: null, error: null });
    },
  };
  return {
    from(table: string): unknown {
      calls.push("from:" + table);
      return builder;
    },
  } as unknown as SupabaseClient;
}

Deno.test("the key ignores casing and surrounding whitespace", async () => {
  const messy: string = await buildCacheKey(
    request({ user_text: "  Give   Up " }),
  );
  const clean: string = await buildCacheKey(request({ user_text: "give up" }));
  assertEquals(messy, clean);
});

Deno.test("a different learning goal produces a different key", async () => {
  const both: string = await buildCacheKey(request({ learning_goal: "Both" }));
  const recognition: string = await buildCacheKey(
    request({ learning_goal: "RecognitionOnly" }),
  );
  assertNotEquals(both, recognition);
});

Deno.test("previous_issues never changes the key", async () => {
  const first: string = await buildCacheKey(request({}));
  const retry: string = await buildCacheKey(
    request({
      previous_issues: [{ code: "missing_ipa", field: "ipa" }],
    }),
  );
  assertEquals(first, retry);
});

Deno.test("the key is a hex sha256 digest", async () => {
  const key: string = await buildCacheKey(request({}));
  assertEquals(key.length, 64);
  assertEquals(/^[0-9a-f]{64}$/.test(key), true);
});

Deno.test("a successful note never expires", () => {
  assertEquals(cacheExpiry(true, NOW), null);
});

Deno.test("a refusal expires twenty four hours later", () => {
  assertEquals(cacheExpiry(false, NOW), "2026-09-08T10:00:00.000Z");
  assertEquals(refusalExpiresAt(NOW), "2026-09-08T10:00:00.000Z");
});

Deno.test("a first attempt writes the shared entry", async () => {
  const store: Map<string, unknown> = new Map();
  const calls: string[] = [];

  await commitNoteCache(
    storeClient(store, calls),
    request({}),
    row("key-1"),
  );

  assertEquals(store.has("key-1"), true);
  assertEquals(calls, ["from:note_cache", "upsert:key-1"]);
});

Deno.test("a retry deletes the shared entry instead of overwriting it", async () => {
  const store: Map<string, unknown> = new Map([["key-1", { hits: 0 }]]);
  const calls: string[] = [];

  await commitNoteCache(
    storeClient(store, calls),
    request({
      previous_issues: [{ code: "missing_ipa", field: "ipa" }],
    }),
    row("key-1"),
  );

  assertEquals(store.has("key-1"), false);
  assertEquals(calls, ["from:note_cache", "delete", "eq:cache_key:key-1"]);
});
