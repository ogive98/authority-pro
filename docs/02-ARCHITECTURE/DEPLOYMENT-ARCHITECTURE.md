# Deployment Architecture

**Decision status:** APPROVED direction: a new dedicated Authority Pro database on the existing PostgreSQL server, with the other application untouched. Endpoint, compatibility and provisioning details remain unverified/PROPOSED.
**Implementation status:** NOT IMPLEMENTED. No database connection, creation, migration or restart has been performed for this decision.

## Modes and current scope

Lite (local), Standard (hosted) and Hybrid are blueprint targets. LITE/STANDARD/POWER/ADAPTIVE are separate resource profiles. The foundation first validates a local development topology; no hosted environment or production release is created automatically.

## Proposed local topology

Angular development server on loopback proxies API requests to Spring Boot on loopback; their candidate ports 4200/8080 must be checked before startup. Do not stop unrelated services to free a port. The owner reports an existing PostgreSQL server serving another application and requires a NEW database for Authority Pro on that server. Never reuse that application's database, schemas, credentials or tables as the new foundation.

The existing server's host, port and version are not established. `authority_pro` is a proposed new database name, not a database whose existence or ownership has been verified. The old Compose mapping `127.0.0.1:5433` and `postgres:16-alpine` are draft values, not evidence about this server. Leave Compose unused; do not start an additional instance or reconfigure the running service to match those defaults.

## Safe provisioning boundary

1. Obtain the existing server's host/port and an authorized operator connection method without requesting secrets in chat or putting passwords in command arguments.
2. Perform only a separately scoped read-only metadata preflight: server identity/version, candidate database/role collisions and effective privileges. Do not read the other application's tables or export its configuration/data.
3. Confirm database and dedicated project-role names. Abort if a candidate already exists; never silently adopt, drop/recreate or overwrite it. PostgreSQL roles are cluster-wide, so creation must be limited to approved NEW project roles; no existing role alteration, global grants/defaults or service configuration edits.
4. Provision only the new database and agreed roles with narrowly scoped privileges. Use an appropriate maintenance connection, never apply migrations to the old application database. Creating a database cannot be treated as an ordinary rollbackable migration; report partial creation and stop on failure rather than deleting objects automatically.
5. Before migrations, positively verify the connected database, project owner/runtime identities and expected project state. Migrations, tests, cleanup and restoration must target only dedicated project destinations; collision or target mismatch stops execution.

No `DROP DATABASE`, recreation, cluster initialization, existing data-directory/volume reuse, server upgrade, service/container restart, session termination or shared configuration change is authorized. If compatibility or isolation requires such a change, stop and present alternatives; do not fix it by changing the existing application.

A separate database shares server resources and cluster administration. It is not proof of resource or privilege isolation. Validate effective project-role access, including inherited and PUBLIC privileges; if restricting cross-application access would require altering existing database/global permissions, stop for approval instead of applying revokes to the other application. Bound new connection pools/jobs to protect the existing workload.

## Credentials and role separation

Use operator-controlled authority only for the approved provisioning step. Dedicated migrator/table-owner and restricted runtime identities serve only Authority Pro; never reuse the other application's application credentials. Runtime must not be a superuser or use provisioning credentials.

Deliver secrets through a protected prompt or approved secret mechanism, not source, chat, command arguments, process output or logs. A blank example password is not a usable account. Do not copy the other application's secret files or edit authentication/service settings to obtain access.

## Delivery and operations

After version validation: Maven Wrapper builds the backend; a locked npm dependency tree builds Angular. Exact application startup commands belong in the README only after runnable files exist and have been verified. The PostgreSQL client version does not establish the server version or compatibility.

TLS is required outside explicitly local development. Production frontend/API should be same-origin, probes restricted and database exposure controlled. Multiple instances require additional verified resource/session/execution behavior, not an assumption based on a shared database.

## Recovery

The first database-bearing slice requires backup and restoration of Authority Pro data into a NEW isolated destination; never restore over the other application's database. Do not back up or restore the whole shared cluster as a shortcut. RPO/RTO remain proposed workload-dependent targets. Whole-project recovery does not prove selective tenant restoration or reversible application migrations.

## Acceptance

Verify approved target identity, non-colliding names, effective roles, bounded resource use, clean project startup, missing-secret/invalid-config refusal, migration target checks and an isolated restore drill. Record no secrets. No runtime gate is claimed passed; the existing application remains outside the operation's mutation boundary.

## Related

- [Approved orientation and database constraint](../00-GOVERNANCE/DECISION-MANAGEMENT.md#owner-confirmation-of-foundation-orientations)
- [Database architecture](DATABASE-ARCHITECTURE.md)
- [Non-functional requirements](../01-PRODUCT/NON-FUNCTIONAL-REQUIREMENTS.md)
- [Validation gates](../13-IMPLEMENTATION/VALIDATION-GATES.md)
