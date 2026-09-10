import { assertEquals, assertNotEquals } from "jsr:@std/assert@^1";
import { type SupabaseClient } from "npm:@supabase/supabase-js@2";
import {
  buildCacheKey,
  cacheExpiry,
  type NoteCacheRow,
  refusalExpiresAt,
  writeNoteCache,
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
  const builder: Record<string, unknown> = {
    upsert(entry: Record<string, unknown>): unknown {
      calls.push("upsert:" + String(entry.cache_key));
      store.set(String(entry.cache_key), entry);
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

  await writeNoteCache(storeClient(store, calls), row("key-1"));

  assertEquals(store.has("key-1"), true);
  assertEquals(calls, ["from:note_cache", "upsert:key-1"]);
});

Deno.test("a retry with previous issues upserts under the same key as the base request", async () => {
  const store: Map<string, unknown> = new Map();
  const calls: string[] = [];
  const client: SupabaseClient = storeClient(store, calls);

  const baseKey: string = await buildCacheKey(request({}));
  const retryKey: string = await buildCacheKey(
    request({
      previous_issues: [{ code: "missing_ipa", field: "ipa" }],
    }),
  );
  assertEquals(baseKey, retryKey);

  await writeNoteCache(client, row(baseKey));
  await writeNoteCache(client, row(retryKey));

  assertEquals(store.has(baseKey), true);
  assertEquals(calls, [
    "from:note_cache",
    "upsert:" + baseKey,
    "from:note_cache",
    "upsert:" + baseKey,
  ]);
});
