import { type SupabaseClient } from "npm:@supabase/supabase-js@2";

export type GenerationOutcome = "success" | "refusal" | "providers_exhausted";

export type GenerationOperation = "generate-note" | "suggest-words";

export type GenerationEvent = {
  userId: string;
  operation: GenerationOperation;
  cacheKey: string | null;
  provider: string | null;
  model: string | null;
  cached: boolean;
  outcome: GenerationOutcome;
};

type QueryResult = { data: unknown; error: { message: string } | null };

export async function recordGenerationEvent(
  client: SupabaseClient,
  event: GenerationEvent,
): Promise<void> {
  try {
    const { error }: QueryResult = await client.from("generation_events")
      .insert({
        user_id: event.userId,
        operation: event.operation,
        cache_key: event.cacheKey,
        provider: event.provider,
        model: event.model,
        cached: event.cached,
        outcome: event.outcome,
      });
    if (error !== null) {
      console.error("EventWriteFailure");
    }
  } catch (_error: unknown) {
    console.error("EventWriteFailure");
  }
}
