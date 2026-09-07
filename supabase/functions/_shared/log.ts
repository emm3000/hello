import { type GenerationOperation, type GenerationOutcome } from "./credits.ts";

export function logRequest(
  operation: GenerationOperation,
  cached: boolean,
  outcome: GenerationOutcome,
  startedAt: number,
): void {
  console.info(JSON.stringify({
    op: operation,
    cached,
    outcome,
    ms: Math.round(performance.now() - startedAt),
  }));
}
