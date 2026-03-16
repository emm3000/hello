create extension if not exists pgcrypto;

create table if not exists public.app_account (
    id text primary key,
    display_name text,
    primary_device_id text,
    created_at timestamptz not null default now()
);

create table if not exists public.app_device (
    id text primary key,
    app_account_id text not null references public.app_account(id) on delete cascade,
    auth_user_id uuid not null references auth.users(id) on delete cascade,
    device_name text,
    platform text,
    created_at timestamptz not null default now(),
    revoked_at timestamptz,
    last_seen_at timestamptz,
    unique (auth_user_id)
);

alter table public.app_account
    drop constraint if exists app_account_primary_device_id_fkey;

alter table public.app_account
    add constraint app_account_primary_device_id_fkey
    foreign key (primary_device_id) references public.app_device(id) on delete set null;

create table if not exists public.pairing_session (
    id text primary key,
    app_account_id text not null references public.app_account(id) on delete cascade,
    created_by_device_id text not null references public.app_device(id) on delete cascade,
    code_hash text not null,
    expires_at timestamptz not null,
    used_at timestamptz,
    created_at timestamptz not null default now()
);

create table if not exists public.deck (
    app_account_id text not null references public.app_account(id) on delete cascade,
    id text not null,
    name text not null,
    description text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted_at timestamptz,
    origin_device_id text not null references public.app_device(id),
    last_modified_by_device_id text not null references public.app_device(id),
    version_lamport bigint not null,
    primary key (app_account_id, id)
);

create table if not exists public.flashcard (
    app_account_id text not null references public.app_account(id) on delete cascade,
    id text not null,
    deck_id text not null,
    word text not null,
    meaning text not null,
    translation text,
    phonetic text,
    part_of_speech text,
    type text,
    note text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted_at timestamptz,
    origin_device_id text not null references public.app_device(id),
    last_modified_by_device_id text not null references public.app_device(id),
    version_lamport bigint not null,
    primary key (app_account_id, id),
    constraint flashcard_deck_fkey
        foreign key (app_account_id, deck_id)
        references public.deck(app_account_id, id)
        on delete cascade
);

create table if not exists public.flashcard_example (
    app_account_id text not null references public.app_account(id) on delete cascade,
    id text not null,
    flashcard_id text not null,
    text text not null,
    translation text not null,
    type text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted_at timestamptz,
    origin_device_id text not null references public.app_device(id),
    last_modified_by_device_id text not null references public.app_device(id),
    version_lamport bigint not null,
    primary key (app_account_id, id),
    constraint flashcard_example_flashcard_fkey
        foreign key (app_account_id, flashcard_id)
        references public.flashcard(app_account_id, id)
        on delete cascade
);

create table if not exists public.review_event (
    app_account_id text not null references public.app_account(id) on delete cascade,
    id text not null,
    flashcard_id text not null,
    grade text not null,
    reviewed_at timestamptz not null,
    next_review_at timestamptz not null,
    ease_factor double precision not null,
    interval_days integer not null,
    repetitions integer not null,
    lapses integer not null,
    created_at timestamptz not null default now(),
    origin_device_id text not null references public.app_device(id),
    last_modified_by_device_id text not null references public.app_device(id),
    version_lamport bigint not null,
    primary key (app_account_id, id),
    constraint review_event_flashcard_fkey
        foreign key (app_account_id, flashcard_id)
        references public.flashcard(app_account_id, id)
        on delete cascade
);

create table if not exists public.quote (
    app_account_id text not null references public.app_account(id) on delete cascade,
    id text not null,
    title text not null,
    phrase text not null,
    description text not null,
    translation text not null,
    example text not null,
    context text not null,
    pronunciation text not null,
    formality text not null,
    tags text not null,
    category text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted_at timestamptz,
    origin_device_id text not null references public.app_device(id),
    last_modified_by_device_id text not null references public.app_device(id),
    version_lamport bigint not null,
    primary key (app_account_id, id)
);

create table if not exists public.sync_operation (
    cursor bigint generated by default as identity primary key,
    app_account_id text not null references public.app_account(id) on delete cascade,
    op_id text not null,
    entity_type text not null,
    entity_id text not null,
    operation_type text not null,
    payload jsonb not null,
    lamport bigint not null,
    origin_device_id text not null references public.app_device(id),
    created_at timestamptz not null default now(),
    status text not null default 'accepted',
    unique (app_account_id, op_id)
);

create table if not exists public.sync_ack (
    id bigint generated by default as identity primary key,
    app_account_id text not null references public.app_account(id) on delete cascade,
    device_id text not null references public.app_device(id) on delete cascade,
    op_id text not null,
    acked_at timestamptz not null default now(),
    unique (device_id, op_id)
);

create table if not exists public.sync_cursor (
    id bigint generated by default as identity primary key,
    app_account_id text not null references public.app_account(id) on delete cascade,
    device_id text not null references public.app_device(id) on delete cascade,
    last_pulled_cursor bigint not null default 0,
    updated_at timestamptz not null default now(),
    unique (device_id)
);

create index if not exists idx_app_device_app_account_id
    on public.app_device (app_account_id);

create index if not exists idx_pairing_session_app_account_id
    on public.pairing_session (app_account_id);

create index if not exists idx_pairing_session_expires_at
    on public.pairing_session (expires_at);

create index if not exists idx_deck_app_account_id
    on public.deck (app_account_id);

create index if not exists idx_deck_created_at
    on public.deck (created_at);

create index if not exists idx_flashcard_app_account_id
    on public.flashcard (app_account_id);

create index if not exists idx_flashcard_deck_id
    on public.flashcard (app_account_id, deck_id);

create index if not exists idx_flashcard_created_at
    on public.flashcard (created_at);

create index if not exists idx_flashcard_example_app_account_id
    on public.flashcard_example (app_account_id);

create index if not exists idx_flashcard_example_flashcard_id
    on public.flashcard_example (app_account_id, flashcard_id);

create index if not exists idx_flashcard_example_created_at
    on public.flashcard_example (created_at);

create index if not exists idx_review_event_app_account_id
    on public.review_event (app_account_id);

create index if not exists idx_review_event_flashcard_id
    on public.review_event (app_account_id, flashcard_id);

create index if not exists idx_review_event_created_at
    on public.review_event (created_at);

create index if not exists idx_quote_app_account_id
    on public.quote (app_account_id);

create index if not exists idx_quote_created_at
    on public.quote (created_at);

create index if not exists idx_sync_operation_app_account_id
    on public.sync_operation (app_account_id);

create index if not exists idx_sync_operation_entity_id
    on public.sync_operation (entity_id);

create index if not exists idx_sync_operation_lamport
    on public.sync_operation (lamport);

create index if not exists idx_sync_operation_created_at
    on public.sync_operation (created_at);

create index if not exists idx_sync_operation_account_cursor
    on public.sync_operation (app_account_id, cursor);

create index if not exists idx_sync_ack_app_account_id
    on public.sync_ack (app_account_id);

create index if not exists idx_sync_cursor_app_account_id
    on public.sync_cursor (app_account_id);
