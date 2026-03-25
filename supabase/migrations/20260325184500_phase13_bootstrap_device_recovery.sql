create or replace function public.sync_bootstrap_anonymous(
    p_device_id text,
    p_device_name text default null,
    p_platform text default null
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    v_auth_user_id uuid;
    v_active public.app_device%rowtype;
    v_device public.app_device%rowtype;
    v_revoked public.app_device%rowtype;
    v_account_id text;
    v_created boolean := false;
    v_has_revoked boolean := false;
begin
    v_auth_user_id := auth.uid();
    if v_auth_user_id is null then
        raise exception 'Not authenticated';
    end if;

    select *
    into v_active
    from public.app_device d
    where d.auth_user_id = v_auth_user_id
      and d.revoked_at is null
    order by d.created_at asc
    limit 1;

    if found then
        update public.app_device
        set
            device_name = coalesce(nullif(p_device_name, ''), device_name),
            platform = coalesce(nullif(p_platform, ''), platform),
            last_seen_at = now()
        where id = v_active.id;

        return jsonb_build_object(
            'app_account_id', v_active.app_account_id,
            'app_device_id', v_active.id,
            'auth_user_id', v_auth_user_id::text,
            'created', false
        );
    end if;

    if nullif(p_device_id, '') is not null then
        select *
        into v_device
        from public.app_device d
        where d.id = p_device_id
        limit 1;

        if found then
            update public.app_device
            set
                auth_user_id = v_auth_user_id,
                device_name = coalesce(nullif(p_device_name, ''), device_name),
                platform = coalesce(nullif(p_platform, ''), platform),
                revoked_at = null,
                revoked_by_device_id = null,
                revoke_reason = null,
                last_seen_at = now()
            where id = v_device.id;

            update public.app_account
            set primary_device_id = coalesce(primary_device_id, v_device.id)
            where id = v_device.app_account_id;

            return jsonb_build_object(
                'app_account_id', v_device.app_account_id,
                'app_device_id', v_device.id,
                'auth_user_id', v_auth_user_id::text,
                'created', false
            );
        end if;
    end if;

    select *
    into v_revoked
    from public.app_device d
    where d.auth_user_id = v_auth_user_id
    order by d.created_at asc
    limit 1;
    v_has_revoked := found;

    v_account_id := gen_random_uuid()::text;

    insert into public.app_account (id, display_name, created_at)
    values (v_account_id, null, now());

    if v_has_revoked then
        update public.app_account
        set primary_device_id = null
        where primary_device_id = v_revoked.id;

        update public.app_device
        set
            app_account_id = v_account_id,
            device_name = coalesce(nullif(p_device_name, ''), device_name),
            platform = coalesce(nullif(p_platform, ''), platform),
            revoked_at = null,
            revoked_by_device_id = null,
            revoke_reason = null,
            last_seen_at = now()
        where id = v_revoked.id;
    else
        insert into public.app_device (
            id,
            app_account_id,
            auth_user_id,
            device_name,
            platform,
            created_at,
            last_seen_at
        ) values (
            coalesce(nullif(p_device_id, ''), gen_random_uuid()::text),
            v_account_id,
            v_auth_user_id,
            p_device_name,
            p_platform,
            now(),
            now()
        );
    end if;

    update public.app_account
    set primary_device_id = (
        select d.id
        from public.app_device d
        where d.auth_user_id = v_auth_user_id
          and d.app_account_id = v_account_id
        limit 1
    )
    where id = v_account_id;

    v_created := true;

    return (
        select jsonb_build_object(
            'app_account_id', d.app_account_id,
            'app_device_id', d.id,
            'auth_user_id', d.auth_user_id::text,
            'created', v_created
        )
        from public.app_device d
        where d.auth_user_id = v_auth_user_id
          and d.app_account_id = v_account_id
        limit 1
    );
end;
$$;
