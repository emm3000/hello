import { assertEquals, assertRejects } from "jsr:@std/assert@^1";
import { type SupabaseClient } from "npm:@supabase/supabase-js@2";
import {
  type CreditsSnapshot,
  CreditsUnavailableError,
  DEFAULT_DAILY_ALLOWANCE,
  DEFAULT_DAILY_REFUSAL_ALLOWANCE,
  nextUtcMidnight,
  readCredits,
  readDailyAllowance,
  readDailyRefusalAllowance,
  recordGenerationEvent,
  remainingAfter,
  type Reservation,
  reserveGeneration,
  settleGeneration,
  snapshotFrom,
  startOfUtcDay,
} from "./credits.ts";

const MIDDAY: Date = new Date("2026-09-07T10:00:00.000Z");
const LAST_SECOND: Date = new Date("2026-09-07T23:59:59.000Z");

function envWith(
  value: string | undefined,
): (name: string) => string | undefined {
  return (name: string): string | undefined =>
    name === "DAILY_ALLOWANCE" ? value : undefined;
}

function refusalEnvWith(
  value: string | undefined,
): (name: string) => string | undefined {
  return (name: string): string | undefined =>
    name === "DAILY_REFUSAL_ALLOWANCE" ? value : undefined;
}

type QueryReply = { data: unknown; error: { message: string } | null };

function tableClient(reply: QueryReply, calls: string[]): SupabaseClient {
  const builder: Record<string, unknown> = {
    select(columns: string): unknown {
      calls.push("select:" + columns);
      return builder;
    },
    insert(payload: Record<string, unknown>): unknown {
      calls.push("insert:" + JSON.stringify(payload));
      return builder;
    },
    update(payload: Record<string, unknown>): unknown {
      calls.push("update:" + JSON.stringify(payload));
      return builder;
    },
    eq(column: string, value: unknown): unknown {
      calls.push("eq:" + column + ":" + String(value));
      return builder;
    },
    gte(column: string, value: unknown): unknown {
      calls.push("gte:" + column + ":" + String(value));
      return builder;
    },
    then(resolve: (value: QueryReply) => unknown): unknown {
      return resolve(reply);
    },
  };
  return {
    from(table: string): unknown {
      calls.push("from:" + table);
      return builder;
    },
  } as unknown as SupabaseClient;
}

function rpcClient(reply: QueryReply, calls: string[]): SupabaseClient {
  return {
    rpc(name: string, params: Record<string, unknown>): Promise<QueryReply> {
      calls.push(name + ":" + JSON.stringify(params));
      return Promise.resolve(reply);
    },
  } as unknown as SupabaseClient;
}

function throwingClient(): SupabaseClient {
  return {
    from(_table: string): unknown {
      throw new Error("connection refused");
    },
    rpc(_name: string, _params: Record<string, unknown>): unknown {
      throw new Error("connection refused");
    },
  } as unknown as SupabaseClient;
}

function reservationInput(): Parameters<typeof reserveGeneration>[1] {
  return {
    userId: "user-1",
    operation: "generate-note",
    cacheKey: "cache-key",
    now: MIDDAY,
    allowance: 5,
    refusalAllowance: 10,
  };
}

Deno.test("an unset allowance falls back to the default", () => {
  assertEquals(readDailyAllowance(envWith(undefined)), DEFAULT_DAILY_ALLOWANCE);
  assertEquals(DEFAULT_DAILY_ALLOWANCE, 5);
});

Deno.test("an empty allowance falls back to the default", () => {
  assertEquals(readDailyAllowance(envWith("")), 5);
});

Deno.test("a non integer allowance falls back to the default", () => {
  assertEquals(readDailyAllowance(envWith("abc")), 5);
  assertEquals(readDailyAllowance(envWith("2.5")), 5);
});

Deno.test("a negative allowance falls back to the default", () => {
  assertEquals(readDailyAllowance(envWith("-1")), 5);
});

Deno.test("an integer allowance is honoured", () => {
  assertEquals(readDailyAllowance(envWith("2")), 2);
  assertEquals(readDailyAllowance(envWith(" 20 ")), 20);
});

Deno.test("an unset refusal allowance falls back to the default", () => {
  assertEquals(
    readDailyRefusalAllowance(refusalEnvWith(undefined)),
    DEFAULT_DAILY_REFUSAL_ALLOWANCE,
  );
  assertEquals(DEFAULT_DAILY_REFUSAL_ALLOWANCE, 10);
});

