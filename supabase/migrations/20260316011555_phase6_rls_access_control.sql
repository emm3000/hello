alter table public.app_device
    add column if not exists revoked_by_device_id text references public.app_device(id),
    add column if not exists revoke_reason text;

create or replace function public.current_app_account_id()
returns text
language sql
stable
security definer
set search_path = public
as $$
    select d.app_account_id
    from public.app_device d
    where d.auth_user_id = auth.uid()
      and d.revoked_at is null
    order by d.created_at asc
    limit 1
$$;

create or replace function public.current_app_device_id()
returns text
language sql
stable
security definer
set search_path = public
as $$
    select d.id
    from public.app_device d
    where d.auth_user_id = auth.uid()
      and d.revoked_at is null
    order by d.created_at asc
    limit 1
$$;

create or replace function public.is_account_member(target_app_account_id text)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select exists (
        select 1
        from public.app_device d
        where d.auth_user_id = auth.uid()
          and d.app_account_id = target_app_account_id
          and d.revoked_at is null
    )
$$;

grant execute on function public.current_app_account_id() to anon, authenticated;
grant execute on function public.current_app_device_id() to anon, authenticated;
grant execute on function public.is_account_member(text) to anon, authenticated;

alter table public.app_account enable row level security;
alter table public.app_device enable row level security;
alter table public.pairing_session enable row level security;
alter table public.deck enable row level security;
alter table public.flashcard enable row level security;
alter table public.flashcard_example enable row level security;
alter table public.review_event enable row level security;
alter table public.quote enable row level security;
alter table public.sync_operation enable row level security;
alter table public.sync_ack enable row level security;
alter table public.sync_cursor enable row level security;

revoke all on table public.app_account from anon, authenticated;
revoke all on table public.app_device from anon, authenticated;
revoke all on table public.pairing_session from anon, authenticated;
revoke all on table public.deck from anon, authenticated;
revoke all on table public.flashcard from anon, authenticated;
revoke all on table public.flashcard_example from anon, authenticated;
revoke all on table public.review_event from anon, authenticated;
revoke all on table public.quote from anon, authenticated;
revoke all on table public.sync_operation from anon, authenticated;
revoke all on table public.sync_ack from anon, authenticated;
revoke all on table public.sync_cursor from anon, authenticated;

grant select on table public.app_account to authenticated;
grant select on table public.app_device to authenticated;
grant update (revoked_at, revoked_by_device_id, revoke_reason, last_seen_at) on table public.app_device to authenticated;
grant select on table public.pairing_session to authenticated;
grant select on table public.deck to authenticated;
grant select on table public.flashcard to authenticated;
grant select on table public.flashcard_example to authenticated;
grant select on table public.review_event to authenticated;
grant select on table public.quote to authenticated;
grant select on table public.sync_operation to authenticated;
grant select on table public.sync_ack to authenticated;
grant select on table public.sync_cursor to authenticated;

drop policy if exists app_account_select_member on public.app_account;
create policy app_account_select_member
    on public.app_account
    for select
    to authenticated
    using (id = public.current_app_account_id());

drop policy if exists app_device_select_member on public.app_device;
create policy app_device_select_member
    on public.app_device
    for select
    to authenticated
    using (public.is_account_member(app_account_id));

drop policy if exists app_device_update_member on public.app_device;
create policy app_device_update_member
    on public.app_device
    for update
    to authenticated
    using (public.is_account_member(app_account_id))
    with check (public.is_account_member(app_account_id));

drop policy if exists pairing_session_select_member on public.pairing_session;
create policy pairing_session_select_member
    on public.pairing_session
    for select
    to authenticated
    using (public.is_account_member(app_account_id));

drop policy if exists deck_select_member on public.deck;
create policy deck_select_member
    on public.deck
    for select
    to authenticated
    using (public.is_account_member(app_account_id));

drop policy if exists flashcard_select_member on public.flashcard;
create policy flashcard_select_member
    on public.flashcard
    for select
    to authenticated
    using (public.is_account_member(app_account_id));

drop policy if exists flashcard_example_select_member on public.flashcard_example;
create policy flashcard_example_select_member
    on public.flashcard_example
    for select
    to authenticated
    using (public.is_account_member(app_account_id));

drop policy if exists review_event_select_member on public.review_event;
create policy review_event_select_member
    on public.review_event
    for select
    to authenticated
    using (public.is_account_member(app_account_id));

drop policy if exists quote_select_member on public.quote;
create policy quote_select_member
    on public.quote
    for select
    to authenticated
    using (public.is_account_member(app_account_id));

drop policy if exists sync_operation_select_member on public.sync_operation;
create policy sync_operation_select_member
    on public.sync_operation
    for select
    to authenticated
    using (public.is_account_member(app_account_id));

drop policy if exists sync_ack_select_member on public.sync_ack;
create policy sync_ack_select_member
    on public.sync_ack
    for select
    to authenticated
    using (public.is_account_member(app_account_id));

drop policy if exists sync_cursor_select_member on public.sync_cursor;
create policy sync_cursor_select_member
    on public.sync_cursor
    for select
    to authenticated
    using (public.is_account_member(app_account_id));
