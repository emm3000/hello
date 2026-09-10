import { type SupabaseClient } from "npm:@supabase/supabase-js@2";

export type GenerationOutcome =
  | "pending"
  | "success"
  | "refusal"
  | "providers_exhausted"
  | "credits_exhausted"
  | "error";

export type GenerationOperation = "generate-note" | "suggest-words";

export type CreditsSnapshot = {
  allowance: number;
  charged: number;
  remaining: number;
  refusalAllowance: number;
  refused: number;
  resetAt: string;
};

export const DEFAULT_DAILY_ALLOWANCE: number = 5;

export const DEFAULT_DAILY_REFUSAL_ALLOWANCE: number = 10;

export const CREDITS_UNAVAILABLE_RETRY_AFTER_SECONDS: number = 30;

export const PENDING_RESERVATION_TTL_MS: number = 300_000;

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

export type Settlement = {
  outcome: "success" | "refusal" | "providers_exhausted" | "error";
  provider: string | null;
  model: string | null;
};

export type Reservation =
  | { reserved: true; eventId: number; credits: CreditsSnapshot }
  | { reserved: false; credits: CreditsSnapshot };

export type ReservationInput = {
  userId: string;
  operation: GenerationOperation;
  cacheKey: string | null;
  now: Date;
  allowance: number;
  refusalAllowance: number;
};

type QueryResult = { data: unknown; error: { message: string } | null };

type ReservationRow = {
  reserved: unknown;
  event_id: unknown;
  charged: unknown;
  refused: unknown;
};

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
  console.error(failure);
  return new CreditsUnavailableError(reason);
}

function readAllowance(
  name: string,
  fallback: number,
  env: (name: string) => string | undefined,
): number {
  const raw: string | undefined = env(name);
  if (raw === undefined || raw.trim().length === 0) {
    return fallback;
  }
  const parsed: number = Number(raw);
  if (!Number.isInteger(parsed) || parsed < 0) {
    return fallback;
  }
  return parsed;
}

export function readDailyAllowance(
  env: (name: string) => string | undefined = Deno.env.get,
): number {
  return readAllowance("DAILY_ALLOWANCE", DEFAULT_DAILY_ALLOWANCE, env);
}

export function readDailyRefusalAllowance(
  env: (name: string) => string | undefined = Deno.env.get,
): number {
  return readAllowance(
    "DAILY_REFUSAL_ALLOWANCE",
    DEFAULT_DAILY_REFUSAL_ALLOWANCE,
    env,
  );
}

export function startOfUtcDay(now: Date): Date {
  return new Date(
    Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate()),
  );
}

export function nextUtcMidnight(now: Date): Date {
  return new Date(startOfUtcDay(now).getTime() + DAY_MS);
}

export function staleBefore(now: Date): Date {
  return new Date(now.getTime() - PENDING_RESERVATION_TTL_MS);
}

export function snapshotFrom(
  allowance: number,
  charged: number,
  refusalAllowance: number,
  refused: number,
  now: Date,
): CreditsSnapshot {
  return {
    allowance,
    charged,
    remaining: Math.max(allowance - charged, 0),
    refusalAllowance,
    refused,
    resetAt: nextUtcMidnight(now).toISOString(),
  };
}

export function remainingAfter(
  credits: CreditsSnapshot,
  outcome: Settlement["outcome"],
): number {
  if (outcome === "success") {
    return credits.remaining;
  }
  return Math.min(credits.remaining + 1, credits.allowance);
}

