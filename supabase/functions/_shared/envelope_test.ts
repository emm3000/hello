import { assertEquals } from "jsr:@std/assert@^1";
import { type CreditsSnapshot, snapshot } from "./credits.ts";
import {
  buildMeta,
  creditsExhaustedResponse,
  errorResponse,
  providersExhaustedResponse,
  type ResponseMeta,
} from "./envelope.ts";
import { PROMPT_VERSION } from "./prompt.ts";
import { SCHEMA_VERSION } from "./schema.ts";

const MIDDAY: Date = new Date("2026-09-10T10:00:00.000Z");
const NEXT_MIDNIGHT: string = "2026-09-11T00:00:00.000Z";

type ErrorBody = {
  success: boolean;
  data: unknown;
  error: { code: string; message: string; reset_at?: string };
  meta: ResponseMeta | null;
};

Deno.test("meta without credits reports an unknown balance and the utc reset", () => {
  const meta: ResponseMeta = buildMeta(null, null, false, null, MIDDAY);

  assertEquals(meta.credits_remaining, null);
  assertEquals(meta.reset_at, NEXT_MIDNIGHT);
  assertEquals(meta.provider, null);
  assertEquals(meta.model, null);
  assertEquals(meta.cached, false);
  assertEquals(meta.prompt_version, PROMPT_VERSION);
  assertEquals(meta.schema_version, SCHEMA_VERSION);
});

Deno.test("meta with credits reports the balance and the snapshot reset", () => {
  const credits: CreditsSnapshot = snapshot(50, 8, MIDDAY);
  const meta: ResponseMeta = buildMeta("groq", "oss", true, credits, MIDDAY);

  assertEquals(meta.credits_remaining, 42);
  assertEquals(meta.reset_at, NEXT_MIDNIGHT);
  assertEquals(meta.provider, "groq");
  assertEquals(meta.model, "oss");
  assertEquals(meta.cached, true);
});

Deno.test("an exhausted response carries the reset twice and the balance", async () => {
  const credits: CreditsSnapshot = snapshot(50, 50, MIDDAY);
  const response: Response = creditsExhaustedResponse(
    buildMeta(null, null, false, credits, MIDDAY),
  );
  const body: ErrorBody = await response.json();

  assertEquals(response.status, 402);
  assertEquals(body.error.code, "credits_exhausted");
  assertEquals(body.error.reset_at, NEXT_MIDNIGHT);
  assertEquals(body.meta?.credits_remaining, 0);
  assertEquals(body.meta?.reset_at, NEXT_MIDNIGHT);
});

Deno.test("an exhausted provider response still reports the balance", async () => {
  const credits: CreditsSnapshot = snapshot(50, 9, MIDDAY);
  const response: Response = providersExhaustedResponse(
    30,
    buildMeta(null, null, false, credits, MIDDAY),
  );
  const body: ErrorBody = await response.json();

  assertEquals(response.status, 503);
  assertEquals(response.headers.get("Retry-After"), "30");
  assertEquals(body.meta?.credits_remaining, 41);
});

Deno.test("an error response defaults to no meta", async () => {
  const response: Response = errorResponse("internal", "boom");
  const body: ErrorBody = await response.json();

  assertEquals(response.status, 500);
  assertEquals(body.meta, null);
});
