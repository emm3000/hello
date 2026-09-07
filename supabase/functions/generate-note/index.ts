import { createSupabaseContext } from "npm:@supabase/server@^1";
import { type SupabaseClient } from "npm:@supabase/supabase-js@2";
import {
  AppCheckRejectedError,
  verifyAppCheckToken,
} from "../_shared/appcheck.ts";
import {
  buildCacheKey,
  cacheExpiry,
  commitNoteCache,
  type NoteCacheRow,
  readNoteCache,
} from "../_shared/cache.ts";
import {
  CREDITS_UNAVAILABLE_RETRY_AFTER_SECONDS,
  type CreditsSnapshot,
  CreditsUnavailableError,
  type GenerationOperation,
  type GenerationOutcome,
  readCredits,
  readDailyAllowance,
  readDailyRefusalAllowance,
  recordGenerationEvent,
  remainingAfter,
  type Reservation,
  reserveGeneration,
  settleGeneration,
} from "../_shared/credits.ts";
import {
  buildMeta,
  creditsExhaustedResponse,
  creditsUnavailableResponse,
  errorResponse,
  methodNotAllowedResponse,
  providersExhaustedResponse,
  refusalResponse,
  type ResponseMeta,
  successResponse,
} from "../_shared/envelope.ts";
import { logRequest } from "../_shared/log.ts";
import { buildLearningNotePrompt, PROMPT_VERSION } from "../_shared/prompt.ts";
import { createProviderStateStore } from "../_shared/provider_state.ts";
import {
  generateStructured,
  type GenerationResult,
  ProvidersExhaustedError,
} from "../_shared/providers.ts";
import {
  type GenerateNoteRequest,
  generateNoteRequestSchema,
  learningNoteJsonSchema,
  type LearningNoteResponse,
  learningNoteResponseSchema,
  SCHEMA_VERSION,
  withoutNulls,
} from "../_shared/schema.ts";

const APP_CHECK_HEADER: string = "X-Firebase-AppCheck";

function respondCreditsUnavailable(
  operation: GenerationOperation,
  cached: boolean,
  startedAt: number,
): Response {
  logRequest(operation, cached, "credits_unavailable", startedAt);
  return creditsUnavailableResponse(CREDITS_UNAVAILABLE_RETRY_AFTER_SECONDS);
}

