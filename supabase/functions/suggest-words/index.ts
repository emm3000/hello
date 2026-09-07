import { createSupabaseContext } from "npm:@supabase/server@^1";
import {
  AppCheckRejectedError,
  verifyAppCheckToken,
} from "../_shared/appcheck.ts";
import {
  buildMeta,
  errorResponse,
  jsonResponse,
  methodNotAllowedResponse,
  providersExhaustedResponse,
  type ResponseMeta,
} from "../_shared/envelope.ts";
import { buildWordSuggestionPrompt } from "../_shared/prompt.ts";
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

  try {
    const generated: GenerationResult<WordSuggestionResponse> =
      await generateStructured<WordSuggestionResponse>({
        prompt: buildWordSuggestionPrompt(request.recent_words),
        schemaName: "word_suggestions",
        jsonSchema: wordSuggestionJsonSchema,
        parse: wordSuggestionSchema.parse,
      });
    const meta: ResponseMeta = buildMeta(generated.provider, generated.model);
    const cleaned: WordSuggestionResponse = withoutNulls(generated.value);
    return jsonResponse({
      situation: cleaned.situation,
      words: cleaned.words,
      meta,
    }, 200);
  } catch (error: unknown) {
    if (error instanceof ProvidersExhaustedError) {
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
