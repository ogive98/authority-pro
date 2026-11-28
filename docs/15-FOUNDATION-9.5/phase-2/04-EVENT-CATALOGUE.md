# Phase 2 — Event Catalogue

**Decision status:** PROPOSED. No event, outbox or worker is implemented.
**Delivery status:** DESIGN ONLY. Durable delivery remains deferred (ADR-0006, R-009).
**Document validation status:** DRAFT.

## 1. Why an event catalogue now

Events are recorded so that future consumers do not invent incompatible facts. The
catalogue does not authorize implementation. Today the only durable trace is the audit
tables; they are not a message bus and must not be read as one.

## 2. Envelope (proposed)

Every event carries: `eventId`, `eventType`, `eventVersion`, `occurredAt`,
`tenantId` (when applicable), `actorId` or explicit system actor, `correlationId`,
`causationId`, `operationId`, and a bounded JSON payload with no secrets and no
unnecessary personal data. Naming follows `context.entity.action` in the past tense.

## 3. Candidate events

| Event type | Owner | Trigger | Payload sketch | Consumer candidates |
|---|---|---|---|---|
| `identity.session.started` | Identity | Successful login | userId, source category, sessionIdHash | Audit, operations |
| `identity.session.terminated` | Identity | Logout, cap eviction, expiry | userId, reason, sessionIdHash | Audit, operations |
| `identity.authentication.failed` | Identity | Refused or throttled attempt | loginHash, source category, reason | Audit, security |
| `tenancy.membership.revoked` | Tenancy | Membership removal | tenantId, userId, membershipId | Access, Identity, Audit |
| `access.grant.changed` | Access | Role or permission assignment change | tenantId, subjectId, grant, revision | Audit, authorization cache |
| `audit.record.appended` | Audit | Any protected audit insert | recordId, actionCode, outcome | Operations only |
| `platform.inventory.observed` | Inventory | Catalogue rebuild | buildRevision, digest | Operations |

Session ids must be represented as salted hashes, never raw identifiers. Login names
must be normalized and hashed in event payloads.

## 4. Delivery semantics (proposed, not built)

1. The state change and the outbox row are written in one transaction.
2. A bounded worker claims batches with a safe concurrency strategy.
3. Consumers record a deduplication key in the same transaction as their effect.
4. Retries are limited to classified transient failures with bounded exponential
   backoff and jitter.
5. Terminal failures go to a dead-letter store with redacted reason and operator action.
6. Replay requires authorization, audit, idempotency and explicit selection.

No broker is selected. A PostgreSQL outbox is the candidate first implementation.

## 5. Ordering and compatibility

- Per aggregate, consumers must tolerate at-least-once delivery and possible reordering
  unless a version field is used.
- Adding an optional field is compatible; removing or repurposing a field is breaking.
- Event types are immutable facts; a new fact gets a new type.

## 6. Acceptance criteria before implementation

- A written use case exists with a named consumer; no speculative events.
- Crash-after-commit, crash-after-dispatch and duplicate delivery are tested.
- Poison-event and retry-exhaustion paths are tested.
- Payload redaction is reviewed against the threat model.
- Operational controls exist for inspection and replay.

## Related documents

- [Module ownership matrix](01-MODULE-OWNERSHIP-MATRIX.md)
- [Security threat model](05-SECURITY-THREAT-MODEL.md)
- [ADR-0006 durable publication](../../adr/ADR-0006-outbox.md)
