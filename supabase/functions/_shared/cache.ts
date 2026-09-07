import { type SupabaseClient } from "npm:@supabase/supabase-js@2";
import { PROMPT_VERSION } from "./prompt.ts";
import {
  type GenerateNoteRequest,
  type LearningNoteResponse,
  SCHEMA_VERSION,
} from "./schema.ts";

export type NoteCacheRow = {
  cache_key: string;
  response: LearningNoteResponse;
  success: boolean;
  provider: string;
  model: string;
  prompt_version: number;
  schema_version: number;
  hits: number;
  expires_at: string | null;
};

type QueryResult = { data: unknown; error: { message: string } | null };

const REFUSAL_TTL_MS: number = 86_400_000;

export async function buildCacheKey(
  request: GenerateNoteRequest,
): Promise<string> {
  const canonical: string = JSON.stringify({
    user_text: request.user_text.toLowerCase(),
    input_type: request.input_type,
    learning_goal: request.learning_goal,
    level_band: request.level_band,
    register: request.register,
    domain: request.domain,
    intended_meaning_es: request.intended_meaning_es,
    context_sentence: request.context_sentence,
    communicative_intent_id: request.communicative_intent_id,
    prompt_version: PROMPT_VERSION,
    schema_version: SCHEMA_VERSION,
  });
  const digest: ArrayBuffer = await crypto.subtle.digest(
    "SHA-256",
    new TextEncoder().encode(canonical),
  );
  return Array.from(new Uint8Array(digest))
    .map((byte: number): string => byte.toString(16).padStart(2, "0"))
    .join("");
}

export function refusalExpiresAt(now: Date): string {
  return new Date(now.getTime() + REFUSAL_TTL_MS).toISOString();
}

export function cacheExpiry(success: boolean, now: Date): string | null {
  return success ? null : refusalExpiresAt(now);
}

export async function readNoteCache(
  client: SupabaseClient,
  key: string,
): Promise<NoteCacheRow | null> {
  try {
    const { data, error }: QueryResult = await client.rpc("note_cache_hit", {
      p_key: key,
    });
    if (error !== null) {
      console.error("CacheReadFailure");
      return null;
    }
    const rows: NoteCacheRow[] = Array.isArray(data)
      ? data as NoteCacheRow[]
      : [];
    return rows.length === 0 ? null : rows[0];
  } catch (_error: unknown) {
    console.error("CacheReadFailure");
    return null;
  }
}

export async function writeNoteCache(
  client: SupabaseClient,
  row: Omit<NoteCacheRow, "hits">,
): Promise<void> {
  try {
    const { error }: QueryResult = await client.from("note_cache").upsert(
      { ...row, hits: 0 },
      { onConflict: "cache_key" },
    );
    if (error !== null) {
      console.error("CacheWriteFailure");
    }
  } catch (_error: unknown) {
    console.error("CacheWriteFailure");
  }
}
