# ADR-0006 — Durable internal event publication

- **Status:** PROPOSED — durable delivery requirements and candidate mechanisms; no implementation selected or owner approval claimed.
- **Decision gate:** Choose one durability mechanism after a focused spike; do not implement two parallel publication stores.

- **Record reviewed:** 2026-11-17; this is not an approval date.
- **Accountable owner / approval authority:** Project owner.
- **Application delivery:** NOT IMPLEMENTED; test and release evidence absent.

## Context

Future component events must survive a crash between committing state and
notifying listeners. The approved deployment is a modular monolith backed by
PostgreSQL. No event code or business modules exist; only a technical spike is
in scope for evaluating this proposal.

## Options considered

1. **Spring Modulith durable event publication registry.**
   Could record publications transactionally and track listener completion with
   less custom infrastructure. Verify the selected version's persistence adapter,
   transaction semantics, resubmission, concurrent delivery, retention and tenant
   isolation. Ordering and inbox idempotency must not be assumed to be automatic.
2. **Explicit transactional outbox with a polling dispatcher.**
   Store each event with its state change in one PostgreSQL transaction. Gives
   direct control over RLS, claims, retry and ordering, but requires maintaining
   dispatcher, recovery, retention and observability code.
3. **In-memory application events only.**
   Simple but not durable across commit/crash windows; unsuitable when delivery
   is required. Synchronous calls can still be appropriate for immediate work.
4. **External broker or database notifications.**
   A broker adds infrastructure and does not alone make a database change and
   publish atomic. Notifications alone are not durable publication storage.

## Proposed decision and spike gate

Evaluate options 1 and 2 with the same failure scenarios and production-equivalent
RLS roles. Prefer a verified framework registry if it meets all requirements
without bypasses or an additional competing publication ledger; otherwise select
an explicit outbox. Record and approve exactly one result before implementation.
Do not persist every event to both a Modulith registry and a custom outbox. A
consumer inbox is a deduplication receipt, not a second producer durability store.

The spike must demonstrate these shared requirements:

- **Atomic production:** state change and durable publication record commit or
  roll back in the same local transaction. Publishing only after commit without
  a durable record is insufficient.
- **At-least-once delivery:** retries and crashes may cause duplicates; do not
  promise end-to-end exactly-once execution.
- **Atomic consumer effects:** assign stable event and consumer IDs. Insert a
  unique inbox receipt keyed by tenant, consumer and event, and apply that
  consumer's database side effects in the same transaction. A duplicate must
  skip effects only after the original transaction committed. Failure rolls
  back receipt and effects together; never mark the inbox complete first.
- **Completion:** acknowledge the publication only after consumer commit, or
  atomically with it if supported on the same transaction/connection. A crash
  before acknowledgement causes safe redelivery and inbox deduplication.
- **Concurrent claims and recovery:** evaluate short transactional row claims
  (for example `FOR UPDATE SKIP LOCKED`) and explicit processing leases for work
  performed after releasing locks. Persist claim owner/attempt and expiry; use
  fencing or conditional completion to prevent stale workers from acknowledging
  reclaimed work. Locks alone do not survive crashes, and lease expiry can
  produce concurrent attempts, so inbox uniqueness remains necessary.
- **Retries:** bounded exponential backoff with jitter, attempt tracking,
  observable failure/quarantine state and authorized replay. A poison message
  must not retry indefinitely without operational visibility.
- **Aggregate order:** define ordering per tenant and aggregate with a sequence
  allocated in the producer transaction. Do not dispatch a later sequence while
  an earlier event is pending, retrying or leased. `SKIP LOCKED` alone does not
  ensure this. A quarantined event blocks that aggregate unless an explicit,
  audited resolution permits progress; no global order is promised.

External HTTP calls, email or other non-transactional effects cannot share the
inbox/database atomicity guarantee. Defer such integrations until downstream
idempotency keys or a separately reviewed durable egress design closes that gap;
do not label a database receipt as proof of exactly-once external effects.

## Consequences

Delivery becomes eventually consistent, with operational work for lag, retries,
quarantine and retention. Event payloads need independent schema versions and
consumer compatibility rules; HTTP URI versions do not version events. Receipt
retention must cover the replay/deduplication horizon. Selection remains unresolved
until the spike meets these requirements, including recovery under concurrency.

## Security considerations

Publication records, inbox rows and payloads are tenant-scoped and subject to
ADR-0003. Dispatch must use explicitly authorized per-tenant work scopes and
initialize context on each real transaction; do not introduce a global polling
role, `app.platform_scope`, or owner access to scan all tenant rows. Prove a
bounded per-tenant scheduling approach during the spike; a candidate requiring
unrestricted registry reads does not pass. Validate payload type/version, avoid
unsafe polymorphic deserialization and exclude secrets from events and logs.

## Validation and tests

Planned, not executed: crash before/after producer commit, during claim, before
consumer commit and after effects commit but before acknowledgement. Test
simultaneous duplicate delivery, rollback of effects and inbox together, expired
leases, stale completion, retry exhaustion, poison-event ordering and replay.
Use at least two workers and test per-aggregate order and cross-tenant denial
with the actual runtime roles. Verify retention does not permit duplicate effects
within the supported replay window. No business event module is required.

## Migration implications

No publication tables exist. After selection and approval, add only the chosen
producer persistence and needed inbox schema through ADR-0007. Changing mechanisms
later requires draining or mapping pending publications while retaining event IDs
and deduplication receipts; do not run both as independent publishers.

## Related

- [ADR-0002 — Component contracts](ADR-0002-modular-monolith.md)
- [ADR-0003 — Tenant-scoped transaction context](ADR-0003-multi-tenancy.md)
- [ADR-0007 — Schema migrations](ADR-0007-flyway.md)
- [ADR-0010 — Modulith compatibility verification](ADR-0010-java-spring-versions.md)
