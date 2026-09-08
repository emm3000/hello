create or replace function public.heartbeat()
returns timestamptz
language sql
stable
set search_path = ''
as $$
  select now();
$$;

revoke execute on function public.heartbeat() from public;
grant execute on function public.heartbeat() to anon;
