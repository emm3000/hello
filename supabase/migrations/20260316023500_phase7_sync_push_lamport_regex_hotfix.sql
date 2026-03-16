-- Hotfix: accept numeric lamport values correctly in sync_push_internal
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
