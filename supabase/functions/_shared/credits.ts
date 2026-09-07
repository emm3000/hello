import { type SupabaseClient } from "npm:@supabase/supabase-js@2";

export type GenerationOutcome =
  | "success"
  | "refusal"
  | "providers_exhausted"
  | "credits_exhausted";

export type GenerationOperation = "generate-note" | "suggest-words";

export type CreditsSnapshot = {
  allowance: number;
  charged: number;
  remaining: number;
  resetAt: string;
};

export const DEFAULT_DAILY_ALLOWANCE: number = 5;

const DAY_MS: number = 86_400_000;

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

type CountResult = { count: number | null; error: { message: string } | null };

export function readDailyAllowance(
  env: (name: string) => string | undefined = Deno.env.get,
): number {
  const raw: string | undefined = env("DAILY_ALLOWANCE");
  if (raw === undefined || raw.trim().length === 0) {
    return DEFAULT_DAILY_ALLOWANCE;
  }
  const parsed: number = Number(raw);
  if (!Number.isInteger(parsed) || parsed < 0) {
    return DEFAULT_DAILY_ALLOWANCE;
  }
  return parsed;
}

export function startOfUtcDay(now: Date): Date {
  return new Date(
    Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate()),
  );
}

export function nextUtcMidnight(now: Date): Date {
  return new Date(startOfUtcDay(now).getTime() + DAY_MS);
}

export function snapshotFrom(
  allowance: number,
  charged: number,
  now: Date,
): CreditsSnapshot {
  return {
    allowance,
    charged,
    remaining: Math.max(allowance - charged, 0),
    resetAt: nextUtcMidnight(now).toISOString(),
  };
}

export async function checkCredits(
  client: SupabaseClient,
  userId: string,
  now: Date,
  allowance: number,
): Promise<CreditsSnapshot> {
  try {
    const { count, error }: CountResult = await client
      .from("generation_events")
      .select("id", { count: "exact", head: true })
      .eq("user_id", userId)
      .eq("outcome", "success")
      .eq("cached", false)
      .gte("created_at", startOfUtcDay(now).toISOString());
    if (error !== null) {
      console.error("CreditsReadFailure");
      return snapshotFrom(allowance, 0, now);
    }
    return snapshotFrom(allowance, count ?? 0, now);
  } catch (_error: unknown) {
    console.error("CreditsReadFailure");
    return snapshotFrom(allowance, 0, now);
  }
}

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
