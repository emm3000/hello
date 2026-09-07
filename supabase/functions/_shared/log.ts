import { type GenerationOperation, type GenerationOutcome } from "./credits.ts";

export type LogOutcome = GenerationOutcome | "credits_unavailable";

export function logRequest(
  operation: GenerationOperation,
  cached: boolean,
  outcome: LogOutcome,
  startedAt: number,
): void {
  console.info(JSON.stringify({
    op: operation,
    cached,
    outcome,
    ms: Math.round(performance.now() - startedAt),
  }));
}
