create index if not exists idx_deck_account_deleted_created
    on public.deck (app_account_id, deleted_at, created_at desc);

create index if not exists idx_flashcard_account_deck_deleted_created
    on public.flashcard (app_account_id, deck_id, deleted_at, created_at desc);

create index if not exists idx_flashcard_account_deleted_created
    on public.flashcard (app_account_id, deleted_at, created_at desc);

create index if not exists idx_flashcard_example_account_flashcard_deleted_created
    on public.flashcard_example (app_account_id, flashcard_id, deleted_at, created_at desc);

create index if not exists idx_review_event_account_flashcard_reviewed_desc
    on public.review_event (app_account_id, flashcard_id, reviewed_at desc);

create index if not exists idx_sync_operation_account_lamport_created
    on public.sync_operation (app_account_id, lamport, created_at);
