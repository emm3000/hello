import { type SupabaseClient } from "npm:@supabase/supabase-js@2";
import { type ProviderStateStore } from "./providers.ts";

type ProviderStateRow = {
  provider_id: string;
  exhausted_until: string | null;
};

type QueryResult = { data: unknown; error: { message: string } | null };

export function createProviderStateStore(
  client: SupabaseClient,
): ProviderStateStore {
  return {
    async load(): Promise<Map<string, number>> {
      const state: Map<string, number> = new Map<string, number>();
      try {
        const { data, error }: QueryResult = await client
          .from("provider_state")
          .select("provider_id, exhausted_until");
        if (error !== null) {
          console.error("ProviderStateFailure");
          return state;
        }
        const rows: ProviderStateRow[] = Array.isArray(data)
          ? data as ProviderStateRow[]
          : [];
        const nowMs: number = Date.now();
        for (const row of rows) {
          if (row.exhausted_until === null) {
            continue;
          }
          const untilMs: number = Date.parse(row.exhausted_until);
          if (Number.isNaN(untilMs) || untilMs <= nowMs) {
            continue;
          }
          state.set(row.provider_id, untilMs);
        }
        return state;
      } catch (_error: unknown) {
        console.error("ProviderStateFailure");
        return state;
      }
    },
    async markExhausted(providerId: string, untilMs: number): Promise<void> {
      try {
        const { error }: QueryResult = await client.from("provider_state")
          .upsert(
            {
              provider_id: providerId,
              exhausted_until: new Date(untilMs).toISOString(),
            },
            { onConflict: "provider_id" },
          );
        if (error !== null) {
          console.error("ProviderStateFailure");
        }
      } catch (_error: unknown) {
        console.error("ProviderStateFailure");
      }
    },
  };
}