Deno.test("an empty refusal allowance falls back to the default", () => {
  assertEquals(readDailyRefusalAllowance(refusalEnvWith("")), 10);
});

Deno.test("a non integer refusal allowance falls back to the default", () => {
  assertEquals(readDailyRefusalAllowance(refusalEnvWith("abc")), 10);
  assertEquals(readDailyRefusalAllowance(refusalEnvWith("2.5")), 10);
});

Deno.test("a negative refusal allowance falls back to the default", () => {
  assertEquals(readDailyRefusalAllowance(refusalEnvWith("-1")), 10);
});

Deno.test("an integer refusal allowance is honoured", () => {
  assertEquals(readDailyRefusalAllowance(refusalEnvWith("3")), 3);
  assertEquals(readDailyRefusalAllowance(refusalEnvWith(" 40 ")), 40);
});

Deno.test("the utc day starts at midnight for a midday instant", () => {
  assertEquals(startOfUtcDay(MIDDAY).toISOString(), "2026-09-07T00:00:00.000Z");
  assertEquals(
    nextUtcMidnight(MIDDAY).toISOString(),
    "2026-09-08T00:00:00.000Z",
  );
});

Deno.test("the last second of a utc day still belongs to that day", () => {
  assertEquals(
    startOfUtcDay(LAST_SECOND).toISOString(),
    "2026-09-07T00:00:00.000Z",
  );
  assertEquals(
    nextUtcMidnight(LAST_SECOND).toISOString(),
    "2026-09-08T00:00:00.000Z",
  );
});

Deno.test("a snapshot reports what is left of the allowance", () => {
  const snapshot: CreditsSnapshot = snapshotFrom(5, 2, 10, 1, MIDDAY);

  assertEquals(snapshot.allowance, 5);
  assertEquals(snapshot.charged, 2);
  assertEquals(snapshot.remaining, 3);
  assertEquals(snapshot.refusalAllowance, 10);
  assertEquals(snapshot.refused, 1);
  assertEquals(snapshot.resetAt, "2026-09-08T00:00:00.000Z");
});

Deno.test("remaining never falls below zero", () => {
  assertEquals(snapshotFrom(5, 5, 10, 0, MIDDAY).remaining, 0);
  assertEquals(snapshotFrom(5, 9, 10, 0, MIDDAY).remaining, 0);
});

Deno.test("a reservation sends both allowances and the utc day start", async () => {
  const calls: string[] = [];
  await reserveGeneration(
    rpcClient({
      data: [{ reserved: true, event_id: 7, charged: 1, refused: 0 }],
      error: null,
    }, calls),
    reservationInput(),
  );

  assertEquals(calls, [
    'reserve_generation:{"p_user_id":"user-1","p_operation":"generate-note",' +
    '"p_cache_key":"cache-key","p_allowance":5,"p_refusal_allowance":10,' +
    '"p_day_start":"2026-09-07T00:00:00.000Z"}',
  ]);
});

Deno.test("a reserved row carries the event id and the charged snapshot", async () => {
  const calls: string[] = [];
  const reservation: Reservation = await reserveGeneration(
    rpcClient({
      data: [{ reserved: true, event_id: 42, charged: 3, refused: 2 }],
      error: null,
    }, calls),
    reservationInput(),
  );

  assertEquals(reservation.reserved, true);
  assertEquals(reservation.reserved ? reservation.eventId : null, 42);
  assertEquals(reservation.credits.charged, 3);
  assertEquals(reservation.credits.remaining, 2);
  assertEquals(reservation.credits.refused, 2);
  assertEquals(reservation.credits.resetAt, "2026-09-08T00:00:00.000Z");
});

Deno.test("an exhausted user is refused the reservation and keeps the snapshot", async () => {
  const calls: string[] = [];
  const reservation: Reservation = await reserveGeneration(
    rpcClient({
      data: [{ reserved: false, event_id: null, charged: 5, refused: 10 }],
      error: null,
    }, calls),
    reservationInput(),
  );

  assertEquals(reservation.reserved, false);
  assertEquals("eventId" in reservation, false);
  assertEquals(reservation.credits.charged, 5);
  assertEquals(reservation.credits.remaining, 0);
  assertEquals(reservation.credits.refused, 10);
});

