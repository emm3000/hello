import { PROMPT_VERSION } from "./prompt.ts";
import { SCHEMA_VERSION } from "./schema.ts";

export type ResponseMeta = {
  cached: boolean;
  provider: string;
  model: string;
  prompt_version: number;
  schema_version: number;
};

export type GenerationRefusal = {
  input?: string;
  message: string;
};

export type ErrorCode =
  | "unauthorized"
  | "app_check_rejected"
  | "invalid_request"
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
  providers_exhausted: 503,
  misconfigured: 500,
  internal: 500,
};

export function buildMeta(provider: string, model: string): ResponseMeta {
  return {
    cached: false,
    provider,
    model,
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

export function errorResponse(code: ErrorCode, message: string): Response {
  return jsonResponse(
    { success: false, data: null, error: { code, message }, meta: null },
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

export function providersExhaustedResponse(
  retryAfterSeconds: number,
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
      meta: null,
    },
    ERROR_STATUS.providers_exhausted,
    { "Retry-After": String(retryAfterSeconds) },
  );
}
