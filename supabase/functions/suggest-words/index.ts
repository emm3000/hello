import { createSupabaseContext } from "npm:@supabase/server@^1";
import { type SupabaseClient } from "npm:@supabase/supabase-js@2";
import {
  AppCheckRejectedError,
  verifyAppCheckToken,
} from "../_shared/appcheck.ts";
import {
  consumeCredit,
  type ConsumeInput,
  type Consumption,
  CREDITS_UNAVAILABLE_RETRY_AFTER_SECONDS,
  type CreditsSnapshot,
  CreditsUnavailableError,
  type GenerationEvent,
  type GenerationOperation,
  readDailyAllowance,
  recordGenerationEvent,
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
  type GenerateStructuredArgs,
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

export type SuggestWordsDeps = {
  createSupabaseContext: typeof createSupabaseContext;
  verifyAppCheckToken: (
    token: string | null,
    projectNumber: string,
  ) => Promise<string>;
  generateStructured<T>(
    args: GenerateStructuredArgs<T>,
  ): Promise<GenerationResult<T>>;
  consumeCredit: (
    client: SupabaseClient,
    input: ConsumeInput,
  ) => Promise<Consumption>;
  recordGenerationEvent: (
    client: SupabaseClient,
    event: GenerationEvent,
  ) => Promise<void>;
  readDailyAllowance: (env: (name: string) => string | undefined) => number;
  now: () => Date;
  env: (name: string) => string | undefined;
};

export const defaultDeps: SuggestWordsDeps = {
  createSupabaseContext,
  verifyAppCheckToken,
  generateStructured,
  consumeCredit,
  recordGenerationEvent,
  readDailyAllowance,
  now: (): Date => new Date(),
  env: (name: string): string | undefined => Deno.env.get(name),
};

function respondCreditsUnavailable(
  operation: GenerationOperation,
  cached: boolean,
  startedAt: number,
): Response {
  logRequest(operation, cached, "credits_unavailable", startedAt);
  return creditsUnavailableResponse(CREDITS_UNAVAILABLE_RETRY_AFTER_SECONDS);
}

export async function handle(
  req: Request,
  deps: SuggestWordsDeps = defaultDeps,
): Promise<Response> {
  const startedAt: number = performance.now();
  if (req.method !== "POST") {
    return methodNotAllowedResponse();
  }

  const { data: ctx, error: authError } = await deps.createSupabaseContext(
    req,
    { auth: "user" },
  );
  if (
    authError !== null || ctx === null || ctx.userClaims === null ||
    ctx.userClaims.role !== "authenticated"
  ) {
    return errorResponse(
      "unauthorized",
      "An authenticated session is required.",
    );
  }

  const projectNumber: string | undefined = deps.env(
    "FIREBASE_PROJECT_NUMBER",
  );
  if (projectNumber === undefined || projectNumber.length === 0) {
    return errorResponse(
      "misconfigured",
      "The function is missing its Firebase project number.",
    );
  }

  try {
    await deps.verifyAppCheckToken(
      req.headers.get(APP_CHECK_HEADER),
      projectNumber,
    );
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
  const now: Date = deps.now();
  const allowance: number = deps.readDailyAllowance(deps.env);

  let consumption: Consumption;
  try {
    consumption = await deps.consumeCredit(client, {
      userId,
      now,
      allowance,
    });
  } catch (error: unknown) {
    if (error instanceof CreditsUnavailableError) {
      return respondCreditsUnavailable("suggest-words", false, startedAt);
    }
    throw error;
  }
  const credits: CreditsSnapshot = consumption.credits;
  if (!consumption.consumed) {
    logRequest("suggest-words", false, "credits_exhausted", startedAt);
    return creditsExhaustedResponse(buildMeta(null, null, false, credits, now));
  }

  try {
    const generated: GenerationResult<WordSuggestionResponse> = await deps
      .generateStructured<WordSuggestionResponse>({
        prompt: buildWordSuggestionPrompt(request.recent_words),
        schemaName: "word_suggestions",
        jsonSchema: wordSuggestionJsonSchema,
        parse: wordSuggestionSchema.parse,
        providerState: createProviderStateStore(client),
      });
    await deps.recordGenerationEvent(client, {
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
      credits,
      now,
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
      await deps.recordGenerationEvent(client, {
        userId,
        operation: "suggest-words",
        cacheKey: null,
        provider: null,
        model: null,
        cached: false,
        outcome: "providers_exhausted",
      });
      logRequest("suggest-words", false, "providers_exhausted", startedAt);
      return providersExhaustedResponse(
        error.retryAfterSeconds,
        buildMeta(null, null, false, credits, now),
      );
    }
    await deps.recordGenerationEvent(client, {
      userId,
      operation: "suggest-words",
      cacheKey: null,
      provider: null,
      model: null,
      cached: false,
      outcome: "error",
    });
    console.error(error instanceof Error ? error.name : "UnknownError");
    return errorResponse(
      "internal",
      "The suggestions could not be generated.",
      buildMeta(null, null, false, credits, now),
    );
  }
}

export default {
  fetch: (req: Request): Promise<Response> => handle(req),
};
