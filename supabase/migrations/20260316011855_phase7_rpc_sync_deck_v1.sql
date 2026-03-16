create or replace function public.sync_push(batch jsonb)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    v_account_id text;
    v_device_id text;
    v_item jsonb;
    v_payload jsonb;
    v_op_id text;
    v_entity_type text;
    v_entity_id text;
    v_operation_type text;
    v_lamport bigint;
    v_inserted_op_id text;
    v_accepted jsonb := '[]'::jsonb;
    v_rejected jsonb := '[]'::jsonb;
begin
    v_account_id := public.current_app_account_id();
    v_device_id := public.current_app_device_id();

    if v_account_id is null or v_device_id is null then
        raise exception 'Current device is not linked or is revoked';
    end if;

    if batch is null or jsonb_typeof(batch) <> 'array' then
        raise exception 'sync_push expects a JSON array';
    end if;

    for v_item in select value from jsonb_array_elements(batch)
    loop
        v_op_id := nullif(v_item->>'op_id', '');
        v_entity_type := lower(coalesce(v_item->>'entity_type', ''));
        v_entity_id := nullif(v_item->>'entity_id', '');
        v_operation_type := lower(coalesce(v_item->>'operation_type', ''));
        v_lamport := coalesce((v_item->>'lamport')::bigint, 0);
        v_payload := coalesce(v_item->'payload', '{}'::jsonb);

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
            v_account_id,
            v_op_id,
            v_entity_type,
            v_entity_id,
            v_operation_type,
            v_payload,
            v_lamport,
            v_device_id,
            now(),
            'accepted'
        )
        on conflict (app_account_id, op_id) do nothing
        returning op_id into v_inserted_op_id;

        if v_inserted_op_id is null then
            -- Idempotency: duplicate op is already accepted.
            v_accepted := v_accepted || jsonb_build_array(v_op_id);
            continue;
        end if;

        if v_operation_type not in ('create', 'update', 'upsert', 'delete') then
            v_rejected := v_rejected || jsonb_build_object(
                'op_id', v_op_id,
                'reason', 'unsupported_operation_type'
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
                v_account_id,
                v_entity_id,
                coalesce(nullif(v_payload->>'name', ''), '[deleted]'),
                v_payload->>'description',
                coalesce((v_payload->>'created_at')::timestamptz, now()),
                now(),
                coalesce((v_payload->>'deleted_at')::timestamptz, now()),
                v_device_id,
                v_device_id,
                greatest(v_lamport, 0)
            )
            on conflict (app_account_id, id) do update
            set
                deleted_at = excluded.deleted_at,
                updated_at = excluded.updated_at,
                last_modified_by_device_id = excluded.last_modified_by_device_id,
                version_lamport = excluded.version_lamport
            where excluded.version_lamport >= public.deck.version_lamport;
        else
            if nullif(v_payload->>'name', '') is null then
                v_rejected := v_rejected || jsonb_build_object(
                    'op_id', v_op_id,
                    'reason', 'missing_deck_name'
                );
                continue;
            end if;

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
                v_account_id,
                v_entity_id,
                v_payload->>'name',
                v_payload->>'description',
                coalesce((v_payload->>'created_at')::timestamptz, now()),
                coalesce((v_payload->>'updated_at')::timestamptz, now()),
                (v_payload->>'deleted_at')::timestamptz,
                v_device_id,
                v_device_id,
                greatest(v_lamport, 0)
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
        end if;

        v_accepted := v_accepted || jsonb_build_array(v_op_id);
        v_inserted_op_id := null;
    end loop;

    return jsonb_build_object(
        'accepted_op_ids', v_accepted,
        'rejected', v_rejected
    );
end;
$$;

create or replace function public.sync_pull(p_cursor bigint default 0, p_limit integer default 100)
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
begin
    v_account_id := public.current_app_account_id();

    if v_account_id is null then
        raise exception 'Current device is not linked or is revoked';
    end if;

    return query
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
    where s.app_account_id = v_account_id
      and s.cursor > coalesce(p_cursor, 0)
    order by s.cursor asc
    limit least(greatest(coalesce(p_limit, 100), 1), 500);
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
        value,
        now()
    from jsonb_array_elements_text(op_ids)
    on conflict (device_id, op_id) do nothing;

    get diagnostics v_count = row_count;
    return v_count;
end;
$$;

grant execute on function public.sync_push(jsonb) to authenticated;
grant execute on function public.sync_pull(bigint, integer) to authenticated;
grant execute on function public.sync_ack(jsonb) to authenticated;