export async function reserveGeneration(
  client: SupabaseClient,
  input: ReservationInput,
): Promise<Reservation> {
  let result: QueryResult;
  try {
    result = await client.rpc("reserve_generation", {
      p_user_id: input.userId,
      p_operation: input.operation,
      p_cache_key: input.cacheKey,
      p_allowance: input.allowance,
      p_refusal_allowance: input.refusalAllowance,
      p_day_start: startOfUtcDay(input.now).toISOString(),
      p_stale_before: staleBefore(input.now).toISOString(),
    });
  } catch (_error: unknown) {
    throw creditsUnavailable(
      "CreditsReserveFailure",
      "reserve_generation failed",
    );
  }
  if (result.error !== null) {
    throw creditsUnavailable(
      "CreditsReserveFailure",
      "reserve_generation failed",
    );
  }
  if (!Array.isArray(result.data) || result.data.length !== 1) {
    throw creditsUnavailable(
      "CreditsReserveFailure",
      "reserve_generation returned no row",
    );
  }
  const row: ReservationRow = result.data[0] as ReservationRow;
  if (
    typeof row.reserved !== "boolean" || typeof row.charged !== "number" ||
    typeof row.refused !== "number"
  ) {
    throw creditsUnavailable(
      "CreditsReserveFailure",
      "reserve_generation returned an unusable row",
    );
  }
  const credits: CreditsSnapshot = snapshotFrom(
    input.allowance,
    row.charged,
    input.refusalAllowance,
    row.refused,
    input.now,
  );
  if (!row.reserved) {
    return { reserved: false, credits };
  }
  if (typeof row.event_id !== "number") {
    throw creditsUnavailable(
      "CreditsReserveFailure",
      "reserve_generation returned an unusable row",
    );
  }
  return { reserved: true, eventId: row.event_id, credits };
}

export async function settleGeneration(
  client: SupabaseClient,
  eventId: number,
  settlement: Settlement,
): Promise<void> {
  let result: QueryResult;
  try {
    result = await client.from("generation_events").update({
      outcome: settlement.outcome,
      provider: settlement.provider,
      model: settlement.model,
    }).eq("id", eventId);
  } catch (_error: unknown) {
    throw creditsUnavailable("CreditsSettleFailure", "the settle write failed");
  }
  if (result.error !== null) {
    throw creditsUnavailable("CreditsSettleFailure", "the settle write failed");
  }
}

function pendingIsCharged(createdAt: unknown, stale: Date): boolean {
  if (typeof createdAt !== "string") {
    return true;
  }
  const created: number = Date.parse(createdAt);
  if (Number.isNaN(created)) {
    return true;
  }
  return created >= stale.getTime();
}

export async function readCredits(
  client: SupabaseClient,
  userId: string,
  now: Date,
  allowance: number,
  refusalAllowance: number,
): Promise<CreditsSnapshot> {
  let result: QueryResult;
  try {
    result = await client
      .from("generation_events")
      .select("outcome, created_at")
      .eq("user_id", userId)
      .eq("cached", false)
      .gte("created_at", startOfUtcDay(now).toISOString());
  } catch (_error: unknown) {
    throw creditsUnavailable("CreditsReadFailure", "the credits read failed");
  }
  if (result.error !== null) {
    throw creditsUnavailable("CreditsReadFailure", "the credits read failed");
  }
  const rows: Record<string, unknown>[] = Array.isArray(result.data)
    ? result.data as Record<string, unknown>[]
    : [];
  const stale: Date = staleBefore(now);
  let charged: number = 0;
  let refused: number = 0;
  for (const row of rows) {
    const outcome: unknown = row.outcome;
    if (typeof outcome !== "string") {
      continue;
    }
    if (outcome === "success") {
      charged += 1;
    }
    if (outcome === "pending" && pendingIsCharged(row.created_at, stale)) {
      charged += 1;
    }
    if (outcome === "refusal") {
      refused += 1;
    }
  }
  return snapshotFrom(allowance, charged, refusalAllowance, refused, now);
}

export async function recordGenerationEvent(
  client: SupabaseClient,
  event: GenerationEvent,
): Promise<void> {
  let result: QueryResult;
  try {
    result = await client.from("generation_events").insert({
      user_id: event.userId,
      operation: event.operation,
      cache_key: event.cacheKey,
      provider: event.provider,
      model: event.model,
      cached: event.cached,
      outcome: event.outcome,
    });
  } catch (_error: unknown) {
    throw creditsUnavailable("EventWriteFailure", "the event write failed");
  }
  if (result.error !== null) {
    throw creditsUnavailable("EventWriteFailure", "the event write failed");
  }
}
