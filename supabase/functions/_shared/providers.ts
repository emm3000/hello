import { withSchemaAppendix } from "./prompt.ts";

export type OutputMode = "json_schema" | "json_object";

export type ProviderConfig = {
  id: string;
  baseUrl: string;
  apiKeyEnv: string;
  model: string;
  outputMode: OutputMode;
  timeoutMs: number;
  cooldownAfterRateLimit: (now: Date) => Date;
};

export type ProviderOutcome =
  | "ok"
  | "rate_limited"
  | "unavailable"
  | "unauthorized"
  | "timeout"
  | "invalid_output";

export type GenerationResult<T> = {
  value: T;
  provider: string;
  model: string;
};

export type GenerateStructuredArgs<T> = {
  prompt: string;
  schemaName: string;
  jsonSchema: Record<string, unknown>;
  parse: (raw: unknown) => T;
  providers?: ProviderConfig[];
  fetchFn?: typeof fetch;
  env?: (name: string) => string | undefined;
  now?: () => Date;
};

export class ProvidersExhaustedError extends Error {
  readonly retryAfterSeconds: number;

  constructor(retryAfterSeconds: number) {
    super("every configured provider failed");
    this.name = "ProvidersExhaustedError";
    this.retryAfterSeconds = retryAfterSeconds;
  }
}

const HOUR_IN_MS: number = 3_600_000;

const DEFAULT_RETRY_AFTER_SECONDS: number = 60;

const exhaustedUntil: Map<string, number> = new Map<string, number>();

const losAngelesParts: Intl.DateTimeFormat = new Intl.DateTimeFormat("en-US", {
  timeZone: "America/Los_Angeles",
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
  second: "2-digit",
  hourCycle: "h23",
});

function losAngelesOffsetMs(instant: Date): number {
  const parts: Intl.DateTimeFormatPart[] = losAngelesParts.formatToParts(
    instant,
  );
  const read = (type: string): number =>
    Number(
      parts.find((part: Intl.DateTimeFormatPart): boolean => part.type === type)
        ?.value ?? "0",
    );
  const wallClock: number = Date.UTC(
    read("year"),
    read("month") - 1,
    read("day"),
    read("hour"),
    read("minute"),
    read("second"),
  );
  return wallClock - Math.floor(instant.getTime() / 1000) * 1000;
}

function nextMidnightInLosAngeles(now: Date): Date {
  const offset: number = losAngelesOffsetMs(now);
  const wallClock: Date = new Date(now.getTime() + offset);
  const nextWallClockMidnight: number = Date.UTC(
    wallClock.getUTCFullYear(),
    wallClock.getUTCMonth(),
    wallClock.getUTCDate() + 1,
  );
  const firstGuess: number = nextWallClockMidnight - offset;
  const settledOffset: number = losAngelesOffsetMs(new Date(firstGuess));
  return new Date(nextWallClockMidnight - settledOffset);
}

export const PROVIDER_CHAIN: ProviderConfig[] = [
  {
    id: "gemini",
    baseUrl: "https://generativelanguage.googleapis.com/v1beta/openai/",
    apiKeyEnv: "GEMINI_API_KEY",
    model: "gemini-3.1-flash-lite",
    outputMode: "json_schema",
    timeoutMs: 30000,
    cooldownAfterRateLimit: nextMidnightInLosAngeles,
  },
  {
    id: "openrouter",
    baseUrl: "https://openrouter.ai/api/v1/",
    apiKeyEnv: "OPENROUTER_API_KEY",
    model: "minimax/minimax-m3:free",
    outputMode: "json_object",
    timeoutMs: 45000,
    cooldownAfterRateLimit: (now: Date): Date =>
      new Date(now.getTime() + HOUR_IN_MS),
  },
];

export function resetProviderState(): void {
  exhaustedUntil.clear();
}

function logProviderAttempt(
  provider: ProviderConfig,
  status: number | null,
  latencyMs: number,
  outcome: ProviderOutcome,
): void {
  console.log(JSON.stringify({
    event: "provider_attempt",
    provider: provider.id,
    model: provider.model,
    status,
    latency_ms: latencyMs,
    outcome,
  }));
}

type InvalidOutputReason = "json_parse" | "schema";

const MAX_DETAIL_LENGTH: number = 160;

function truncate(detail: string): string {
  return detail.length <= MAX_DETAIL_LENGTH
    ? detail
    : detail.slice(0, MAX_DETAIL_LENGTH);
}

function errorMessage(error: unknown): string {
  const message: string = error instanceof Error
    ? error.message
    : String(error);
  return message.replace(/"[^"]*"/g, '"..."').replace(/'[^']*'/g, "'...'");
}

function schemaDetail(error: unknown): string {
  const issues: unknown = (error as { issues?: unknown }).issues;
  if (Array.isArray(issues) && issues.length > 0) {
    const issue: { path?: unknown; message?: unknown } = issues[0] as {
      path?: unknown;
      message?: unknown;
    };
    const path: string = Array.isArray(issue.path) ? issue.path.join(".") : "";
    const message: string = typeof issue.message === "string"
      ? issue.message
      : "";
    return truncate((path + " " + message).trim());
  }
  return truncate(errorMessage(error));
}

function logInvalidOutput(
  provider: ProviderConfig,
  reason: InvalidOutputReason,
  detail: string,
): void {
  console.log(JSON.stringify({
    event: "invalid_output",
    provider: provider.id,
    model: provider.model,
    reason,
    detail,
  }));
}

