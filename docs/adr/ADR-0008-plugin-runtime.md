# ADR-0008 — Plugin-Based Components and extension trust

- **Status:** APPROVED — compiled first-party components and explicit contracts, without arbitrary hot-loaded plugins. Packaging/tooling details remain PROPOSED; third-party execution is deferred.
- **Approval basis:** [Bounded owner confirmation](../00-GOVERNANCE/DECISION-MANAGEMENT.md#owner-confirmation-of-foundation-orientations); this is not permission to execute third-party packages or restart unrelated services.
- **Approved context:** DDD/hexagonal modular monolith with Plugin-Based Components (PBC), not a dynamic plugin runtime.

- **Record reviewed:** 2026-11-17; this is not an approval date.
- **Accountable owner / approval authority:** Project owner.
- **Application delivery:** NOT IMPLEMENTED; test and release evidence absent.

## Context

PBC needs a clear distinction between trusted application components and
untrusted extensions. No code or business modules exist. The current foundation
must not become a marketplace runtime or gain arbitrary code installation just
because the architecture uses the word "plugin".

## Options considered

1. **Trusted compiled first-party components.** Build backend components into
   the Spring Boot application; register Angular routes at build time with
   static lazy-loaded imports. Review, rebuild and restart/redeploy for changes.
2. **Constrained declarative packages.** Validate manifests and allowlisted
   metadata against a schema and capability model. They may describe permitted
   contributions but must not implicitly carry arbitrary SQL, JavaScript, Java,
   expressions or remote code URLs.
3. **Dynamically loaded code in the application JVM.** Classloaders can organize
   dependencies, not securely contain malicious code, CPU/memory exhaustion or
   JVM access. Reject as an untrusted-code security boundary.
4. **Third-party execution out of process.** A separately supervised process or
   service with OS/container isolation and narrow authenticated contracts could
   support future executable extensions; it adds significant operational work.

## Proposed decision

Option 1 is approved as the foundation's execution orientation. First-party components are explicitly trusted, compiled and reviewed as part of the application. Concrete discovery, packaging and dependency/contract-check tooling under ADR-0002 remain proposals requiring validation. Installing/upgrading code requires a backend
build and restart/redeployment. Angular uses a statically known route registry
and lazy routes pointing to bundled first-party code; lazy loading is a loading
optimization, not runtime trust or installation. No remote JavaScript, runtime
module federation or arbitrary classloader loading is proposed for the foundation.

Option 2 may be evaluated later for strictly allowlisted data-only contributions.
"Declarative" is not a security property by itself: SQL migrations execute with
database privileges, UI templates can introduce XSS, and expressions/automation
can become executable programs. Packages require schema validation, explicit
capability review and operator approval; signatures identify a publisher but do
not prove code safe. Migrations must enter ADR-0007's reviewed deterministic
stream, never execute merely because a package declares them.

Defer third-party executable extensions. If later authorized, evaluate option 4
with independent credentials, bounded resources, network/filesystem restrictions,
message validation and a reviewed tenant-aware capability API. A separate process
alone is not a complete sandbox. Do not share the application DB credentials or
allow direct tenant-table access. Arbitrary third-party browser code must not run
in the trusted Angular origin; any future UI extension needs its own isolation
and message-origin design. No framework/runtime is selected for this later work.

## Consequences

First-party PBC releases remain normal application releases, with simpler
operations and no hot install/unload. Shared-JVM failures remain possible. Typed
in-process contracts may be used now; requiring every internal interface to be
serializable would prematurely constrain the foundation. Future out-of-process
extensions need explicit versioned transport contracts and capability mediation.
Declarative support is not promised to cover all future extension needs.

## Security considerations

Backend authorization, not Angular route visibility, determines access. Enabling
a component for a tenant does not install code or bypass RLS. Trusted first-party
code can access its process privileges and therefore still needs least privilege,
dependency review and boundary tests. Deny unknown manifest fields/capabilities
and unapproved executable content; never infer safe trust from a label or signature.

## Validation and tests

Planned, not executed: verify first-party dependency boundaries, static route
registration, unauthorized endpoint denial and tenant isolation. Test manifest
rejection for executable SQL/JS, remote imports, undeclared capabilities and
malformed data before any declarative installer is considered. A later execution
spike must prove process termination, resource limits, credential separation,
network/filesystem denial and cross-tenant denial under hostile input. No
classloader test can substitute for that security review.

## Migration implications

There are no installed plugins to migrate. Start only with the approved technical
foundation; add no business modules. If third-party execution is approved later,
design transport/version compatibility, data ownership, upgrade/disable/uninstall,
and rollback before enabling it. Disabling a component must not automatically
remove tenant data or undo shared schema migrations.

## Related

- [ADR-0002 — Modular monolith and PBC](ADR-0002-modular-monolith.md)
- [ADR-0003 — Tenant isolation](ADR-0003-multi-tenancy.md)
- [ADR-0007 — Migration ordering and trust](ADR-0007-flyway.md)
- [ADR-0009 — Deferred DSL and scripts](ADR-0009-script-engine.md)
- [ADR-0011 — API contracts](ADR-0011-api-versioning.md)
