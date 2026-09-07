import { createSupabaseContext } from "npm:@supabase/server@^1";
import { type SupabaseClient } from "npm:@supabase/supabase-js@2";
import {
  AppCheckRejectedError,
  verifyAppCheckToken,
} from "../_shared/appcheck.ts";
import {
  CREDITS_UNAVAILABLE_RETRY_AFTER_SECONDS,
  type CreditsSnapshot,
  CreditsUnavailableError,
  type GenerationOperation,
  readDailyAllowance,
  readDailyRefusalAllowance,
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
  jsonResponse,
  methodNotAllowedResponse,
  providersExhaustedResponse,
  type ResponseMeta,
} from "../_shared/envelope.ts";
import { logRequest } from "../_shared/log.ts";
import { buildWordSuggestionPrompt } from "../_shared/prompt.ts";
import { createProviderStateStore } from "../_shared/provider_state.ts";
import {
  generateStructured,
  type GenerationResult,
  ProvidersExhaustedError,
} from "../_shared/providers.ts";
import {
  type SuggestWordsRequest,
  suggestWordsRequestSchema,
  withoutNulls,
  wordSuggestionJsonSchema,
  type WordSuggestionResponse,
  wordSuggestionSchema,
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

  const parsed = suggestWordsRequestSchema.safeParse(payload);
  if (!parsed.success) {
    const path: string = parsed.error.issues[0].path.join(".");
    return errorResponse(
      "invalid_request",
      "The field " + (path.length === 0 ? "body" : path) + " is invalid.",
    );
  }
  const request: SuggestWordsRequest = parsed.data;

  const client: SupabaseClient = ctx.supabaseAdmin;
  const userId: string = ctx.userClaims.id;
  const now: Date = new Date();
  const allowance: number = readDailyAllowance();
  const refusalAllowance: number = readDailyRefusalAllowance();

  let reservation: Reservation;
  try {
    reservation = await reserveGeneration(client, {
      userId,
      operation: "suggest-words",
      cacheKey: null,
      now,
      allowance,
      refusalAllowance,
    });
  } catch (error: unknown) {
    if (error instanceof CreditsUnavailableError) {
      return respondCreditsUnavailable("suggest-words", false, startedAt);
    }
    throw error;
  }
  if (!reservation.reserved) {
    logRequest("suggest-words", false, "credits_exhausted", startedAt);
    return creditsExhaustedResponse(reservation.credits.resetAt);
  }
  const eventId: number = reservation.eventId;
  const credits: CreditsSnapshot = reservation.credits;

  try {
    try {
      const generated: GenerationResult<WordSuggestionResponse> =
        await generateStructured<WordSuggestionResponse>({
          prompt: buildWordSuggestionPrompt(request.recent_words),
          schemaName: "word_suggestions",
          jsonSchema: wordSuggestionJsonSchema,
          parse: wordSuggestionSchema.parse,
          providerState: createProviderStateStore(client),
        });
      await settleGeneration(client, eventId, {
        outcome: "success",
        provider: generated.provider,
        model: generated.model,
      });
      const meta: ResponseMeta = buildMeta(
        generated.provider,
        generated.model,
        false,
        remainingAfter(credits, "success"),
      );
      const cleaned: WordSuggestionResponse = withoutNulls(generated.value);
      logRequest("suggest-words", false, "success", startedAt);
      return jsonResponse({
        situation: cleaned.situation,
        words: cleaned.words,
        meta,
      }, 200);
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
        logRequest("suggest-words", false, "providers_exhausted", startedAt);
        return providersExhaustedResponse(error.retryAfterSeconds);
      }
      await settleGeneration(client, eventId, {
        outcome: "error",
        provider: null,
        model: null,
      });
      console.error(error instanceof Error ? error.name : "UnknownError");
      return errorResponse(
        "internal",
        "The suggestions could not be generated.",
      );
    }
  } catch (error: unknown) {
    if (error instanceof CreditsUnavailableError) {
      return respondCreditsUnavailable("suggest-words", false, startedAt);
    }
    throw error;
  }
}

export default {
  fetch(req: Request): Promise<Response> {
    return handle(req);
  },
};
