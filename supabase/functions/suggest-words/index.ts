import { createSupabaseContext } from "npm:@supabase/server@^1";
import { type SupabaseClient } from "npm:@supabase/supabase-js@2";
import {
  AppCheckRejectedError,
  verifyAppCheckToken,
} from "../_shared/appcheck.ts";
import {
  checkCredits,
  type CreditsSnapshot,
  readDailyAllowance,
  recordGenerationEvent,
} from "../_shared/credits.ts";
import {
  buildMeta,
  creditsExhaustedResponse,
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
  const credits: CreditsSnapshot = await checkCredits(
    client,
    userId,
    new Date(),
    readDailyAllowance(),
  );
  if (credits.remaining <= 0) {
    await recordGenerationEvent(client, {
      userId,
      operation: "suggest-words",
      cacheKey: null,
      provider: null,
      model: null,
      cached: false,
      outcome: "credits_exhausted",
    });
    logRequest("suggest-words", false, "credits_exhausted", startedAt);
    return creditsExhaustedResponse(credits.resetAt);
  }

  try {
    const generated: GenerationResult<WordSuggestionResponse> =
      await generateStructured<WordSuggestionResponse>({
        prompt: buildWordSuggestionPrompt(request.recent_words),
        schemaName: "word_suggestions",
        jsonSchema: wordSuggestionJsonSchema,
        parse: wordSuggestionSchema.parse,
        providerState: createProviderStateStore(client),
      });
    await recordGenerationEvent(client, {
      userId,
      operation: "suggest-words",
      cacheKey: null,
      provider: generated.provider,
      model: generated.model,
      cached: false,
      outcome: "success",
    });
    const meta: ResponseMeta = buildMeta(
      generated.provider,
      generated.model,
      false,
      Math.max(credits.remaining - 1, 0),
    );
    const cleaned: WordSuggestionResponse = withoutNulls(generated.value);
    logRequest("suggest-words", false, "success", startedAt);
    return jsonResponse({
      situation: cleaned.situation,
      words: cleaned.words,
      meta,
    }, 200);
  } catch (error: unknown) {
    if (error instanceof ProvidersExhaustedError) {
      await recordGenerationEvent(client, {
        userId,
        operation: "suggest-words",
        cacheKey: null,
        provider: null,
        model: null,
        cached: false,
        outcome: "providers_exhausted",
      });
      logRequest("suggest-words", false, "providers_exhausted", startedAt);
      return providersExhaustedResponse(error.retryAfterSeconds);
    }
    console.error(error instanceof Error ? error.name : "UnknownError");
    return errorResponse("internal", "The suggestions could not be generated.");
  }
}

export default {
  fetch(req: Request): Promise<Response> {
    return handle(req);
  },
};
