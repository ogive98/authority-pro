# ADR-0012 — Atomic enforcement of the concurrent session cap

- **Status:** APPROVED — implemented 2026-11-28 at `56e5662`; the transaction-scoped
  advisory lock now covers the session insert itself.
- **Implementation note (2026-11-28):** `CappedSessionRepository` wraps the JDBC session
  repository used by the session filter. For a new session it opens one transaction,
  takes `pg_try_advisory_xact_lock(hashtextextended(principal, 0))` with a bounded
  2-second wait, evicts deterministically (last access, then creation, then identifier)
  so the newest three survive, and inserts the row inside the same transaction.
  `JdbcSessionLimiter` remains only as a best-effort pre-eviction before
  authentication. No schema change. Proof: L05 isolated qualification `checks=40`,
  four parallel fresh sessions leave exactly the three newest, reproduced on three
  consecutive runs. Lock-timeout and rotation-specific paths remain untested.
  The original PROPOSED text below is preserved as the decision record.
- **Scope:** Browser session creation for a single principal. Distributed throttling
  of failed logins is a separate decision.
- **Risk addressed:** R-005 (session cap not atomic).

## Context

`JdbcSessionLimiter` enforces at most three concurrent sessions per principal. It
counts the principal's sessions, deletes the oldest, then lets Spring Security create
the new one. The count and the delete are not serialized against another login of
the same principal, and session rotation (`changeSessionId`) produces a new row after
the limiter runs. Two concurrent logins can therefore both observe "within the cap"
and leave four or more sessions, or could delete the session that just authenticated.

This is a bounded defect, not an exploitable authorization bypass, but it invalidates
the stated cap and weakens revocation guarantees.

## Options considered

1. **Keep best-effort behaviour and document it.** No schema or code change. The
   cap remains advisory and the claim must be removed from the published contract.
2. **Serialize session creation per principal inside one database transaction.**
   Acquire a transaction-scoped advisory lock keyed by the principal, count sessions,
   delete the oldest when needed, and commit before or with the new session insert.
   Requires the limiter and session creation to share a transaction boundary, or an
   explicit insert of the new session row under the same lock.
3. **External session store with native per-principal limits.** Adds infrastructure
   and a second source of truth; rejected for the foundation.
4. **Reject the login instead of evicting.** Simpler to reason about, but worse for
   users and inconsistent with the current product decision to evict the oldest.

## Proposed decision

Adopt option 2.

- Use a transaction-scoped advisory lock derived from the principal identifier, so
  concurrent logins for the same principal execute the count/evict/create sequence
  serially and logins for different principals do not contend.
- The lock must cover the new session row creation, not only the eviction, so a
  session created by rotation is counted exactly once.
- Derive the identifier from the persisted principal name, not from a client header.
- If the lock cannot be acquired within a bounded wait, fail the login closed with a
  generic authentication error; never create an uncapped session.
- Keep the cap constant configurable but default to three; document that the cap is
  per principal, not per device or per IP.

## Consequences

Session creation for one principal becomes serialized, adding one lock acquisition
to login latency. Lock contention is limited to concurrent logins of the same user.
The limiter must join the same transaction as the session repository operation;
this changes the current structure and requires an integration test against
PostgreSQL, since an in-memory repository cannot prove locking behaviour.

## Security considerations

The lock key must not leak the principal identifier into logs. Failure paths must not
distinguish "too many sessions" from other authentication failures. Eviction must be
deterministic (oldest by last-accessed time, tie-broken by creation time) so an
attacker cannot pin a victim's session by racing logins.

## Validation and tests

1. N parallel logins for one principal with cap three: exactly three live sessions
   remain, all unexpired, and the newest three are the survivors.
2. Parallel logins for two principals do not block each other beyond normal latency.
3. Rotation during login counts the rotated row once.
4. Lock timeout path returns a generic failure and leaves no partial session.
5. Session table inspected directly after each scenario.

## Migration implications

No schema change is required if an advisory lock is used. If a dedicated lock table
is chosen instead, it is a new migration and must follow ADR-0007.

## Related

- [ADR-0005 — Sessions](ADR-0005-sessions.md)
- [ADR-0013 — Production HTTP profile](ADR-0013-production-http-profile.md)
- [Security threat model](../15-FOUNDATION-9.5/phase-2/05-SECURITY-THREAT-MODEL.md)
