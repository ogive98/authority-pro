# Product Objectives

**Decision status:** PROPOSED detailed objectives within the approved scope of
normative documentation plus a runnable, business-neutral foundation.
**Delivery status:** PLANNED / NOT IMPLEMENTED. Phase 0 is in progress; no
backend/frontend implementation or passing acceptance evidence exists.

## 1. Socle objectives (planned Phase 1; none implemented)

All measures below are future acceptance checks, not reported results.

| ID | Objective | Proposed measure of success |
|---|---|---|
| O-S1 | Reproducible local startup | Document and test database/backend/frontend startup commands from a clean environment; no single-command launcher is claimed |
| O-S2 | Basic foundation identity, not a complete identity product | Closed-default operator CLI bootstrap, Spring Security + Spring Session JDBC cookie login/logout, CSRF, revocation and verified memberships; design remains PROPOSED |
| O-S3 | Structural tenant isolation | Test shared PostgreSQL `tenant_id` + RLS with a non-owner, non-superuser, NOBYPASSRLS runtime, separate migration role and transaction-local verified tenant/user context; no global bypass |
| O-S4 | Server-side authorization | Every endpoint has an explicit rule and denial tests; tenant = organization with company/site hierarchy is the proposed model, exact roles/grants remain open |
| O-S5 | Durable security audit | Sensitive writes atomically commit with audit or are rejected; anonymous failures use bounded separate transactions without secrets; failure behavior tested |
| O-S6 | Verifiable module boundaries | Proposed Modulith/ArchUnit checks pass in a real CI run, with linked evidence |
| O-S7 | Usable UI shell | Login, tenant display, navigation, authorized audit view and FR/EN/IT/AR language support with RTL; no staged language reduction is approved |
| O-S8 | Documentation agrees with delivery | Each status links to approval/code/test evidence as applicable; no generated implementation claims |

## 2. Platform objectives (later-phase vision, not authorized)

Phase assignments are PROPOSED; these objectives are not delivered and do not
expand the authorized foundation scope. Business modules require separate approval.

- O-P1: Install a module package without modifying the core (Phase 5/6).
- O-P2: Resolve dependencies and integrations automatically with impact
  analysis (Phase 5).
- O-P3: Operate with logs, metrics, health and diagnostics (Phase 3).
- O-P4: Back up and restore with tested drills (Phase 3).
- O-P5: Expose a governed automation builder (Phase 5/8).
- O-P6: Offer a marketplace and licensing model (Phase 7).
- O-P7: Support local, hosted and hybrid deployment (Phase 8).

## 3. Anti-objectives

- Shipping modules before the socle is verified.
- Optimizing theoretical scale before measured need.
- Adding parallelism or distribution without an operational reason.
- Documenting capabilities as implemented when they are not.

## 4. Prioritization rule

Security and tenant isolation outrank features. A feature that weakens a
foundation is rejected regardless of business value.

## Related documents

- [Product Vision](PRODUCT-VISION.md)
- [MVP Requirements](REQUIREMENTS-MVP.md)
- [Validation Gates](../13-IMPLEMENTATION/VALIDATION-GATES.md)
