# Current Local-First Runtime

| Field | Value |
|---|---|
| Status | Active |
| Role | Current runtime contract |
| Source of Truth | Yes |
| Read this when | You touch startup, persistence or product assumptions |

## Principles

- the UI reads from `HelloDb`
- writes persist locally
- startup does not depend on remote services
- the product runs as single-device
- the study loop works without network
- AI card generation requires network and is an external service, not part of the local runtime

## Startup

`AppStartupCoordinator.start()` only does this:

1. ensures local install identity (`LocalIdentityInitializer.ensureReady()`)
2. seeds starter data if the install is empty (`SeedDataInitializer.ensureSeeded()`)
3. marks the app as ready if both succeed, carrying `hasSeenWelcome` read from `OnboardingStateRepository`
4. exposes local error if it fails

All three steps are local; none of them touches the network.

## Exists today

- local identity with `deviceId`
- local repositories over SQLDelight
- generation with Firebase AI
- local study with `ReviewEvent` and `ReviewProjection`

## Does not exist in active runtime

- remote bootstrap
- pairing
- remote push/pull/ack
- remote sync workers
- sync debug panel in product path

## Local write

Current pattern:

1. open local transaction
2. persist business entities
3. render from local queries

## Scope

- the current product is local-first single-device
- local-first applies to the study loop and persistence
- AI-assisted content entry is online by design and is not considered a break of the contract
- without network the user can study what they already have; they cannot create new cards until the connection is back
- legacy references to sync or Supabase are not part of the current runtime

## See also

- `ARCHITECTURE.md`
- `docs/CAPTURE_CURRENT.md`
- `docs/STUDY_CURRENT.md`
