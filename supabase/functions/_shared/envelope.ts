import { type CreditsSnapshot, nextUtcMidnight } from "./credits.ts";
import { PROMPT_VERSION } from "./prompt.ts";
import { type RefusalCode, SCHEMA_VERSION } from "./schema.ts";

export type ResponseMeta = {
  cached: boolean;
  provider: string | null;
  model: string | null;
  credits_remaining: number | null;
  reset_at: string;
  prompt_version: number;
  schema_version: number;
};

export type GenerationRefusal = {
  input?: string | null;
  message: string;
  code?: RefusalCode | null;
};

export type ErrorCode =
  | "unauthorized"
  | "app_check_rejected"
  | "invalid_request"
  | "credits_exhausted"
  | "credits_unavailable"
  | "providers_exhausted"
  | "misconfigured"
  | "internal";

const JSON_HEADERS: Record<string, string> = {
  "Content-Type": "application/json",
};

const ERROR_STATUS: Record<ErrorCode, number> = {
  unauthorized: 401,
  app_check_rejected: 401,
  invalid_request: 400,
  credits_exhausted: 402,
  credits_unavailable: 503,
  providers_exhausted: 503,
  misconfigured: 500,
  internal: 500,
};

export function buildMeta(
  provider: string | null,
  model: string | null,
  cached: boolean,
  credits: CreditsSnapshot | null,
  now: Date,
): ResponseMeta {
  return {
    cached,
    provider,
    model,
    credits_remaining: credits?.remaining ?? null,
    reset_at: credits?.resetAt ?? nextUtcMidnight(now).toISOString(),
    prompt_version: PROMPT_VERSION,
    schema_version: SCHEMA_VERSION,
  };
}

export function jsonResponse(
  body: unknown,
  status: number,
  extraHeaders: Record<string, string> = {},
): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...JSON_HEADERS, ...extraHeaders },
  });
}

export function successResponse(data: unknown, meta: ResponseMeta): Response {
  return jsonResponse({ success: true, data, error: null, meta }, 200);
}

export function refusalResponse(
  refusal: GenerationRefusal | null,
  meta: ResponseMeta,
): Response {
  return jsonResponse(
    { success: false, data: null, error: refusal, meta },
    200,
  );
}

export function errorResponse(
  code: ErrorCode,
  message: string,
  meta: ResponseMeta | null = null,
): Response {
  return jsonResponse(
    { success: false, data: null, error: { code, message }, meta },
    ERROR_STATUS[code],
  );
}

export function methodNotAllowedResponse(): Response {
  return jsonResponse(
    {
      success: false,
      data: null,
      error: {
        code: "invalid_request",
        message: "This endpoint only accepts POST.",
      },
      meta: null,
    },
    405,
  );
}

export function creditsExhaustedResponse(meta: ResponseMeta): Response {
  return jsonResponse(
    {
      success: false,
      data: null,
      error: {
        code: "credits_exhausted",
        message:
          "Alcanzaste el límite diario de generaciones con IA. Vuelve a intentarlo mañana.",
        reset_at: meta.reset_at,
      },
      meta,
    },
    ERROR_STATUS.credits_exhausted,
  );
}

export function creditsUnavailableResponse(
  retryAfterSeconds: number,
): Response {
  return jsonResponse(
    {
      success: false,
      data: null,
      error: {
        code: "credits_unavailable",
        message: "The daily allowance could not be checked right now.",
        retry_after: retryAfterSeconds,
      },
      meta: null,
    },
    ERROR_STATUS.credits_unavailable,
    { "Retry-After": String(retryAfterSeconds) },
  );
}

export function providersExhaustedResponse(
  retryAfterSeconds: number,
  meta: ResponseMeta,
): Response {
  return jsonResponse(
    {
      success: false,
      data: null,
      error: {
        code: "providers_exhausted",
        message: "Every AI provider is unavailable right now.",
        retry_after: retryAfterSeconds,
      },
      meta,
    },
    ERROR_STATUS.providers_exhausted,
    { "Retry-After": String(retryAfterSeconds) },
  );
}