async function handle(req: Request): Promise<Response> {
  const startedAt: number = performance.now();
  if (req.method !== "POST") {
    return methodNotAllowedResponse();
  }

  const { data: ctx, error: authError } = await createSupabaseContext(req, {
    auth: "user",
  });
  if (
    authError !== null || ctx === null || ctx.userClaims === null ||
    ctx.userClaims.role !== "authenticated"
  ) {
    return errorResponse(
      "unauthorized",
      "An authenticated session is required.",
    );
  }

  const projectNumber: string | undefined = Deno.env.get(
    "FIREBASE_PROJECT_NUMBER",
  );
  if (projectNumber === undefined || projectNumber.length === 0) {
    return errorResponse(
      "misconfigured",
      "The function is missing its Firebase project number.",
    );
  }

  try {
    await verifyAppCheckToken(req.headers.get(APP_CHECK_HEADER), projectNumber);
  } catch (error: unknown) {
    if (error instanceof AppCheckRejectedError) {
      return errorResponse("app_check_rejected", error.message);
    }
    console.error("AppCheckVerificationFailure");
    return errorResponse("internal", "The request could not be completed.");
  }

  let payload: unknown;
  try {
    payload = await req.json();
  } catch (_error: unknown) {
    return errorResponse("invalid_request", "The body is not valid JSON.");
  }

  const parsed = generateNoteRequestSchema.safeParse(payload);
  if (!parsed.success) {
    const path: string = parsed.error.issues[0].path.join(".");
    return errorResponse(
      "invalid_request",
      "The field " + (path.length === 0 ? "body" : path) + " is invalid.",
    );
  }
  const request: GenerateNoteRequest = parsed.data;

  const client: SupabaseClient = ctx.supabaseAdmin;
  const userId: string = ctx.userClaims.id;
  const now: Date = new Date();
  const allowance: number = readDailyAllowance();
  const refusalAllowance: number = readDailyRefusalAllowance();
  const cacheKey: string = await buildCacheKey(request);

  if (request.previous_issues.length === 0) {
    const hit: NoteCacheRow | null = await readNoteCache(client, cacheKey);
    if (hit !== null) {
      const outcome: GenerationOutcome = hit.success ? "success" : "refusal";
      let cachedCredits: CreditsSnapshot;
      try {
        await recordGenerationEvent(client, {
          userId,
          operation: "generate-note",
          cacheKey,
          provider: null,
          model: null,
          cached: true,
          outcome,
        });
        cachedCredits = await readCredits(
          client,
          userId,
          now,
          allowance,
          refusalAllowance,
        );
      } catch (error: unknown) {
        if (error instanceof CreditsUnavailableError) {
          return respondCreditsUnavailable("generate-note", true, startedAt);
        }
        throw error;
      }
      const meta: ResponseMeta = buildMeta(
        hit.provider,
        hit.model,
        true,
        cachedCredits.remaining,
      );
      logRequest("generate-note", true, outcome, startedAt);
      if (hit.success && hit.response.data !== undefined) {
        return successResponse(hit.response.data, meta);
      }
      return refusalResponse(hit.response.error ?? null, meta);
    }
  }

  let reservation: Reservation;
  try {
    reservation = await reserveGeneration(client, {
      userId,
      operation: "generate-note",
      cacheKey,
      now,
      allowance,
      refusalAllowance,
    });
  } catch (error: unknown) {
    if (error instanceof CreditsUnavailableError) {
      return respondCreditsUnavailable("generate-note", false, startedAt);
    }
    throw error;
  }
  if (!reservation.reserved) {
    logRequest("generate-note", false, "credits_exhausted", startedAt);
    return creditsExhaustedResponse(reservation.credits.resetAt);
  }
  const eventId: number = reservation.eventId;
  const credits: CreditsSnapshot = reservation.credits;

  try {
    try {
      const generated: GenerationResult<LearningNoteResponse> =
        await generateStructured<LearningNoteResponse>({
          prompt: buildLearningNotePrompt(request),
          schemaName: "learning_note",
          jsonSchema: learningNoteJsonSchema,
          parse: learningNoteResponseSchema.parse,
          providerState: createProviderStateStore(client),
        });
      const cleaned: LearningNoteResponse = withoutNulls(generated.value);
      const outcome: "success" | "refusal" = cleaned.success
        ? "success"
        : "refusal";
      await commitNoteCache(client, request, {
        cache_key: cacheKey,
        response: cleaned,
        success: cleaned.success,
        provider: generated.provider,
        model: generated.model,
        prompt_version: PROMPT_VERSION,
        schema_version: SCHEMA_VERSION,
        expires_at: cacheExpiry(cleaned.success, now),
      });
      await settleGeneration(client, eventId, {
        outcome,
        provider: generated.provider,
        model: generated.model,
      });
      const meta: ResponseMeta = buildMeta(
        generated.provider,
        generated.model,
        false,
        remainingAfter(credits, outcome),
      );
      logRequest("generate-note", false, outcome, startedAt);
      if (cleaned.success && cleaned.data !== undefined) {
        return successResponse(cleaned.data, meta);
      }
      return refusalResponse(cleaned.error ?? null, meta);
    } catch (error: unknown) {
      if (error instanceof CreditsUnavailableError) {
        throw error;
      }
      if (error instanceof ProvidersExhaustedError) {
        await settleGeneration(client, eventId, {
          outcome: "providers_exhausted",
          provider: null,
          model: null,
        });
        logRequest("generate-note", false, "providers_exhausted", startedAt);
        return providersExhaustedResponse(error.retryAfterSeconds);
      }
      await settleGeneration(client, eventId, {
        outcome: "error",
        provider: null,
        model: null,
      });
      console.error(error instanceof Error ? error.name : "UnknownError");
      return errorResponse("internal", "The note could not be generated.");
    }
  } catch (error: unknown) {
    if (error instanceof CreditsUnavailableError) {
      return respondCreditsUnavailable("generate-note", false, startedAt);
    }
    throw error;
  }
}

export default {
  fetch(req: Request): Promise<Response> {
    return handle(req);
  },
};
