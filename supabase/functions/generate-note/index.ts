import { createSupabaseContext } from "npm:@supabase/server@^1";
import {
  AppCheckRejectedError,
  verifyAppCheckToken,
} from "../_shared/appcheck.ts";
import {
  buildMeta,
  errorResponse,
  methodNotAllowedResponse,
  providersExhaustedResponse,
  refusalResponse,
  type ResponseMeta,
  successResponse,
} from "../_shared/envelope.ts";
import { buildLearningNotePrompt } from "../_shared/prompt.ts";
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

  const parsed = generateNoteRequestSchema.safeParse(payload);
  if (!parsed.success) {
    const path: string = parsed.error.issues[0].path.join(".");
    return errorResponse(
      "invalid_request",
      "The field " + (path.length === 0 ? "body" : path) + " is invalid.",
    );
  }
  const request: GenerateNoteRequest = parsed.data;

  try {
    const generated: GenerationResult<LearningNoteResponse> =
      await generateStructured<LearningNoteResponse>({
        prompt: buildLearningNotePrompt(request),
        schemaName: "learning_note",
        jsonSchema: learningNoteJsonSchema,
        parse: learningNoteResponseSchema.parse,
      });
    const meta: ResponseMeta = buildMeta(
      generated.provider,
      generated.model,
    );
    if (generated.value.success && generated.value.data !== undefined) {
      return successResponse(generated.value.data, meta);
    }
    return refusalResponse(generated.value.error ?? null, meta);
  } catch (error: unknown) {
    if (error instanceof ProvidersExhaustedError) {
      return providersExhaustedResponse(error.retryAfterSeconds);
    }
    console.error(error instanceof Error ? error.name : "UnknownError");
    return errorResponse("internal", "The note could not be generated.");
  }
}

export default {
  fetch(req: Request): Promise<Response> {
    return handle(req);
  },
};
