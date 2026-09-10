import { type SupabaseClient } from "npm:@supabase/supabase-js@2";

export type GenerationOutcome =
  | "success"
  | "refusal"
  | "providers_exhausted"
  | "credits_exhausted"
  | "error";

export type GenerationOperation = "generate-note" | "suggest-words";

export type CreditsSnapshot = {
  allowance: number;
  used: number;
  remaining: number;
  resetAt: string;
};

export const DEFAULT_DAILY_ALLOWANCE: number = 5;

export const CREDITS_UNAVAILABLE_RETRY_AFTER_SECONDS: number = 30;

export const MAX_DAILY_ALLOWANCE: number = 2_147_483_647;

const DAY_MS: number = 86_400_000;

const DIGITS_ONLY: RegExp = /^\d+$/;

export type GenerationEvent = {
  userId: string;
  operation: GenerationOperation;
  cacheKey: string | null;
  provider: string | null;
  model: string | null;
  cached: boolean;
  outcome: GenerationOutcome;
};

export type ConsumeInput = {
  userId: string;
  now: Date;
  allowance: number;
};

export type Consumption = {
  consumed: boolean;
  credits: CreditsSnapshot;
};

type QueryResult = { data: unknown; error: { message: string } | null };

type ConsumptionRow = { consumed: unknown; used: unknown };

export class CreditsUnavailableError extends Error {
  constructor(reason: string) {
    super(reason);
    this.name = "CreditsUnavailableError";
  }
}

function creditsUnavailable(
  failure: string,
  reason: string,
): CreditsUnavailableError {
  console.error(JSON.stringify({ event: failure, reason }));
  return new CreditsUnavailableError(reason);
}

export function readDailyAllowance(
  env: (name: string) => string | undefined = Deno.env.get,
): number {
  const raw: string | undefined = env("DAILY_ALLOWANCE");
  if (raw === undefined) {
    return DEFAULT_DAILY_ALLOWANCE;
  }
  const trimmed: string = raw.trim();
  if (!DIGITS_ONLY.test(trimmed)) {
    return DEFAULT_DAILY_ALLOWANCE;
  }
  const parsed: number = Number(trimmed);
  if (parsed > MAX_DAILY_ALLOWANCE) {
    return DEFAULT_DAILY_ALLOWANCE;
  }
  return parsed;
}

function startOfUtcDay(now: Date): Date {
  return new Date(
    Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate()),
  );
}

export function utcDay(now: Date): string {
  return startOfUtcDay(now).toISOString().slice(0, 10);
}

export function nextUtcMidnight(now: Date): Date {
  return new Date(startOfUtcDay(now).getTime() + DAY_MS);
}

export function snapshot(
  allowance: number,
  used: number,
  now: Date,
): CreditsSnapshot {
  return {
    allowance,
    used,
    remaining: Math.max(allowance - used, 0),
    resetAt: nextUtcMidnight(now).toISOString(),
  };
}

export async function consumeCredit(
  client: SupabaseClient,
  input: ConsumeInput,
): Promise<Consumption> {
  let result: QueryResult;
  try {
    result = await client.rpc("consume_generation", {
      p_user_id: input.userId,
      p_day: utcDay(input.now),
      p_allowance: input.allowance,
    });
  } catch (_error: unknown) {
    throw creditsUnavailable(
      "CreditsConsumeFailure",
      "consume_generation failed",
    );
  }
  if (result.error !== null) {
    throw creditsUnavailable(
      "CreditsConsumeFailure",
      "consume_generation failed",
    );
  }
  if (!Array.isArray(result.data) || result.data.length !== 1) {
    throw creditsUnavailable(
      "CreditsConsumeFailure",
      "consume_generation returned no row",
    );
  }
  const row: ConsumptionRow = result.data[0] as ConsumptionRow;
  if (typeof row.consumed !== "boolean" || typeof row.used !== "number") {
    throw creditsUnavailable(
      "CreditsConsumeFailure",
      "consume_generation returned an unusable row",
    );
  }
  return {
    consumed: row.consumed,
    credits: snapshot(input.allowance, row.used, input.now),
  };
}

export async function readCredits(
  client: SupabaseClient,
  userId: string,
  now: Date,
  allowance: number,
): Promise<CreditsSnapshot> {
  let result: QueryResult;
  try {
    result = await client
      .from("daily_usage")
      .select("used")
      .eq("user_id", userId)
      .eq("day", utcDay(now))
      .maybeSingle();
  } catch (_error: unknown) {
    throw creditsUnavailable("CreditsReadFailure", "the credits read failed");
  }
  if (result.error !== null) {
    throw creditsUnavailable("CreditsReadFailure", "the credits read failed");
  }
  const row: Record<string, unknown> | null =
    result.data === null || result.data === undefined
      ? null
      : result.data as Record<string, unknown>;
  const used: number = row !== null && typeof row.used === "number"
    ? row.used
    : 0;
  return snapshot(allowance, used, now);
}

export async function recordGenerationEvent(
  client: SupabaseClient,
  event: GenerationEvent,
): Promise<void> {
  try {
    const result: QueryResult = await client.from("generation_events").insert({
      user_id: event.userId,
      operation: event.operation,
      cache_key: event.cacheKey,
      provider: event.provider,
      model: event.model,
      cached: event.cached,
      outcome: event.outcome,
    });
    if (result.error !== null) {
      console.error(
        JSON.stringify({
          event: "GenerationEventWriteFailure",
          reason: result.error.message,
        }),
      );
    }
  } catch (_error: unknown) {
    console.error(
      JSON.stringify({
        event: "GenerationEventWriteFailure",
        reason: "the event write threw",
      }),
    );
  }
}
