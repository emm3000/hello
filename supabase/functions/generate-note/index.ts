import { createSupabaseContext } from "npm:@supabase/server@^1";
import { type SupabaseClient } from "npm:@supabase/supabase-js@2";
import {
  AppCheckRejectedError,
  verifyAppCheckToken,
} from "../_shared/appcheck.ts";
import {
  buildCacheKey,
  cacheExpiry,
  type NoteCacheRow,
  readNoteCache,
  writeNoteCache,
} from "../_shared/cache.ts";
import {
  type GenerationOutcome,
  recordGenerationEvent,
} from "../_shared/credits.ts";
import {
  buildMeta,
  errorResponse,
  methodNotAllowedResponse,
  providersExhaustedResponse,
  refusalResponse,
  type ResponseMeta,
  successResponse,
} from "../_shared/envelope.ts";
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

function logRequest(
  cached: boolean,
  outcome: GenerationOutcome,
  startedAt: number,
): void {
  console.info(JSON.stringify({
    op: "generate-note",
    cached,
    outcome,
    ms: Math.round(performance.now() - startedAt),
  }));
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
  const cacheKey: string = await buildCacheKey(request);

  if (request.previous_issues.length === 0) {
    const hit: NoteCacheRow | null = await readNoteCache(client, cacheKey);
    if (hit !== null) {
      const outcome: GenerationOutcome = hit.success ? "success" : "refusal";
      await recordGenerationEvent(client, {
        userId,
        operation: "generate-note",
        cacheKey,
        provider: null,
        model: null,
        cached: true,
        outcome,
      });
      const meta: ResponseMeta = buildMeta(hit.provider, hit.model, true);
      logRequest(true, outcome, startedAt);
      if (hit.success && hit.response.data !== undefined) {
        return successResponse(hit.response.data, meta);
      }
      return refusalResponse(hit.response.error ?? null, meta);
    }
  }

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
    const outcome: GenerationOutcome = cleaned.success ? "success" : "refusal";
    await writeNoteCache(client, {
      cache_key: cacheKey,
      response: cleaned,
      success: cleaned.success,
      provider: generated.provider,
      model: generated.model,
      prompt_version: PROMPT_VERSION,
      schema_version: SCHEMA_VERSION,
      expires_at: cacheExpiry(cleaned.success, new Date()),
    });
    await recordGenerationEvent(client, {
      userId,
      operation: "generate-note",
      cacheKey,
      provider: generated.provider,
      model: generated.model,
      cached: false,
      outcome,
    });
    const meta: ResponseMeta = buildMeta(
      generated.provider,
      generated.model,
      false,
    );
    logRequest(false, outcome, startedAt);
    if (cleaned.success && cleaned.data !== undefined) {
      return successResponse(cleaned.data, meta);
    }
    return refusalResponse(cleaned.error ?? null, meta);
  } catch (error: unknown) {
    if (error instanceof ProvidersExhaustedError) {
      await recordGenerationEvent(client, {
        userId,
        operation: "generate-note",
        cacheKey,
        provider: null,
        model: null,
        cached: false,
        outcome: "providers_exhausted",
      });
      logRequest(false, "providers_exhausted", startedAt);
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
