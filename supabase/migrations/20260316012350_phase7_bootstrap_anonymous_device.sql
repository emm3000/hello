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
    v_existing public.app_device%rowtype;
    v_account_id text;
begin
    v_auth_user_id := auth.uid();
    if v_auth_user_id is null then
        raise exception 'Not authenticated';
    end if;

    select *
    into v_existing
    from public.app_device d
    where d.auth_user_id = v_auth_user_id
      and d.revoked_at is null
    limit 1;

    if found then
        return jsonb_build_object(
            'app_account_id', v_existing.app_account_id,
            'app_device_id', v_existing.id,
            'auth_user_id', v_existing.auth_user_id::text,
            'created', false
        );
    end if;

    v_account_id := gen_random_uuid()::text;

    insert into public.app_account (id, display_name, created_at)
    values (v_account_id, null, now());

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

    update public.app_account
    set primary_device_id = (
        select d.id
        from public.app_device d
        where d.auth_user_id = v_auth_user_id
          and d.app_account_id = v_account_id
        limit 1
    )
    where id = v_account_id;

    return (
        select jsonb_build_object(
            'app_account_id', d.app_account_id,
            'app_device_id', d.id,
            'auth_user_id', d.auth_user_id::text,
            'created', true
        )
        from public.app_device d
        where d.auth_user_id = v_auth_user_id
          and d.app_account_id = v_account_id
        limit 1
    );
end;
$$;

grant execute on function public.sync_bootstrap_anonymous(text, text, text) to authenticated;
