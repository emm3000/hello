alter table public.pairing_session
    add column if not exists used_by_device_id text references public.app_device(id);

create or replace function public.create_pairing_session(p_ttl_minutes integer default 10)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    v_account_id text;
    v_device_id text;
    v_session_id text;
    v_code text;
    v_expires_at timestamptz;
begin
    v_account_id := public.current_app_account_id();
    v_device_id := public.current_app_device_id();

    if v_account_id is null or v_device_id is null then
        raise exception 'Current device is not linked or is revoked';
    end if;

    v_code := lpad(((random() * 1000000)::int % 1000000)::text, 6, '0');
    v_session_id := gen_random_uuid()::text;
    v_expires_at := now() + make_interval(mins => greatest(coalesce(p_ttl_minutes, 10), 1));

    insert into public.pairing_session (
        id,
        app_account_id,
        created_by_device_id,
        code_hash,
        expires_at,
        created_at
    ) values (
        v_session_id,
        v_account_id,
        v_device_id,
        crypt(v_code, gen_salt('bf')),
        v_expires_at,
        now()
    );

    return jsonb_build_object(
        'pairing_session_id', v_session_id,
        'code', v_code,
        'expires_at', v_expires_at
    );
end;
$$;

create or replace function public.redeem_pairing_code(
    p_code text,
    p_device_id text default null,
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
    v_current_device public.app_device%rowtype;
    v_pairing public.pairing_session%rowtype;
    v_used_id text;
begin
    if p_code is null or length(trim(p_code)) <> 6 then
        raise exception 'Invalid pairing code';
    end if;

    v_auth_user_id := auth.uid();
    if v_auth_user_id is null then
        raise exception 'Not authenticated';
    end if;

    select *
    into v_current_device
    from public.app_device d
    where d.auth_user_id = v_auth_user_id
      and d.revoked_at is null
    limit 1;

    if not found then
        raise exception 'Current device is not linked or is revoked';
    end if;

    select *
    into v_pairing
    from public.pairing_session ps
    where ps.used_at is null
      and ps.expires_at > now()
      and ps.code_hash = crypt(trim(p_code), ps.code_hash)
    order by ps.created_at desc
    limit 1
    for update;

    if not found then
        raise exception 'Pairing code invalid or expired';
    end if;

    update public.pairing_session
    set
        used_at = now(),
        used_by_device_id = v_current_device.id
    where id = v_pairing.id
      and used_at is null
    returning id into v_used_id;

    if v_used_id is null then
        raise exception 'Pairing code already used';
    end if;

    update public.app_device
    set
        app_account_id = v_pairing.app_account_id,
        device_name = coalesce(nullif(p_device_name, ''), device_name),
        platform = coalesce(nullif(p_platform, ''), platform),
        revoked_at = null,
        revoked_by_device_id = null,
        revoke_reason = null,
        last_seen_at = now()
    where auth_user_id = v_auth_user_id;

    return (
        select jsonb_build_object(
            'app_account_id', d.app_account_id,
            'app_device_id', d.id,
            'auth_user_id', d.auth_user_id::text,
            'pairing_session_id', v_pairing.id,
            'joined', true
        )
        from public.app_device d
        where d.auth_user_id = v_auth_user_id
        limit 1
    );
end;
$$;

create or replace function public.list_linked_devices()
returns table (
    id text,
    device_name text,
    platform text,
    created_at timestamptz,
    last_seen_at timestamptz,
    revoked_at timestamptz,
    is_current boolean
)
language sql
stable
security definer
set search_path = public
as $$
    select
        d.id,
        d.device_name,
        d.platform,
        d.created_at,
        d.last_seen_at,
        d.revoked_at,
        d.id = public.current_app_device_id() as is_current
    from public.app_device d
    where d.app_account_id = public.current_app_account_id()
    order by d.created_at asc
$$;

create or replace function public.revoke_linked_device(
    p_device_id text,
    p_reason text default null
)
returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
    v_current_device_id text;
    v_account_id text;
    v_updated integer;
begin
    v_current_device_id := public.current_app_device_id();
    v_account_id := public.current_app_account_id();

    if v_current_device_id is null or v_account_id is null then
        raise exception 'Current device is not linked or is revoked';
    end if;

    if p_device_id is null or p_device_id = v_current_device_id then
        raise exception 'Cannot revoke current device';
    end if;

    update public.app_device
    set
        revoked_at = now(),
        revoked_by_device_id = v_current_device_id,
        revoke_reason = coalesce(nullif(p_reason, ''), 'revoked_by_user')
    where id = p_device_id
      and app_account_id = v_account_id
      and revoked_at is null;

    get diagnostics v_updated = row_count;
    return v_updated > 0;
end;
$$;

grant execute on function public.create_pairing_session(integer) to authenticated;
grant execute on function public.redeem_pairing_code(text, text, text, text) to authenticated;
grant execute on function public.list_linked_devices() to authenticated;
grant execute on function public.revoke_linked_device(text, text) to authenticated;
