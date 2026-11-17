# ADR-0009 — Deferred DSL and script execution

- **Status:** PROPOSED — recommend deferral; no language, engine or sandbox is approved or implemented.

- **Record reviewed:** 2026-11-17; this is not an approval date.
- **Accountable owner / approval authority:** Project owner.
- **Application delivery:** NOT IMPLEMENTED; test and release evidence absent.

## Context

User-authored conditions or automation may be useful later, but they are not a
requirement to implement in the current documentation and technical foundation.
No application code or business modules exist. A graph or declarative expression
can still execute expensive or unsafe work; a visual representation is not a
security boundary.

## Options considered

1. No user-authored execution in the initial foundation; fixed, reviewed
   application behavior only — recommended initial scope.
2. A small typed DSL with an allowlisted AST for bounded conditions/mappings —
   possible later candidate, not an approved language or runtime.
3. An automation graph invoking approved capabilities — also deferred; loops,
   fan-out and command calls require execution limits and authorization.
4. General-purpose JavaScript, Lua or another embedded interpreter — larger
   attack surface; host-access flags alone do not establish a safe sandbox.
5. An isolated worker/service or sandbox runtime — possible later execution
   boundary, with additional deployment and security responsibilities.

## Proposed decision

Defer DSLs, automation graphs and arbitrary scripts from the initial foundation.
Do not install an engine or create automation/business modules now. If there is
an approved later use case, first compare a limited DSL with fixed capabilities
against the need for general-purpose code; choose only after a threat model and
resource-limit spike.

Recommend isolated execution outside the trusted application process for untrusted
programs, with a least-privilege broker for allowed actions. A restricted DSL still
requires bounded parsing/evaluation and evidence that its restrictions hold; do
not assume "not Java" means safe. An independent security review is required
before any user-authored execution is enabled.

## Required properties for any future proposal

- Deny host access, reflection, class loading, native calls, direct JDBC, secrets
  and filesystem access. Deny network egress by default; any exception requires
  a narrowly scoped, reviewed capability rather than general sockets/URLs.
- Enforce CPU, memory, wall-clock, input/output size, recursion, expression size,
  iteration, fan-out and concurrency budgets. Bound compilation/parsing as well
  as execution. Use externally enforced termination where cooperative timeouts
  cannot stop a stuck program.
- Bind execution to an authenticated/authorized actor and explicit tenant using
  server-validated context. Brokered commands recheck permissions and use the
  real transaction context from ADR-0003; scripts cannot set their own tenant,
  assume an owner identity, or read across tenants.
- Version artifacts and capability contracts; require review before activation,
  bounded audit output, a kill switch and revocation. Signing is provenance,
  not proof of safety. Do not leak secrets through outputs or error messages.
- Define transactional side effects, cancellation and retry behavior. Use stable
  invocation IDs and deduplication where retries can repeat effects; external
  effects need separate idempotency guarantees under ADR-0006.

A Java classloader is not a security sandbox. A separate process also needs
OS/container restrictions, independent credentials and resource enforcement;
restricted interpreter settings or a runtime name alone prove none of these.
No GraalJS, Lua, WASM or other engine/version is certified by this ADR.

## Consequences

The foundation has less attack surface and no automation runtime to operate.
Later automation will require a capability model, quotas, observability and
execution infrastructure. Users cannot upload scripts merely because a PBC
package supports metadata. Deferral may delay extensibility but does not change
the approved component architecture.

## Security considerations

Treat all user-authored input as hostile, including DSL ASTs, templates and graph
nodes. Test denial-of-service and data-exfiltration paths, not just forbidden API
names. Never execute untrusted Java or JavaScript inside the main JVM or trusted
Angular origin as a shortcut. Sandbox failure must not expose runtime database
credentials or create a global tenant-data read path.

## Validation and tests

Planned only if execution is authorized later: test infinite work, deep recursion,
large allocations, parser bombs, oversized output, concurrency exhaustion,
network/filesystem/reflection attempts and process termination. Verify revoked
capabilities, forged context, cross-tenant denial, duplicate side effects,
kill-switch behavior and bounded non-secret audit. No execution tests have run,
and no sandbox has been proven.

## Migration implications

There are no scripts to migrate. A future implementation needs versioned DSL/AST
formats, capability compatibility, activation/revocation and rollback rules.
Upgrades must not silently broaden permissions or reinterpret existing programs.
Retire or revalidate affected artifacts when capability/security semantics change.

## Related

- [ADR-0002 — Foundation boundaries](ADR-0002-modular-monolith.md)
- [ADR-0003 — Tenant transaction context](ADR-0003-multi-tenancy.md)
- [ADR-0006 — Retry and side-effect semantics](ADR-0006-outbox.md)
- [ADR-0008 — Extension execution and trust](ADR-0008-plugin-runtime.md)
