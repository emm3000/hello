create table note_cache (
  cache_key text primary key,
  response jsonb not null,
  success boolean not null,
  provider text not null,
  model text not null,
  prompt_version int not null,
  schema_version int not null,
  hits int not null default 0,
  expires_at timestamptz,
  created_at timestamptz not null default now()
);

create table generation_events (
  id bigint generated always as identity primary key,
  user_id uuid not null,
  operation text not null,
  cache_key text,
  provider text,
  model text,
  cached boolean not null,
  outcome text not null,
  created_at timestamptz not null default now()
);

create index generation_events_user_day on generation_events (user_id, created_at desc);

create table provider_state (
  provider_id text primary key,
  exhausted_until timestamptz
);

alter table note_cache enable row level security;
alter table generation_events enable row level security;
alter table provider_state enable row level security;

create function note_cache_hit(p_key text)
returns setof note_cache
language sql
security definer
set search_path = public
as $$
  update note_cache
  set hits = hits + 1
  where cache_key = p_key
    and (expires_at is null or expires_at > now())
  returning *;
$$;

revoke execute on function note_cache_hit(text) from public;
revoke execute on function note_cache_hit(text) from anon;
revoke execute on function note_cache_hit(text) from authenticated;
grant execute on function note_cache_hit(text) to service_role;