Deno.test("a reservation error leaves the credits unavailable", async () => {
  const calls: string[] = [];

  await assertRejects(
    () =>
      reserveGeneration(
        rpcClient({ data: null, error: { message: "boom" } }, calls),
        reservationInput(),
      ),
    CreditsUnavailableError,
  );
});

Deno.test("a reservation call that throws leaves the credits unavailable", async () => {
  await assertRejects(
    () => reserveGeneration(throwingClient(), reservationInput()),
    CreditsUnavailableError,
  );
});

Deno.test("an empty reservation result leaves the credits unavailable", async () => {
  const calls: string[] = [];

  await assertRejects(
    () =>
      reserveGeneration(
        rpcClient({ data: [], error: null }, calls),
        reservationInput(),
      ),
    CreditsUnavailableError,
  );
});

Deno.test("a settlement writes the outcome onto the reserved event", async () => {
  const calls: string[] = [];
  await settleGeneration(
    tableClient({ data: null, error: null }, calls),
    42,
    { outcome: "refusal", provider: "gemini", model: "flash-lite" },
  );

  assertEquals(calls, [
    "from:generation_events",
    'update:{"outcome":"refusal","provider":"gemini","model":"flash-lite"}',
    "eq:id:42",
  ]);
});

Deno.test("a settlement failure leaves the credits unavailable", async () => {
  const calls: string[] = [];

  await assertRejects(
    () =>
      settleGeneration(
        tableClient({ data: null, error: { message: "boom" } }, calls),
        42,
        { outcome: "success", provider: null, model: null },
      ),
    CreditsUnavailableError,
  );
  await assertRejects(
    () =>
      settleGeneration(throwingClient(), 42, {
        outcome: "success",
        provider: null,
        model: null,
      }),
    CreditsUnavailableError,
  );
});

Deno.test("a credits read counts pending and successful events as charged", async () => {
  const calls: string[] = [];
  const snapshot: CreditsSnapshot = await readCredits(
    tableClient({
      data: [
        { outcome: "success" },
        { outcome: "pending" },
        { outcome: "refusal" },
        { outcome: "providers_exhausted" },
        { outcome: 7 },
      ],
      error: null,
    }, calls),
    "user-1",
    MIDDAY,
    5,
    10,
  );

  assertEquals(snapshot.charged, 2);
  assertEquals(snapshot.remaining, 3);
  assertEquals(snapshot.refused, 1);
  assertEquals(calls, [
    "from:generation_events",
    "select:outcome",
    "eq:user_id:user-1",
    "eq:cached:false",
    "gte:created_at:2026-09-07T00:00:00.000Z",
  ]);
});

Deno.test("a credits read failure leaves the credits unavailable", async () => {
  const calls: string[] = [];

  await assertRejects(
    () =>
      readCredits(
        tableClient({ data: null, error: { message: "boom" } }, calls),
        "user-1",
        MIDDAY,
        5,
        10,
      ),
    CreditsUnavailableError,
  );
  await assertRejects(
    () => readCredits(throwingClient(), "user-1", MIDDAY, 5, 10),
    CreditsUnavailableError,
  );
});

Deno.test("an event write failure leaves the credits unavailable", async () => {
  const calls: string[] = [];

  await assertRejects(
    () =>
      recordGenerationEvent(
        tableClient({ data: null, error: { message: "boom" } }, calls),
        {
          userId: "user-1",
          operation: "generate-note",
          cacheKey: "cache-key",
          provider: null,
          model: null,
          cached: true,
          outcome: "success",
        },
      ),
    CreditsUnavailableError,
  );
  await assertRejects(
    () =>
      recordGenerationEvent(throwingClient(), {
        userId: "user-1",
        operation: "generate-note",
        cacheKey: "cache-key",
        provider: null,
        model: null,
        cached: true,
        outcome: "success",
      }),
    CreditsUnavailableError,
  );
});

Deno.test("only a success keeps the reserved credit charged", () => {
  const credits: CreditsSnapshot = snapshotFrom(5, 3, 10, 0, MIDDAY);

  assertEquals(remainingAfter(credits, "success"), 2);
  assertEquals(remainingAfter(credits, "refusal"), 3);
  assertEquals(remainingAfter(credits, "providers_exhausted"), 3);
  assertEquals(remainingAfter(credits, "error"), 3);
  assertEquals(remainingAfter(snapshotFrom(5, 0, 10, 0, MIDDAY), "error"), 5);
});
