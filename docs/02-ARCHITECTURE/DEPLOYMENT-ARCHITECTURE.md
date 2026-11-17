# Deployment Architecture

**Decision status:** PROPOSED development topology.
**Implementation status:** NOT IMPLEMENTED. Compose configuration alone is not a working deployment.

## Modes and current scope

Lite (local), Standard (hosted) and Hybrid are blueprint targets. LITE/STANDARD/POWER/ADAPTIVE are separate resource profiles. The foundation first validates a local development topology; no hosted environment, production release or remote repository is created automatically.

## Proposed local topology

Angular development server on loopback proxies API requests to Spring Boot on loopback; PostgreSQL is a separate isolated development instance. Candidate ports 4200/8080/5433 must be checked before startup; do not stop unrelated services or reuse another project's data.

Use a new named data volume scoped by the Compose project. Publish the database only on `127.0.0.1`; never bind it to all network interfaces by default. Docker availability does not prove an image, service or database is initialized.

## Credentials and role separation

The Compose database bootstrap credential provisions the database; it is not the application runtime credential. Supply it from an ignored environment file or secret mount with no committed usable default. A separately approved provisioning procedure creates the migrator and restricted runtime roles described in database architecture. No backend may start with the bootstrap/superuser role.

Do not expose secrets through `docker compose config`, command arguments, logs or reports. Use quiet configuration validation. A local environment example contains variable names and blank secret placeholders only.

## Delivery and operations

After version validation: Maven Wrapper builds the backend; a locked npm dependency tree builds Angular; local checks run before any remote CI execution. Exact application startup commands belong in the README only after runnable files exist and their commands have succeeded.

TLS is required outside explicitly local development. Production serves frontend/API same-origin, protects administrative probes and prevents direct database exposure. Framework session state can live in PostgreSQL; multiple instances additionally require tested scheduling, cache invalidation and publication locking before horizontal scaling is claimed.

## Recovery

The first database-bearing slice requires a tested backup/restore procedure in an isolated destination. RPO/RTO are proposed workload-dependent targets, not achieved guarantees. Shared database restoration is not equivalent to tenant-level restoration. Application downgrade never assumes schema rollback is possible.

No existing service restarts, production credentials, CI secrets or remote state are modified by this documentation.

## Acceptance

Check ports and effective database roles; prove a clean startup, missing-secret refusal, invalid-config refusal, graceful stop, migration failure behavior and restore drill. Record commands, versions and outcomes without secrets. None of these runtime gates has passed yet.

## Related

- [Database architecture](DATABASE-ARCHITECTURE.md)
- [Non-functional requirements](../01-PRODUCT/NON-FUNCTIONAL-REQUIREMENTS.md)
- [Validation gates](../13-IMPLEMENTATION/VALIDATION-GATES.md)
