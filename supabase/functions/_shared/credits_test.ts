import { assertEquals, assertRejects } from "jsr:@std/assert@^1";
import { type SupabaseClient } from "npm:@supabase/supabase-js@2";
import {
  consumeCredit,
  type Consumption,
  type CreditsSnapshot,
  CreditsUnavailableError,
  DEFAULT_DAILY_ALLOWANCE,
  MAX_DAILY_ALLOWANCE,
  nextUtcMidnight,
  readCredits,
  readDailyAllowance,
  recordGenerationEvent,
  snapshot,
  utcDay,
} from "./credits.ts";

const MIDDAY: Date = new Date("2026-09-07T10:00:00.000Z");
const LAST_SECOND: Date = new Date("2026-09-10T23:59:59.000Z");
const FIRST_SECOND: Date = new Date("2026-09-11T00:00:00.000Z");

function envWith(
  value: string | undefined,
): (name: string) => string | undefined {
  return (name: string): string | undefined =>
    name === "DAILY_ALLOWANCE" ? value : undefined;
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
    eq(column: string, value: unknown): unknown {
      calls.push("eq:" + column + ":" + String(value));
      return builder;
    },
    maybeSingle(): unknown {
      calls.push("maybeSingle");
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

function consumeInput(): Parameters<typeof consumeCredit>[1] {
  return { userId: "user-1", now: MIDDAY, allowance: 5 };
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

Deno.test("an allowance that is not plain digits falls back to the default", () => {
  assertEquals(readDailyAllowance(envWith("1e2")), 5);
  assertEquals(readDailyAllowance(envWith("0x10")), 5);
  assertEquals(readDailyAllowance(envWith("+5")), 5);
});

Deno.test("an allowance beyond the integer column falls back to the default", () => {
  assertEquals(MAX_DAILY_ALLOWANCE, 2_147_483_647);
  assertEquals(readDailyAllowance(envWith("2147483648")), 5);
  assertEquals(readDailyAllowance(envWith("2147483647")), 2_147_483_647);
});

Deno.test("an integer allowance is honoured", () => {
  assertEquals(readDailyAllowance(envWith("50")), 50);
  assertEquals(readDailyAllowance(envWith("2")), 2);
  assertEquals(readDailyAllowance(envWith(" 20 ")), 20);
});

Deno.test("the utc day is the calendar date of the instant", () => {
  assertEquals(utcDay(LAST_SECOND), "2026-09-10");
  assertEquals(utcDay(FIRST_SECOND), "2026-09-11");
});

Deno.test("the reset is the next utc midnight", () => {
  assertEquals(
    nextUtcMidnight(MIDDAY).toISOString(),
    "2026-09-08T00:00:00.000Z",
  );
  assertEquals(
    nextUtcMidnight(LAST_SECOND).toISOString(),
    "2026-09-11T00:00:00.000Z",
  );
});

Deno.test("a snapshot reports what is left of the allowance", () => {
  const credits: CreditsSnapshot = snapshot(5, 2, MIDDAY);

  assertEquals(credits.allowance, 5);
  assertEquals(credits.used, 2);
  assertEquals(credits.remaining, 3);
  assertEquals(credits.resetAt, "2026-09-08T00:00:00.000Z");
});

Deno.test("remaining never falls below zero", () => {
  assertEquals(snapshot(5, 5, MIDDAY).remaining, 0);
  assertEquals(snapshot(5, 9, MIDDAY).remaining, 0);
});

Deno.test("consuming a credit sends the user, the utc day and the allowance", async () => {
  const calls: string[] = [];
  await consumeCredit(
    rpcClient({ data: [{ consumed: true, used: 1 }], error: null }, calls),
    consumeInput(),
  );

  assertEquals(calls, [
    'consume_generation:{"p_user_id":"user-1","p_day":"2026-09-07",' +
    '"p_allowance":5}',
  ]);
});

Deno.test("a consumed row reports the credit as taken", async () => {
  const calls: string[] = [];
  const consumption: Consumption = await consumeCredit(
    rpcClient({ data: [{ consumed: true, used: 3 }], error: null }, calls),
    consumeInput(),
  );

  assertEquals(consumption.consumed, true);
  assertEquals(consumption.credits.used, 3);
  assertEquals(consumption.credits.remaining, 2);
  assertEquals(consumption.credits.resetAt, "2026-09-08T00:00:00.000Z");
});

Deno.test("a refused row leaves the day exhausted", async () => {
  const calls: string[] = [];
  const consumption: Consumption = await consumeCredit(
    rpcClient({ data: [{ consumed: false, used: 5 }], error: null }, calls),
    consumeInput(),
  );

  assertEquals(consumption.consumed, false);
  assertEquals(consumption.credits.used, 5);
  assertEquals(consumption.credits.remaining, 0);
});

Deno.test("a consume error leaves the credits unavailable", async () => {
  const calls: string[] = [];

  await assertRejects(
    () =>
      consumeCredit(
        rpcClient({ data: null, error: { message: "boom" } }, calls),
        consumeInput(),
      ),
    CreditsUnavailableError,
  );
});

Deno.test("a consume call that throws leaves the credits unavailable", async () => {
  await assertRejects(
    () => consumeCredit(throwingClient(), consumeInput()),
    CreditsUnavailableError,
  );
});

Deno.test("an empty consume result leaves the credits unavailable", async () => {
  const calls: string[] = [];

  await assertRejects(
    () =>
      consumeCredit(
        rpcClient({ data: [], error: null }, calls),
        consumeInput(),
      ),
    CreditsUnavailableError,
  );
  await assertRejects(
    () =>
      consumeCredit(
        rpcClient({ data: [{ consumed: "yes", used: null }], error: null }, []),
        consumeInput(),
      ),
    CreditsUnavailableError,
  );
});

Deno.test("a user without a row for the day has spent nothing", async () => {
  const calls: string[] = [];
  const credits: CreditsSnapshot = await readCredits(
    tableClient({ data: null, error: null }, calls),
    "user-1",
    MIDDAY,
    5,
  );

  assertEquals(credits.used, 0);
  assertEquals(credits.remaining, 5);
  assertEquals(calls, [
    "from:daily_usage",
    "select:used",
    "eq:user_id:user-1",
    "eq:day:2026-09-07",
    "maybeSingle",
  ]);
});

Deno.test("a stored row reports what the day has already spent", async () => {
  const calls: string[] = [];
  const credits: CreditsSnapshot = await readCredits(
    tableClient({ data: { used: 4 }, error: null }, calls),
    "user-1",
    MIDDAY,
    5,
  );

  assertEquals(credits.used, 4);
  assertEquals(credits.remaining, 1);
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
      ),
    CreditsUnavailableError,
  );
  await assertRejects(
    () => readCredits(throwingClient(), "user-1", MIDDAY, 5),
    CreditsUnavailableError,
  );
});

Deno.test("telemetry writes the event with the outcome", async () => {
  const calls: string[] = [];
  await recordGenerationEvent(
    tableClient({ data: null, error: null }, calls),
    {
      userId: "user-1",
      operation: "generate-note",
      cacheKey: "cache-key",
      provider: "groq",
      model: "oss",
      cached: false,
      outcome: "success",
    },
  );

  assertEquals(calls, [
    "from:generation_events",
    'insert:{"user_id":"user-1","operation":"generate-note",' +
    '"cache_key":"cache-key","provider":"groq","model":"oss",' +
    '"cached":false,"outcome":"success"}',
  ]);
});

Deno.test("a telemetry failure never reaches the caller", async () => {
  const calls: string[] = [];
  await recordGenerationEvent(
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
  );
  await recordGenerationEvent(throwingClient(), {
    userId: "user-1",
    operation: "suggest-words",
    cacheKey: null,
    provider: null,
    model: null,
    cached: false,
    outcome: "error",
  });
});
