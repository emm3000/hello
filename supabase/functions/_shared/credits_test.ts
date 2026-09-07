import { assertEquals } from "jsr:@std/assert@^1";
import { type SupabaseClient } from "npm:@supabase/supabase-js@2";
import {
  checkCredits,
  type CreditsSnapshot,
  DEFAULT_DAILY_ALLOWANCE,
  nextUtcMidnight,
  readDailyAllowance,
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

type CountReply = { count: number | null; error: { message: string } | null };

function countingClient(reply: CountReply, filters: string[]): SupabaseClient {
  const builder: Record<string, unknown> = {
    select(column: string, options: Record<string, unknown>): unknown {
      filters.push("select:" + column + ":" + JSON.stringify(options));
      return builder;
    },
    eq(column: string, value: unknown): unknown {
      filters.push("eq:" + column + ":" + String(value));
      return builder;
    },
    gte(column: string, value: unknown): unknown {
      filters.push("gte:" + column + ":" + String(value));
      return builder;
    },
    then(resolve: (value: CountReply) => unknown): unknown {
      return resolve(reply);
    },
  };
  return {
    from(table: string): unknown {
      filters.push("from:" + table);
      return builder;
    },
  } as unknown as SupabaseClient;
}

function throwingClient(): SupabaseClient {
  return {
    from(_table: string): unknown {
      throw new Error("connection refused");
    },
  } as unknown as SupabaseClient;
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
  const snapshot: CreditsSnapshot = snapshotFrom(5, 2, MIDDAY);

  assertEquals(snapshot.allowance, 5);
  assertEquals(snapshot.charged, 2);
  assertEquals(snapshot.remaining, 3);
  assertEquals(snapshot.resetAt, "2026-09-08T00:00:00.000Z");
});

Deno.test("remaining never falls below zero", () => {
  assertEquals(snapshotFrom(5, 5, MIDDAY).remaining, 0);
  assertEquals(snapshotFrom(5, 9, MIDDAY).remaining, 0);
});

Deno.test("checkCredits counts today's uncached successes", async () => {
  const filters: string[] = [];
  const snapshot: CreditsSnapshot = await checkCredits(
    countingClient({ count: 3, error: null }, filters),
    "user-1",
    MIDDAY,
    5,
  );

  assertEquals(snapshot.charged, 3);
  assertEquals(snapshot.remaining, 2);
  assertEquals(filters, [
    "from:generation_events",
    'select:id:{"count":"exact","head":true}',
    "eq:user_id:user-1",
    "eq:outcome:success",
    "eq:cached:false",
    "gte:created_at:2026-09-07T00:00:00.000Z",
  ]);
});

Deno.test("a credits read failure fails open", async () => {
  const filters: string[] = [];
  const failed: CreditsSnapshot = await checkCredits(
    countingClient({ count: null, error: { message: "boom" } }, filters),
    "user-1",
    MIDDAY,
    5,
  );
  const thrown: CreditsSnapshot = await checkCredits(
    throwingClient(),
    "user-1",
    MIDDAY,
    5,
  );

  assertEquals(failed.charged, 0);
  assertEquals(failed.remaining, 5);
  assertEquals(thrown.charged, 0);
  assertEquals(thrown.remaining, 5);
});
