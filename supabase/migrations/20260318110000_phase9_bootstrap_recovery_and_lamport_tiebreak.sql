create or replace function public.sync_incoming_wins(
    current_lamport bigint,
    current_last_modified_by_device_id text,
    incoming_lamport bigint,
    incoming_last_modified_by_device_id text
)
returns boolean
language sql
immutable
as $$
    select
        incoming_lamport > coalesce(current_lamport, -1)
        or (
            incoming_lamport = coalesce(current_lamport, -1)
            and coalesce(incoming_last_modified_by_device_id, '') > coalesce(current_last_modified_by_device_id, '')
        )
$$;

grant execute on function public.sync_incoming_wins(bigint, text, bigint, text) to authenticated, service_role;

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
            'auth_user_id', v_active.auth_user_id::text,
            'created', false
        );
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

        if v_entity_type = 'deck' then
            if v_operation_type not in ('create', 'update', 'upsert', 'delete') then
                v_rejected := v_rejected || jsonb_build_object('op_id', v_op_id, 'reason', 'unsupported_operation_type');
                continue;
            end if;
            if v_operation_type <> 'delete' and nullif(v_payload->>'name', '') is null then
                v_rejected := v_rejected || jsonb_build_object('op_id', v_op_id, 'reason', 'missing_deck_name');
                continue;
            end if;
        elsif v_entity_type = 'flashcard' then
            if v_operation_type not in ('create', 'update', 'upsert', 'delete') then
                v_rejected := v_rejected || jsonb_build_object('op_id', v_op_id, 'reason', 'unsupported_operation_type');
                continue;
            end if;
            if nullif(v_payload->>'deckId', '') is null then
                v_rejected := v_rejected || jsonb_build_object('op_id', v_op_id, 'reason', 'missing_flashcard_deck_id');
                continue;
            end if;
            if v_operation_type <> 'delete' and (
                nullif(v_payload->>'word', '') is null or
                nullif(v_payload->>'meaning', '') is null
            ) then
                v_rejected := v_rejected || jsonb_build_object('op_id', v_op_id, 'reason', 'missing_flashcard_word_or_meaning');
                continue;
            end if;
        elsif v_entity_type = 'flashcard_example' then
            if v_operation_type not in ('create', 'update', 'upsert', 'delete') then
                v_rejected := v_rejected || jsonb_build_object('op_id', v_op_id, 'reason', 'unsupported_operation_type');
                continue;
            end if;
            if nullif(v_payload->>'flashcardId', '') is null then
                v_rejected := v_rejected || jsonb_build_object('op_id', v_op_id, 'reason', 'missing_example_flashcard_id');
                continue;
            end if;
            if v_operation_type <> 'delete' and (
                nullif(v_payload->>'text', '') is null or
                nullif(v_payload->>'translation', '') is null or
                nullif(v_payload->>'type', '') is null
            ) then
                v_rejected := v_rejected || jsonb_build_object('op_id', v_op_id, 'reason', 'missing_example_fields');
                continue;
            end if;
        elsif v_entity_type = 'quote' then
            if v_operation_type not in ('create', 'update', 'upsert', 'delete') then
                v_rejected := v_rejected || jsonb_build_object('op_id', v_op_id, 'reason', 'unsupported_operation_type');
                continue;
            end if;
            if v_operation_type <> 'delete' and (
                nullif(v_payload->>'phrase', '') is null or
                nullif(v_payload->>'translation', '') is null or
                nullif(v_payload->>'category', '') is null
            ) then
                v_rejected := v_rejected || jsonb_build_object('op_id', v_op_id, 'reason', 'missing_quote_fields');
                continue;
            end if;
        elsif v_entity_type = 'review_event' then
            if v_operation_type <> 'appendevent' then
                v_rejected := v_rejected || jsonb_build_object('op_id', v_op_id, 'reason', 'unsupported_operation_type');
                continue;
            end if;
            if nullif(v_payload->>'flashcardId', '') is null then
                v_rejected := v_rejected || jsonb_build_object('op_id', v_op_id, 'reason', 'missing_review_event_flashcard_id');
                continue;
            end if;
            if nullif(v_payload->>'reviewedAt', '') is null or nullif(v_payload->>'nextReviewAt', '') is null then
                v_rejected := v_rejected || jsonb_build_object('op_id', v_op_id, 'reason', 'missing_review_event_timestamps');
                continue;
            end if;
        else
            v_rejected := v_rejected || jsonb_build_object('op_id', v_op_id, 'reason', 'unsupported_entity_type');
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

        if v_entity_type = 'deck' then
            if v_operation_type = 'delete' then
                insert into public.deck (
                    app_account_id, id, name, description,
                    created_at, updated_at, deleted_at,
                    origin_device_id, last_modified_by_device_id, version_lamport
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
                where public.sync_incoming_wins(
                    public.deck.version_lamport,
                    public.deck.last_modified_by_device_id,
                    excluded.version_lamport,
                    excluded.last_modified_by_device_id
                );
            else
                insert into public.deck (
                    app_account_id, id, name, description,
                    created_at, updated_at, deleted_at,
                    origin_device_id, last_modified_by_device_id, version_lamport
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
                where public.sync_incoming_wins(
                    public.deck.version_lamport,
                    public.deck.last_modified_by_device_id,
                    excluded.version_lamport,
                    excluded.last_modified_by_device_id
                );
            end if;
        elsif v_entity_type = 'flashcard' then
            insert into public.flashcard (
                app_account_id, id, deck_id,
                word, meaning, translation, phonetic, part_of_speech, type, note,
                register, level_band, domain, lemma, why_useful, usage_pattern,
                irregular_forms_json, collocations_json, common_mistake, confusable_with_json,
                cloze_sentence, source_context, warnings_json, study_cards_json, quality_checks_json,
                created_at, updated_at, deleted_at,
                origin_device_id, last_modified_by_device_id, version_lamport
            ) values (
                p_account_id,
                v_entity_id,
                v_payload->>'deckId',
                coalesce(nullif(v_payload->>'word', ''), '[deleted]'),
                coalesce(nullif(v_payload->>'meaning', ''), '[deleted]'),
                nullif(v_payload->>'translation', ''),
                nullif(v_payload->>'phonetic', ''),
                nullif(v_payload->>'partOfSpeech', ''),
                nullif(v_payload->>'type', ''),
                nullif(v_payload->>'note', ''),
                nullif(v_payload->>'register', ''),
                nullif(v_payload->>'levelBand', ''),
                nullif(v_payload->>'domain', ''),
                nullif(v_payload->>'lemma', ''),
                nullif(v_payload->>'whyUseful', ''),
                nullif(v_payload->>'usagePattern', ''),
                nullif(v_payload->>'irregularFormsJson', ''),
                nullif(v_payload->>'collocationsJson', ''),
                nullif(v_payload->>'commonMistake', ''),
                nullif(v_payload->>'confusableWithJson', ''),
                nullif(v_payload->>'clozeSentence', ''),
                nullif(v_payload->>'sourceContext', ''),
                nullif(v_payload->>'warningsJson', ''),
                nullif(v_payload->>'studyCardsJson', ''),
                nullif(v_payload->>'qualityChecksJson', ''),
                now(),
                now(),
                null,
                p_device_id,
                p_device_id,
                v_lamport
            )
            on conflict (app_account_id, id) do update
            set
                deck_id = excluded.deck_id,
                word = excluded.word,
                meaning = excluded.meaning,
                translation = excluded.translation,
                phonetic = excluded.phonetic,
                part_of_speech = excluded.part_of_speech,
                type = excluded.type,
                note = excluded.note,
                register = excluded.register,
                level_band = excluded.level_band,
                domain = excluded.domain,
                lemma = excluded.lemma,
                why_useful = excluded.why_useful,
                usage_pattern = excluded.usage_pattern,
                irregular_forms_json = excluded.irregular_forms_json,
                collocations_json = excluded.collocations_json,
                common_mistake = excluded.common_mistake,
                confusable_with_json = excluded.confusable_with_json,
                cloze_sentence = excluded.cloze_sentence,
                source_context = excluded.source_context,
                warnings_json = excluded.warnings_json,
                study_cards_json = excluded.study_cards_json,
                quality_checks_json = excluded.quality_checks_json,
                updated_at = excluded.updated_at,
                deleted_at = excluded.deleted_at,
                last_modified_by_device_id = excluded.last_modified_by_device_id,
                version_lamport = excluded.version_lamport
            where public.sync_incoming_wins(
                public.flashcard.version_lamport,
                public.flashcard.last_modified_by_device_id,
                excluded.version_lamport,
                excluded.last_modified_by_device_id
            );
        elsif v_entity_type = 'flashcard_example' then
            insert into public.flashcard_example (
                app_account_id, id, flashcard_id,
                text, translation, type,
                created_at, updated_at, deleted_at,
                origin_device_id, last_modified_by_device_id, version_lamport
            ) values (
                p_account_id,
                v_entity_id,
                v_payload->>'flashcardId',
                coalesce(nullif(v_payload->>'text', ''), '[deleted]'),
                coalesce(nullif(v_payload->>'translation', ''), '[deleted]'),
                coalesce(nullif(v_payload->>'type', ''), 'example'),
                now(),
                now(),
                null,
                p_device_id,
                p_device_id,
                v_lamport
            )
            on conflict (app_account_id, id) do update
            set
                flashcard_id = excluded.flashcard_id,
                text = excluded.text,
                translation = excluded.translation,
                type = excluded.type,
                updated_at = excluded.updated_at,
                deleted_at = excluded.deleted_at,
                last_modified_by_device_id = excluded.last_modified_by_device_id,
                version_lamport = excluded.version_lamport
            where public.sync_incoming_wins(
                public.flashcard_example.version_lamport,
                public.flashcard_example.last_modified_by_device_id,
                excluded.version_lamport,
                excluded.last_modified_by_device_id
            );
        elsif v_entity_type = 'quote' then
            insert into public.quote (
                app_account_id, id,
                title, phrase, description, translation,
                example, context, pronunciation, formality, tags, category,
                created_at, updated_at, deleted_at,
                origin_device_id, last_modified_by_device_id, version_lamport
            ) values (
                p_account_id,
                v_entity_id,
                coalesce(nullif(v_payload->>'title', ''), v_payload->>'phrase', '[deleted]'),
                coalesce(nullif(v_payload->>'phrase', ''), '[deleted]'),
                coalesce(v_payload->>'description', ''),
                coalesce(nullif(v_payload->>'translation', ''), '[deleted]'),
                coalesce(v_payload->>'example', ''),
                coalesce(v_payload->>'context', ''),
                coalesce(v_payload->>'pronunciation', ''),
                coalesce(v_payload->>'formality', ''),
                coalesce(v_payload->>'tags', ''),
                coalesce(nullif(v_payload->>'category', ''), '[deleted]'),
                now(),
                now(),
                null,
                p_device_id,
                p_device_id,
                v_lamport
            )
            on conflict (app_account_id, id) do update
            set
                title = excluded.title,
                phrase = excluded.phrase,
                description = excluded.description,
                translation = excluded.translation,
                example = excluded.example,
                context = excluded.context,
                pronunciation = excluded.pronunciation,
                formality = excluded.formality,
                tags = excluded.tags,
                category = excluded.category,
                updated_at = excluded.updated_at,
                deleted_at = excluded.deleted_at,
                last_modified_by_device_id = excluded.last_modified_by_device_id,
                version_lamport = excluded.version_lamport
            where public.sync_incoming_wins(
                public.quote.version_lamport,
                public.quote.last_modified_by_device_id,
                excluded.version_lamport,
                excluded.last_modified_by_device_id
            );
        elsif v_entity_type = 'review_event' then
            insert into public.review_event (
                app_account_id, id, flashcard_id, grade,
                reviewed_at, next_review_at,
                ease_factor, interval_days, repetitions, lapses,
                created_at, origin_device_id, last_modified_by_device_id, version_lamport
            ) values (
                p_account_id,
                v_entity_id,
                v_payload->>'flashcardId',
                coalesce(nullif(v_payload->>'grade', ''), 'review'),
                to_timestamp((v_payload->>'reviewedAt')::bigint / 1000.0),
                to_timestamp((v_payload->>'nextReviewAt')::bigint / 1000.0),
                (v_payload->>'easeFactor')::double precision,
                (v_payload->>'interval')::integer,
                (v_payload->>'repetitions')::integer,
                (v_payload->>'lapses')::integer,
                now(),
                p_device_id,
                p_device_id,
                v_lamport
            )
            on conflict (app_account_id, id) do nothing;
        end if;

        get diagnostics v_affected_rows = row_count;
        v_applied := v_affected_rows > 0;

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
