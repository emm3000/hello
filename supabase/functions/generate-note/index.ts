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
  consumeCredit,
  type ConsumeInput,
  type Consumption,
  CREDITS_UNAVAILABLE_RETRY_AFTER_SECONDS,
  type CreditsSnapshot,
  CreditsUnavailableError,
  type GenerationEvent,
  type GenerationOperation,
  type GenerationOutcome,
  readCredits,
  readDailyAllowance,
  recordGenerationEvent,
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
  type GenerateStructuredArgs,
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

export type GenerateNoteDeps = {
  createSupabaseContext: typeof createSupabaseContext;
  verifyAppCheckToken: (
    token: string | null,
    projectNumber: string,
  ) => Promise<string>;
  readNoteCache: (
    client: SupabaseClient,
    key: string,
  ) => Promise<NoteCacheRow | null>;
  writeNoteCache: (
    client: SupabaseClient,
    row: Omit<NoteCacheRow, "hits">,
  ) => Promise<void>;
  generateStructured<T>(
    args: GenerateStructuredArgs<T>,
  ): Promise<GenerationResult<T>>;
  consumeCredit: (
    client: SupabaseClient,
    input: ConsumeInput,
  ) => Promise<Consumption>;
  readCredits: (
    client: SupabaseClient,
    userId: string,
    now: Date,
    allowance: number,
  ) => Promise<CreditsSnapshot>;
  recordGenerationEvent: (
    client: SupabaseClient,
    event: GenerationEvent,
  ) => Promise<void>;
  readDailyAllowance: (env: (name: string) => string | undefined) => number;
  now: () => Date;
  env: (name: string) => string | undefined;
};

export const defaultDeps: GenerateNoteDeps = {
  createSupabaseContext,
  verifyAppCheckToken,
  readNoteCache,
  writeNoteCache,
  generateStructured,
  consumeCredit,
  readCredits,
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
  deps: GenerateNoteDeps = defaultDeps,
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
  const now: Date = deps.now();
  const allowance: number = deps.readDailyAllowance(deps.env);
  const cacheKey: string = await buildCacheKey(request);

  if (request.previous_issues.length === 0) {
    const hit: NoteCacheRow | null = await deps.readNoteCache(client, cacheKey);
    if (hit !== null) {
      const outcome: GenerationOutcome = hit.success ? "success" : "refusal";
      await deps.recordGenerationEvent(client, {
        userId,
        operation: "generate-note",
        cacheKey,
        provider: null,
        model: null,
        cached: true,
        outcome,
      });
      let cachedCredits: CreditsSnapshot | null;
      try {
        cachedCredits = await deps.readCredits(client, userId, now, allowance);
      } catch (error: unknown) {
        if (!(error instanceof CreditsUnavailableError)) {
          throw error;
        }
        cachedCredits = null;
      }
      const meta: ResponseMeta = buildMeta(
        hit.provider,
        hit.model,
        true,
        cachedCredits,
        now,
      );
      logRequest("generate-note", true, outcome, startedAt);
      if (hit.success && hit.response.data !== undefined) {
        return successResponse(hit.response.data, meta);
      }
      return refusalResponse(hit.response.error ?? null, meta);
    }
  }

  let consumption: Consumption;
  try {
    consumption = await deps.consumeCredit(client, {
      userId,
      now,
      allowance,
    });
  } catch (error: unknown) {
    if (error instanceof CreditsUnavailableError) {
      return respondCreditsUnavailable("generate-note", false, startedAt);
    }
    throw error;
  }
  const credits: CreditsSnapshot = consumption.credits;
  if (!consumption.consumed) {
    logRequest("generate-note", false, "credits_exhausted", startedAt);
    return creditsExhaustedResponse(buildMeta(null, null, false, credits, now));
  }

  try {
    const generated: GenerationResult<LearningNoteResponse> = await deps
      .generateStructured<LearningNoteResponse>({
        prompt: buildLearningNotePrompt(request),
        schemaName: "learning_note",
        jsonSchema: learningNoteJsonSchema,
        parse: learningNoteResponseSchema.parse,
        providerState: createProviderStateStore(client),
      });
    const cleaned: LearningNoteResponse = withoutNulls(generated.value);
    const outcome: GenerationOutcome = cleaned.success ? "success" : "refusal";
    await deps.writeNoteCache(client, {
      cache_key: cacheKey,
      response: cleaned,
      success: cleaned.success,
      provider: generated.provider,
      model: generated.model,
      prompt_version: PROMPT_VERSION,
      schema_version: SCHEMA_VERSION,
      expires_at: cacheExpiry(cleaned.success, now),
    });
    await deps.recordGenerationEvent(client, {
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
      credits,
      now,
    );
    logRequest("generate-note", false, outcome, startedAt);
    if (cleaned.success && cleaned.data !== undefined) {
      return successResponse(cleaned.data, meta);
    }
    return refusalResponse(cleaned.error ?? null, meta);
  } catch (error: unknown) {
    if (error instanceof ProvidersExhaustedError) {
      await deps.recordGenerationEvent(client, {
        userId,
        operation: "generate-note",
        cacheKey,
        provider: null,
        model: null,
        cached: false,
        outcome: "providers_exhausted",
      });
      logRequest("generate-note", false, "providers_exhausted", startedAt);
      return providersExhaustedResponse(
        error.retryAfterSeconds,
        buildMeta(null, null, false, credits, now),
      );
    }
    await deps.recordGenerationEvent(client, {
      userId,
      operation: "generate-note",
      cacheKey,
      provider: null,
      model: null,
      cached: false,
      outcome: "error",
    });
    console.error(error instanceof Error ? error.name : "UnknownError");
    return errorResponse(
      "internal",
      "The note could not be generated.",
      buildMeta(null, null, false, credits, now),
    );
  }
}

export default {
  fetch: (req: Request): Promise<Response> => handle(req),
};
