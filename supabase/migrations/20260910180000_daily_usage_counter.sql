create table public.daily_usage (
  user_id uuid not null,
  day date not null,
  used int not null default 0 check (used >= 0),
  primary key (user_id, day)
);

alter table public.daily_usage enable row level security;
revoke all on table public.daily_usage from anon, authenticated;

create or replace function public.consume_generation(
  p_user_id uuid,
  p_day date,
  p_allowance int
)
returns table (consumed boolean, used int)
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_used int;
begin
  insert into public.daily_usage (user_id, day, used)
  values (p_user_id, p_day, 0)
  on conflict (user_id, day) do nothing;

  update public.daily_usage
  set used = public.daily_usage.used + 1
  where user_id = p_user_id
    and day = p_day
    and public.daily_usage.used < p_allowance
  returning public.daily_usage.used into v_used;

  if v_used is not null then
    return query select true, v_used;
    return;
  end if;

  select u.used into v_used
  from public.daily_usage u
  where u.user_id = p_user_id and u.day = p_day;
  return query select false, v_used;
end;
$$;

revoke execute on function public.consume_generation(uuid, date, int) from public;
revoke execute on function public.consume_generation(uuid, date, int) from anon;
revoke execute on function public.consume_generation(uuid, date, int) from authenticated;
grant execute on function public.consume_generation(uuid, date, int) to service_role;

drop function if exists public.reserve_generation(uuid, text, text, int, int, timestamptz, timestamptz);
