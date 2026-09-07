alter table generation_events add constraint generation_events_outcome_check check (outcome in ('pending', 'success', 'refusal', 'providers_exhausted', 'credits_exhausted', 'error'));

create function reserve_generation(
  p_user_id uuid,
  p_operation text,
  p_cache_key text,
  p_allowance int,
  p_refusal_allowance int,
  p_day_start timestamptz
)
returns table (reserved boolean, event_id bigint, charged int, refused int)
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_charged int;
  v_refused int;
  v_event_id bigint;
begin
  perform pg_advisory_xact_lock(hashtext(p_user_id::text));

  select
    count(*) filter (where outcome in ('success', 'pending')),
    count(*) filter (where outcome = 'refusal')
  into v_charged, v_refused
  from public.generation_events
  where user_id = p_user_id
    and cached = false
    and created_at >= p_day_start;

  if v_charged < p_allowance and v_refused < p_refusal_allowance then
    insert into public.generation_events (user_id, operation, cache_key, provider, model, cached, outcome)
    values (p_user_id, p_operation, p_cache_key, null, null, false, 'pending')
    returning id into v_event_id;
    return query select true, v_event_id, v_charged + 1, v_refused;
  else
    insert into public.generation_events (user_id, operation, cache_key, provider, model, cached, outcome)
    values (p_user_id, p_operation, p_cache_key, null, null, false, 'credits_exhausted');
    return query select false, null::bigint, v_charged, v_refused;
  end if;
end;
$$;

revoke execute on function reserve_generation(uuid, text, text, int, int, timestamptz) from public;
revoke execute on function reserve_generation(uuid, text, text, int, int, timestamptz) from anon;
revoke execute on function reserve_generation(uuid, text, text, int, int, timestamptz) from authenticated;
grant execute on function reserve_generation(uuid, text, text, int, int, timestamptz) to service_role;
