# ADR-0010 — Proposed Java baseline and unverified version matrix

- **Status:** PROPOSED — every exact version is UNVERIFIED; no dependency set is pinned or owner-approved.
- **Evidence state:** No application code, resolved dependency tree, compatibility build or external documentation verification is available from this revision.

- **Record reviewed:** 2026-11-17; this is not an approval date.
- **Accountable owner / approval authority:** Project owner.
- **Application delivery:** NOT IMPLEMENTED; test and release evidence absent.
- **Scope note:** The pinned documentation-validator dependencies are not the application version matrix.

## Context

Angular and Spring Boot are approved technologies, not approved numeric releases.
The foundation needs a reproducible, supported combination of JDK, backend
libraries, frontend compiler/toolchain and PostgreSQL. A package existing in a
registry proves publication at most, not stability, support or interoperability.
No external documentation was fetched for this ADR correction.

## Options considered

1. Propose Java 21 as the initial baseline and select a supported Spring Boot
   line plus compatible libraries/toolchain after verification — recommended.
2. Select a newer JDK baseline if verified library requirements and operational
   support justify it — needs a new comparison, not an automatic upgrade.
3. Copy previously asserted numeric versions or select "latest" packages —
   rejected as a reproducibility and compatibility policy.

## Proposed decision and approval gate

Java 21 is a proposed baseline, not a verified complete stack. Select the exact
JDK distribution/patch and all other versions only after the compatibility spike
below. Approval of ADR-0001 does not constitute approval of this version matrix.

| Component | Proposal / unresolved choice | Verification state |
| --- | --- | --- |
| Java | Java 21 baseline candidate; distribution and patch TBD | UNVERIFIED |
| Spring Boot | Supported line and patch compatible with the chosen JDK | UNVERIFIED; not pinned |
| Spring Modulith | Compatible release/BOM and required modules | UNVERIFIED; adoption also proposed |
| Spring Security / Spring Session JDBC | Versions aligned with the verified Boot dependency set | UNVERIFIED |
| springdoc | Release compatible with the selected Boot, servlet and JSON stack | UNVERIFIED; adoption proposed |
| Hibernate / JDBC driver | Inspect selected Boot dependency management and PostgreSQL compatibility | UNVERIFIED; JPA adoption proposed |
| Flyway | Core, PostgreSQL adapter and edition suitable for the selected database/Boot line | UNVERIFIED; adoption proposed |
| PostgreSQL | Explicit supported server major/patch and deployment policy TBD | UNVERIFIED |
| Angular / Angular CLI | Explicit compatible release and patch, not "latest at build time" | UNVERIFIED |
| TypeScript / RxJS | Versions within the selected Angular release's supported ranges | UNVERIFIED |
| Node.js / package manager | Versions supported by Angular CLI and the build environment | UNVERIFIED |
| Maven / build wrapper | Reproducible toolchain compatible with the selected JDK/Boot plugins | UNVERIFIED; tooling proposed |

Earlier claims that Java 21, Boot 4.1, Modulith 2.1, springdoc 3, Hibernate 7,
Flyway 11 and an unspecified latest Angular were confirmed compatible are
withdrawn. Those labels are not retained as pins, and their release/support
status is not asserted here. The available machine's Node version is not proof
of Angular compatibility or a deployment baseline. Do not assume a particular
Jackson major or that Boot manages a specific dependency without inspecting the
selected release's actual dependency management.

## Verification and tests before pinning

All checks below are planned, not executed:

1. Consult authoritative compatibility/support information for the candidate
   JDK, Boot, Modulith, springdoc and PostgreSQL integrations; record exact
   versions and dated evidence when that separate verification work occurs.
2. Verify Angular/CLI's required TypeScript, RxJS and Node ranges, package-manager
   support and intended browser targets. Select a supported intersection rather
   than installing each package's newest release independently.
3. Resolve a minimal backend build and inspect the dependency tree/effective BOM:
   Security, Session JDBC, Hibernate, JDBC driver, Flyway/PostgreSQL adapter,
   servlet and JSON dependencies must not conflict. Verify Modulith/ArchUnit
   checks and springdoc/OpenAPI behavior if those tools are selected.
4. Against real PostgreSQL, exercise migration/validation, JDBC session lifecycle,
   CSRF login/logout, RLS with runtime roles and transaction-local context on the
   actual connection. Run ADR-0006's durability spike separately to select its
   mechanism, not to enable both candidates.
5. Build/test a minimal Angular client on the candidate Node/TypeScript versions;
   test same-origin cookies/CSRF, API error parsing and configured health probes.
   Include no business modules in these checks.
6. Review vulnerability and support-lifecycle information; then record the exact
   tested matrix, evidence and approval before pinning wrappers, lockfiles and
   dependency management in future foundation work. This revision edits no
   configuration or code.

Registry metadata alone cannot complete any compatibility gate. If a candidate
fails, choose another supported combination and repeat the relevant checks.

## Consequences

Scaffolding cannot honestly advertise a verified stack yet. BOM alignment and
frontend lockfiles should reduce accidental drift once approved, but require
reviewed overrides and repeatable builds. Avoid open-ended version ranges or
floating "latest" selectors. Verification effort is deliberate foundation work,
not evidence of the owner's expertise or prior approval.

## Security considerations

Check dependency provenance, known vulnerabilities, update policy and supported
release lifetimes before pinning. Do not resolve compatibility by disabling
security features or forcing unsupported transitive versions. Protect build and
registry credentials; no secrets belong in manifests or sample configuration.

## Migration implications

There is no existing application to upgrade. Establish and test the first pinned
matrix before code generation. Later upgrades require compatibility tests,
release-note review and schema/session serialization checks, with an application
rollback plan that respects migration compatibility. Do not silently relabel an
unverified combination as approved.

## Related

- [ADR-0001 — Approved technologies](ADR-0001-stack.md)
- [ADR-0002 — Proposed modularity tooling](ADR-0002-modular-monolith.md)
- [ADR-0005 — Security and JDBC sessions](ADR-0005-sessions.md)
- [ADR-0006 — Durability spike](ADR-0006-outbox.md)
- [ADR-0007 — Proposed Flyway integration](ADR-0007-flyway.md)
- [ADR-0011 — API and probes](ADR-0011-api-versioning.md)
