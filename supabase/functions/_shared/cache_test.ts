import { assertEquals, assertNotEquals } from "jsr:@std/assert@^1";
import { buildCacheKey, cacheExpiry, refusalExpiresAt } from "./cache.ts";
import {
  type GenerateNoteRequest,
  generateNoteRequestSchema,
} from "./schema.ts";

const NOW: Date = new Date("2026-09-07T10:00:00.000Z");

function request(overrides: Record<string, unknown>): GenerateNoteRequest {
  return generateNoteRequestSchema.parse({
    input_type: "Phrase",
    user_text: "give up",
    ...overrides,
  });
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
