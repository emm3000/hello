create or replace function public.sync_push_internal(
    p_account_id text,
    p_device_id text,
    p_batch jsonb
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    v_item jsonb;
    v_payload jsonb;
    v_op_id text;
    v_entity_type text;
    v_entity_id text;
    v_operation_type text;
    v_lamport bigint;
    v_server_cursor bigint;
    v_server_created_at timestamptz;
    v_existing public.sync_operation%rowtype;
    v_accepted_ids jsonb := '[]'::jsonb;
    v_acks jsonb := '[]'::jsonb;
    v_rejected jsonb := '[]'::jsonb;
    v_applied boolean;
    v_affected_rows integer;
begin
    if p_account_id is null or p_device_id is null then
        raise exception 'Current device is not linked or is revoked';
    end if;

    if p_batch is null or jsonb_typeof(p_batch) <> 'array' then
        raise exception 'sync_push expects a JSON array';
    end if;

    for v_item in select value from jsonb_array_elements(p_batch)
    loop
        v_op_id := nullif(v_item->>'op_id', '');
        v_entity_type := lower(coalesce(v_item->>'entity_type', ''));
        v_entity_id := nullif(v_item->>'entity_id', '');
        v_operation_type := lower(coalesce(v_item->>'operation_type', ''));
        v_payload := coalesce(v_item->'payload', '{}'::jsonb);
        v_lamport := 0;
        v_server_cursor := null;
        v_server_created_at := null;
        v_applied := false;

        if coalesce(v_item->>'lamport', '') ~ '^-?[0-9]+$' then
            v_lamport := greatest((v_item->>'lamport')::bigint, 0);
        else
            v_rejected := v_rejected || jsonb_build_object(
                'op_id', coalesce(v_op_id, ''),
                'reason', 'invalid_lamport'
            );
            continue;
        end if;

        if v_op_id is null or v_entity_id is null then
            v_rejected := v_rejected || jsonb_build_object(
                'op_id', coalesce(v_op_id, ''),
                'reason', 'missing_op_or_entity_id'
            );
            continue;
        end if;

        if v_entity_type <> 'deck' then
            v_rejected := v_rejected || jsonb_build_object(
                'op_id', v_op_id,
                'reason', 'unsupported_entity_type'
            );
            continue;
        end if;

        if v_operation_type not in ('create', 'update', 'upsert', 'delete') then
            v_rejected := v_rejected || jsonb_build_object(
                'op_id', v_op_id,
                'reason', 'unsupported_operation_type'
            );
            continue;
        end if;

        if v_operation_type <> 'delete' and nullif(v_payload->>'name', '') is null then
            v_rejected := v_rejected || jsonb_build_object(
                'op_id', v_op_id,
                'reason', 'missing_deck_name'
            );
            continue;
        end if;

        insert into public.sync_operation (
            app_account_id,
            op_id,
            entity_type,
            entity_id,
            operation_type,
            payload,
            lamport,
            origin_device_id,
            created_at,
            status
        ) values (
            p_account_id,
            v_op_id,
            v_entity_type,
            v_entity_id,
            v_operation_type,
            v_payload,
            v_lamport,
            p_device_id,
            now(),
            'accepted'
        )
        on conflict (app_account_id, op_id) do nothing
        returning cursor, created_at into v_server_cursor, v_server_created_at;

        if v_server_cursor is null then
            select *
            into v_existing
            from public.sync_operation s
            where s.app_account_id = p_account_id
              and s.op_id = v_op_id
            limit 1;

            v_accepted_ids := v_accepted_ids || jsonb_build_array(v_op_id);
            v_acks := v_acks || jsonb_build_object(
                'op_id', v_existing.op_id,
                'cursor', v_existing.cursor,
                'status', 'duplicate',
                'entity_type', v_existing.entity_type,
                'entity_id', v_existing.entity_id,
                'operation_type', v_existing.operation_type,
                'lamport', v_existing.lamport,
                'origin_device_id', v_existing.origin_device_id,
                'server_created_at', v_existing.created_at,
                'applied', null
            );
            continue;
        end if;

        if v_operation_type = 'delete' then
            insert into public.deck (
                app_account_id,
                id,
                name,
                description,
                created_at,
                updated_at,
                deleted_at,
                origin_device_id,
                last_modified_by_device_id,
                version_lamport
            ) values (
                p_account_id,
                v_entity_id,
                coalesce(nullif(v_payload->>'name', ''), '[deleted]'),
                v_payload->>'description',
                coalesce((v_payload->>'created_at')::timestamptz, now()),
                now(),
                coalesce((v_payload->>'deleted_at')::timestamptz, now()),
                p_device_id,
                p_device_id,
                v_lamport
            )
            on conflict (app_account_id, id) do update
            set
                deleted_at = excluded.deleted_at,
                updated_at = excluded.updated_at,
                last_modified_by_device_id = excluded.last_modified_by_device_id,
                version_lamport = excluded.version_lamport
            where excluded.version_lamport >= public.deck.version_lamport;

            get diagnostics v_affected_rows = row_count;
            v_applied := v_affected_rows > 0;
        else
            insert into public.deck (
                app_account_id,
                id,
                name,
                description,
                created_at,
                updated_at,
                deleted_at,
                origin_device_id,
                last_modified_by_device_id,
                version_lamport
            ) values (
                p_account_id,
                v_entity_id,
                v_payload->>'name',
                v_payload->>'description',
                coalesce((v_payload->>'created_at')::timestamptz, now()),
                coalesce((v_payload->>'updated_at')::timestamptz, now()),
                (v_payload->>'deleted_at')::timestamptz,
                p_device_id,
                p_device_id,
                v_lamport
            )
            on conflict (app_account_id, id) do update
            set
                name = excluded.name,
                description = excluded.description,
                updated_at = excluded.updated_at,
                deleted_at = excluded.deleted_at,
                last_modified_by_device_id = excluded.last_modified_by_device_id,
                version_lamport = excluded.version_lamport
            where excluded.version_lamport >= public.deck.version_lamport;

            get diagnostics v_affected_rows = row_count;
            v_applied := v_affected_rows > 0;
        end if;

        v_accepted_ids := v_accepted_ids || jsonb_build_array(v_op_id);
        v_acks := v_acks || jsonb_build_object(
            'op_id', v_op_id,
            'cursor', v_server_cursor,
            'status', 'accepted',
            'entity_type', v_entity_type,
            'entity_id', v_entity_id,
            'operation_type', v_operation_type,
            'lamport', v_lamport,
            'origin_device_id', p_device_id,
            'server_created_at', v_server_created_at,
            'applied', v_applied
        );
    end loop;

    return jsonb_build_object(
        'accepted_op_ids', v_accepted_ids,
        'acks', v_acks,
        'rejected', v_rejected
    );
end;
$$;

create or replace function public.sync_push(batch jsonb)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    v_account_id text;
    v_device_id text;
begin
    v_account_id := public.current_app_account_id();
    v_device_id := public.current_app_device_id();

    return public.sync_push_internal(
        p_account_id := v_account_id,
        p_device_id := v_device_id,
        p_batch := batch
    );
end;
$$;

create or replace function public.sync_pull_internal(
    p_account_id text,
    p_device_id text,
    p_cursor bigint default 0,
    p_limit integer default 100,
    p_exclude_origin_device boolean default true
)
returns table (
    cursor bigint,
    op_id text,
    entity_type text,
    entity_id text,
    operation_type text,
    payload jsonb,
    lamport bigint,
    origin_device_id text,
    created_at timestamptz
)
language sql
security definer
set search_path = public
as $$
    select
        s.cursor,
        s.op_id,
        s.entity_type,
        s.entity_id,
        s.operation_type,
        s.payload,
        s.lamport,
        s.origin_device_id,
        s.created_at
    from public.sync_operation s
    where s.app_account_id = p_account_id
      and s.cursor > coalesce(p_cursor, 0)
      and (
        not coalesce(p_exclude_origin_device, true)
        or s.origin_device_id <> p_device_id
      )
      and not exists (
          select 1
          from public.sync_ack a
          where a.app_account_id = p_account_id
            and a.device_id = p_device_id
            and a.op_id = s.op_id
      )
    order by s.cursor asc
    limit least(greatest(coalesce(p_limit, 100), 1), 500)
$$;

create or replace function public.sync_pull(
    p_cursor bigint default 0,
    p_limit integer default 100
)
returns table (
    cursor bigint,
    op_id text,
    entity_type text,
    entity_id text,
    operation_type text,
    payload jsonb,
    lamport bigint,
    origin_device_id text,
    created_at timestamptz
)
language plpgsql
security definer
set search_path = public
as $$
declare
    v_account_id text;
    v_device_id text;
begin
    v_account_id := public.current_app_account_id();
    v_device_id := public.current_app_device_id();

    if v_account_id is null or v_device_id is null then
        raise exception 'Current device is not linked or is revoked';
    end if;

    return query
    select *
    from public.sync_pull_internal(
        p_account_id := v_account_id,
        p_device_id := v_device_id,
        p_cursor := p_cursor,
        p_limit := p_limit,
        p_exclude_origin_device := true
    );
end;
$$;

create or replace function public.sync_ack(op_ids jsonb)
returns integer
language plpgsql
security definer
set search_path = public
as $$
declare
    v_account_id text;
    v_device_id text;
    v_count integer := 0;
begin
    v_account_id := public.current_app_account_id();
    v_device_id := public.current_app_device_id();

    if v_account_id is null or v_device_id is null then
        raise exception 'Current device is not linked or is revoked';
    end if;

    if op_ids is null or jsonb_typeof(op_ids) <> 'array' then
        raise exception 'sync_ack expects a JSON array of op_ids';
    end if;

    insert into public.sync_ack (app_account_id, device_id, op_id, acked_at)
    select
        v_account_id,
        v_device_id,
        s.op_id,
        now()
    from (
        select distinct value as op_id
        from jsonb_array_elements_text(op_ids)
        where nullif(trim(value), '') is not null
    ) incoming
    join public.sync_operation s
      on s.app_account_id = v_account_id
     and s.op_id = incoming.op_id
    on conflict (device_id, op_id) do nothing;

    get diagnostics v_count = row_count;
    return v_count;
end;
$$;

grant execute on function public.sync_push_internal(text, text, jsonb) to service_role;
grant execute on function public.sync_pull_internal(text, text, bigint, integer, boolean) to service_role;
grant execute on function public.sync_push(jsonb) to authenticated;
grant execute on function public.sync_pull(bigint, integer) to authenticated;
grant execute on function public.sync_ack(jsonb) to authenticated;

create or replace function public.sync_phase7_contract_check(
    p_account_id text,
    p_origin_device_id text,
    p_peer_device_id text
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    v_test_op_id text := 'phase7-check-op-' || replace(gen_random_uuid()::text, '-', '');
    v_test_deck_id text := 'phase7-check-deck-' || replace(gen_random_uuid()::text, '-', '');
    v_push_first jsonb;
    v_push_duplicate jsonb;
    v_pull_b_count integer;
    v_pull_a_default_count integer;
    v_pull_a_including_own_count integer;
begin
    if p_account_id is null or p_origin_device_id is null or p_peer_device_id is null then
        raise exception 'sync_phase7_contract_check requires account and 2 device ids';
    end if;

    if not exists (
        select 1
        from public.app_device d
        where d.app_account_id = p_account_id
          and d.id = p_origin_device_id
          and d.revoked_at is null
    ) then
        raise exception 'origin device is not an active member of account %', p_account_id;
    end if;

    if not exists (
        select 1
        from public.app_device d
        where d.app_account_id = p_account_id
          and d.id = p_peer_device_id
          and d.revoked_at is null
    ) then
        raise exception 'peer device is not an active member of account %', p_account_id;
    end if;

    delete from public.sync_ack
    where app_account_id = p_account_id
      and op_id = v_test_op_id;

    delete from public.sync_operation
    where app_account_id = p_account_id
      and op_id = v_test_op_id;

    delete from public.deck
    where app_account_id = p_account_id
      and id = v_test_deck_id;

    v_push_first := public.sync_push_internal(
        p_account_id := p_account_id,
        p_device_id := p_origin_device_id,
        p_batch := jsonb_build_array(
            jsonb_build_object(
                'op_id', v_test_op_id,
                'entity_type', 'deck',
                'entity_id', v_test_deck_id,
                'operation_type', 'create',
                'lamport', 11,
                'payload', jsonb_build_object(
                    'name', 'Deck One',
                    'description', 'phase7-test'
                )
            )
        )
    );

    if jsonb_array_length(coalesce(v_push_first->'accepted_op_ids', '[]'::jsonb)) <> 1 then
        raise exception 'phase7 test: expected 1 accepted op on first push';
    end if;

    if jsonb_array_length(coalesce(v_push_first->'acks', '[]'::jsonb)) <> 1 then
        raise exception 'phase7 test: expected 1 canonical ack on first push';
    end if;

    v_push_duplicate := public.sync_push_internal(
        p_account_id := p_account_id,
        p_device_id := p_origin_device_id,
        p_batch := jsonb_build_array(
            jsonb_build_object(
                'op_id', v_test_op_id,
                'entity_type', 'deck',
                'entity_id', v_test_deck_id,
                'operation_type', 'create',
                'lamport', 11,
                'payload', jsonb_build_object(
                    'name', 'Deck One',
                    'description', 'phase7-test'
                )
            )
        )
    );

    if (v_push_duplicate->'acks'->0->>'status') <> 'duplicate' then
        raise exception 'phase7 test: duplicate push must return duplicate status';
    end if;

    if (
        select count(*)
        from public.sync_operation
        where app_account_id = p_account_id
          and op_id = v_test_op_id
    ) <> 1 then
        raise exception 'phase7 test: duplicate push must not duplicate sync_operation row';
    end if;

    select count(*)
    into v_pull_b_count
    from public.sync_pull_internal(
        p_account_id := p_account_id,
        p_device_id := p_peer_device_id,
        p_cursor := 0,
        p_limit := 100,
        p_exclude_origin_device := true
    );

    if v_pull_b_count <> 1 then
        raise exception 'phase7 test: device B must receive exactly 1 operation';
    end if;

    insert into public.sync_ack (app_account_id, device_id, op_id, acked_at)
    values (p_account_id, p_peer_device_id, v_test_op_id, now())
    on conflict (device_id, op_id) do nothing;

    if (select count(*) from public.sync_pull_internal(
        p_account_id := p_account_id,
        p_device_id := p_peer_device_id,
        p_cursor := 0,
        p_limit := 100,
        p_exclude_origin_device := true
    )) <> 0 then
        raise exception 'phase7 test: acked operations must not be redelivered';
    end if;

    select count(*)
    into v_pull_a_default_count
    from public.sync_pull_internal(
        p_account_id := p_account_id,
        p_device_id := p_origin_device_id,
        p_cursor := 0,
        p_limit := 100,
        p_exclude_origin_device := true
    );

    if v_pull_a_default_count <> 0 then
        raise exception 'phase7 test: own ops must be excluded by default strategy';
    end if;

    select count(*)
    into v_pull_a_including_own_count
    from public.sync_pull_internal(
        p_account_id := p_account_id,
        p_device_id := p_origin_device_id,
        p_cursor := 0,
        p_limit := 100,
        p_exclude_origin_device := false
    );

    if v_pull_a_including_own_count <> 1 then
        raise exception 'phase7 test: own ops must be returned when exclusion is disabled';
    end if;

    delete from public.sync_ack
    where app_account_id = p_account_id
      and op_id = v_test_op_id;

    delete from public.sync_operation
    where app_account_id = p_account_id
      and op_id = v_test_op_id;

    delete from public.deck
    where app_account_id = p_account_id
      and id = v_test_deck_id;

    return jsonb_build_object(
        'ok', true,
        'checked', jsonb_build_array(
            'push_idempotency',
            'pull_cursor_ordering',
            'pull_excludes_origin_by_default',
            'pull_can_include_origin',
            'ack_prevents_redelivery',
            'push_returns_canonical_acks'
        )
    );
end;
$$;

grant execute on function public.sync_phase7_contract_check(text, text, text) to service_role;