function stripJsonFences(raw: string): string {
  const trimmed: string = raw.trim();
  if (!trimmed.startsWith("```")) {
    return trimmed;
  }
  let body: string = trimmed;
  if (body.startsWith("```json")) {
    body = body.slice("```json".length);
  } else {
    body = body.slice("```".length);
  }
  if (body.endsWith("```")) {
    body = body.slice(0, -"```".length);
  }
  return body.trim();
}

function readContent(payload: unknown): string {
  const choices: { message?: { content?: unknown } }[] | undefined =
    (payload as { choices?: { message?: { content?: unknown } }[] }).choices;
  const content: unknown = choices?.[0]?.message?.content;
  if (typeof content !== "string" || content.trim().length === 0) {
    throw new Error("the provider returned no content");
  }
  return content;
}

function buildRequestBody<T>(
  provider: ProviderConfig,
  args: GenerateStructuredArgs<T>,
): string {
  const strictOutput: boolean = provider.outputMode === "json_schema";
  const prompt: string = strictOutput
    ? args.prompt
    : withSchemaAppendix(args.prompt, args.jsonSchema);
  const responseFormat: Record<string, unknown> = strictOutput
    ? {
      type: "json_schema",
      json_schema: {
        name: args.schemaName,
        strict: true,
        schema: args.jsonSchema,
      },
    }
    : { type: "json_object" };
  return JSON.stringify({
    model: provider.model,
    temperature: 0,
    top_p: 0.95,
    messages: [{ role: "user", content: prompt }],
    response_format: responseFormat,
  });
}

function retryAfterSeconds(nowMs: number): number {
  let earliest: number | null = null;
  for (const until of exhaustedUntil.values()) {
    if (until > nowMs && (earliest === null || until < earliest)) {
      earliest = until;
    }
  }
  if (earliest === null) {
    return DEFAULT_RETRY_AFTER_SECONDS;
  }
  return Math.max(1, Math.round((earliest - nowMs) / 1000));
}

export async function generateStructured<T>(
  args: GenerateStructuredArgs<T>,
): Promise<GenerationResult<T>> {
  const providers: ProviderConfig[] = args.providers ?? PROVIDER_CHAIN;
  const fetchFn: typeof fetch = args.fetchFn ?? fetch;
  const readEnv: (name: string) => string | undefined = args.env ??
    ((name: string): string | undefined => Deno.env.get(name));
  const now: () => Date = args.now ?? ((): Date => new Date());

  for (const provider of providers) {
    const cooldownEnd: number | undefined = exhaustedUntil.get(provider.id);
    if (cooldownEnd !== undefined && cooldownEnd > now().getTime()) {
      continue;
    }
    const apiKey: string | undefined = readEnv(provider.apiKeyEnv);
    if (apiKey === undefined || apiKey.length === 0) {
      logProviderAttempt(provider, null, 0, "unauthorized");
      continue;
    }

    const startedAt: number = now().getTime();
    const controller: AbortController = new AbortController();
    const timer: number = setTimeout(
      (): void => controller.abort(),
      provider.timeoutMs,
    );
    let status: number | null = null;
    try {
      const response: Response = await fetchFn(
        provider.baseUrl + "chat/completions",
        {
          method: "POST",
          headers: {
            "Authorization": "Bearer " + apiKey,
            "Content-Type": "application/json",
          },
          body: buildRequestBody(provider, args),
          signal: controller.signal,
        },
      );
      status = response.status;
      if (response.status === 429) {
        exhaustedUntil.set(
          provider.id,
          provider.cooldownAfterRateLimit(now()).getTime(),
        );
        logProviderAttempt(
          provider,
          status,
          now().getTime() - startedAt,
          "rate_limited",
        );
        await response.body?.cancel();
        continue;
      }
      if (response.status === 401 || response.status === 403) {
        logProviderAttempt(
          provider,
          status,
          now().getTime() - startedAt,
          "unauthorized",
        );
        await response.body?.cancel();
        continue;
      }
      if (!response.ok) {
        logProviderAttempt(
          provider,
          status,
          now().getTime() - startedAt,
          "unavailable",
        );
        await response.body?.cancel();
        continue;
      }
      const payload: unknown = await response.json();
      const decoded: unknown = JSON.parse(
        stripJsonFences(readContent(payload)),
      );
      try {
        const value: T = args.parse(decoded);
        logProviderAttempt(provider, status, now().getTime() - startedAt, "ok");
        return { value, provider: provider.id, model: provider.model };
      } catch (error: unknown) {
        logInvalidOutput(provider, "schema", schemaDetail(error));
        logProviderAttempt(
          provider,
          status,
          now().getTime() - startedAt,
          "invalid_output",
        );
        continue;
      }
    } catch (error: unknown) {
      const outcome: ProviderOutcome = controller.signal.aborted
        ? "timeout"
        : status === null
        ? "unavailable"
        : "invalid_output";
      if (outcome === "invalid_output") {
        logInvalidOutput(provider, "json_parse", truncate(errorMessage(error)));
      }
      logProviderAttempt(
        provider,
        status,
        now().getTime() - startedAt,
        outcome,
      );
      continue;
    } finally {
      clearTimeout(timer);
    }
  }

  throw new ProvidersExhaustedError(retryAfterSeconds(now().getTime()));
}
