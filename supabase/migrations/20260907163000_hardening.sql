create or replace function note_cache_hit(p_key text)
returns setof public.note_cache
language sql
security definer
set search_path = ''
as $$
  update public.note_cache
  set hits = hits + 1
  where cache_key = p_key
    and (expires_at is null or expires_at > now())
  returning *;
$$;

revoke all on table public.note_cache from anon, authenticated;
revoke all on table public.generation_events from anon, authenticated;
revoke all on table public.provider_state from anon, authenticated;
