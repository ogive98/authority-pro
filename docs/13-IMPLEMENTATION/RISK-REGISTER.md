# Risk Register

**Decision status:** PROPOSED mitigations; risks identified during foundation audit.
**Implementation status:** NOT IMPLEMENTED. A documented mitigation is not a closed runtime risk.

| ID | Severity | Risk | Required mitigation / closure evidence |
|---|---|---|---|
| R01 | Critical | Runtime DB role bypasses RLS | Separate non-owner NOSUPERUSER NOBYPASSRLS credential; assert flags and prove all-DML isolation |
| R02 | Critical | Global scope flag opens all tenants | No blanket policy bypass; bounded identity-plane own-membership lookup |
| R03 | Critical | Public bootstrap account takeover or race | Operator-only disabled-default provisioning; atomic guard and concurrency/audit tests |
| R04 | High | Custom browser token/session handling weakens auth | Framework sessions with HttpOnly cookies, CSRF and lifecycle tests; owner approval pending |
| R05 | Critical | Tenant context applied too late or leaked through pool | Same-connection transaction initialization before tenant SQL; rollback/reuse/async tests |
| R06 | High | Audit failure silently loses critical evidence | Protected state and audit same transaction, tested failure rollback; bounded separate attempt stream |
| R07 | High | Nested packages make module verification vacuous | Assert actual module names and reject intentional illegal dependency |
| R08 | High | Untrusted plugin treated as safe due to signature/classloader | Compiled reviewed first-party only; external sandbox design before executable third-party packages |
| R09 | High | Duplicate durable mechanisms cause double processing | Spike and select one publication mechanism; dedup and side effects same consumer transaction |
| R10 | High | Shared DB backup mistaken for tenant restore | Whole restore drill first; separate selective-restore consistency design |
| R11 | High | Company/site grants absent under tenant-only isolation | Define hierarchy and compound constraints now; test in-tenant privilege boundaries |
| R12 | Medium | Version metadata mistaken for compatibility | Official support matrix + clean build + database/browser tests before pinning |
| R13 | High | Premature APPROVED/IMPLEMENTED labels mislead later agents | Separate decision and delivery status; real evidence for each gate |
| R14 | Medium | Extensive vision delays or expands the socle | Preserve blueprint, tier documentation, exclude all business modules and speculative engines |
| R15 | Medium | Unrealistic machine/performance promises | Measure current environment and workload; targets unmeasured until benchmark evidence |
| R16 | High | Stale request/cache shows another tenant after switch | Cancel/ignore late responses and clear scope-specific client state; browser race test |
| R17 | High | Secret defaults or database port exposed to LAN | No usable committed password; loopback-only dev publish; runtime never bootstrap credential |
| R18 | Medium | Owner confidentiality overclaimed against infrastructure admins | Explicit trust model; RLS not cryptographic protection from a host/DB superuser |
| R19 | Medium | Unapproved language/design reduction | Preserve FR/EN/IT/AR and RTL; no imported legacy theme assumptions |
| R20 | High | Minimal nucleus presented as the complete extensible socle | Distinguish L1/L2/L3 and row-level coverage; G0 scope review and L08 qualification cannot close deferred capabilities |
| R21 | High | Technical demonstration introduces business tables/screens/rules | Technical-only providers/consumers and inert test fixtures; reject business features at every lot review, including purported reference modules |
| R22 | High | Audit arrives after protected provisioning or mutations | L04 audit before L05 exposure; integration fault injection must prove rollback; G3/G4 cannot pass separately on incompatible implementations |
| R23 | High | Capabilities enumerated without ownership or dependency/failure contracts | Track matrix gaps, concrete public contracts and negative cases; L06 validates actual baseline graph, L09 extends it without cyclic private access |
| R24 | High | Release lacks data lifecycle, key rotation or incident response | C45/C47–C50 must have slice-specific baseline requirements before L08 and complete production-readiness decisions before deployment; exercise recovery and revocation |
| R25 | High | Optional engine or phase label mistaken for implementation authority | All L09–L11/L3 allocations remain proposals; record approval per selected scope and N0 decision before code |
| R26 | High | Supply-chain or third-party license obligations confused with commercial licensing | C48 SBOM, vulnerability and dependency-license evidence before distribution; signing/provenance checks separately from C36 entitlement behavior |

Responsibility defaults to the project owner for approval and future platform/security maintainers for execution; no team assignment is invented. Close a risk only with evidence linked from a validation gate, or explicitly accept it with an owner decision and bounds.

## Related

- [Validation gates](VALIDATION-GATES.md)
- [Decision management](../00-GOVERNANCE/DECISION-MANAGEMENT.md)
- [Source blueprint](../MASTER-BLUEPRINT.md)
