# ADR-0010 — Candidate versions and compatibility evidence

- **Status:** PROPOSED numeric matrix; technology orientations remain approved, but no application dependency set is owner-approved or pinned.
- **Evidence state:** Local tools and authoritative compatibility sources checked; disposable probe results are distinguished below from unexecuted application/database tests.

- **Record reviewed:** 2026-11-17; this is not an approval date.
- **Accountable owner / approval authority:** Project owner.
- **Application delivery:** NOT IMPLEMENTED; application acceptance and release evidence absent. Disposable compatibility tests are recorded separately below.
- **Scope note:** The pinned documentation-validator dependencies are not the application version matrix.

## Local environment observed — 2026-11-17

Read-only commands in Git Bash reported Microsoft OpenJDK `21.0.12.1+1-LTS`
(`java` and `javac`), Node.js `24.19.0`, npm `11.17.0` and the PostgreSQL **client**
`16.1`. `mvn` was not found on PATH. These are observations, not approved production
pins or evidence of the running PostgreSQL server's version. No service, PATH or
global tool installation was changed, and no database connection was opened.

The desktop host's reported Node version is not necessarily the shell executable.
Build evidence must name the actual `node --version` used. Use a project-local
Maven distribution/wrapper rather than changing the machine globally. The existing
server's endpoint/version and authorized operator connection remain prerequisites
for the separate database-bearing slice; never infer them from Compose or psql.

## Context

Angular and Spring Boot are approved technologies, not approved numeric releases.
The foundation needs a reproducible, supported combination of JDK, backend
libraries, frontend compiler/toolchain and PostgreSQL. A package existing in a
registry proves publication at most, not stability, support or interoperability.
Official compatibility/lifecycle documents and exact published package metadata
were consulted on 2026-11-17. References and bounded probe outcomes are recorded
below; documentation evidence is not application acceptance.

## Options considered

1. Propose Java 21 as the initial baseline and select a supported Spring Boot
   line plus compatible libraries/toolchain after verification — recommended.
2. Select a newer JDK baseline if verified library requirements and operational
   support justify it — needs a new comparison, not an automatic upgrade.
3. Copy previously asserted numeric versions or select "latest" packages —
   rejected as a reproducibility and compatibility policy.

## Proposed decision and approval gate

Recommend the following candidates for the next bounded implementation decision,
not as an already approved production matrix. Backend versions come from the
published Boot 4.1.2 BOM unless explicitly identified otherwise. No unrelated
latest-version overrides are proposed. A disposable probe does not authorize
application scaffolding, database provisioning or adoption of every optional tool.

| Component | Candidate | Evidence / remaining boundary |
| --- | --- | --- |
| Java | Java 21; proposed reproducible distribution Temurin 21.0.13+9 | GA verified; local probes use the existing Microsoft 21.0.12.1, not Temurin; rerun after approving distribution/patch |
| Spring Boot / Framework | 4.1.2 / 7.0.10 | GA and exact Boot BOM checked; Java 21 within documented requirements |
| Spring Modulith | 2.1.2 BOM | Separately versioned; published POM aligns with Boot 4.1.2; actual application boundary tests still required |
| Spring Security / Spring Session JDBC | 7.1.2 / 4.1.1 | Boot-managed; no session-store, CSRF or authorization integration proof yet |
| springdoc | 3.2.0, webmvc-api starter | Explicit version, not Boot-managed; endpoint/schema behavior still untested |
| Hibernate / pgjdbc | 7.4.13.Final / 42.7.14 | Boot-managed; JPA adoption and actual server compatibility remain gated |
| Flyway core / PostgreSQL module | 12.4.0 / 12.4.0 | Both Boot-managed; PostgreSQL adapter required; no migration executed |
| Servlet / Tomcat | 6.1.0 / 11.0.27 | Boot-managed; do not reuse Boot 3 servlet assumptions |
| JSON | Jackson 3 BOM 3.1.7; Jackson 2 BOM 2.21.7 also imported | Boot defaults to Jackson 3; springdoc still brings Jackson 2; not an exclusively Jackson 3 classpath |
| PostgreSQL | Existing server version still UNKNOWN | Client 16.1 is not server evidence; new dedicated DB only, old application untouched |
| Angular fallback | Framework/compiler 21.2.26; CLI/build 21.2.30 | Current LTS alternative passes the bounded checks below; development advisories remain open |
| TypeScript / RxJS for Angular 21 | 5.9.3 / 7.8.2 | Inside the official Angular 21 ranges; do not substitute TypeScript 7 |
| Angular 22 candidate | Framework/compiler 22.2.6; CLI/build 22.2.7; TypeScript 6.0.3; RxJS 7.8.2 | Officially supported ranges, but rejected for immediate pinning by strict npm resolution failure below |
| Node.js / npm | Probe: 24.19.0 / 11.17.0; future Node patch candidate 24.22.3 | Local versions satisfy the tested Angular ranges; Node 24 is Maintenance LTS; newer patch is published but not installed/tested here |
| Maven / wrapper | Maven 3.9.16; proposed wrapper 3.3.4 only-script | Maven downloaded and checksum-verified in ignored scratch space; wrapper not generated |

Support trade-offs checked on 2026-11-17: Boot 4.1 OSS maintenance is listed through
July 2027; Boot 4.0 through December 2026; Boot 3.5 OSS maintenance ended in June
2026, so no free-maintenance 3.x fallback is assumed. Java 25 is a supported GA
alternative, but offers no demonstrated requirement over the existing Java 21
baseline. Maven 3.10.1 is available; 3.9.16 remains maintained and avoids adding
the Resolver 2 transition to this first probe.

Angular 22 is active, with support listed through June 2028; Angular 21 is LTS
through June 2027. Angular 20 approaches its 2026-11-28 support end and is not a
new-project recommendation. The shorter-lived Angular 21 fallback must be reviewed
again before L07; a passing small probe is not a reason to freeze an outdated
frontend months later. Node 24 maintenance ends on 2028-04-30.

Publication metadata is not enough: Maven Central's `release` entry can point to
Boot 4.2.0-RC2, and Angular's `next` tag points to 22.3.0-rc.0. Neither prerelease
is selected. Official compatibility constraints, exact artifacts and executed
checks take precedence over an unqualified `latest` label.

## Compatibility evidence — 2026-11-17

All probes are disposable under the Git-ignored `tmp/compatibility-20261117/`.
Their manifests are test inputs, not application pins. No server, browser UI,
PostgreSQL connection, migration, default account or business feature was created.
The ordinary repository manifests, lockfile, CI, Compose and global tools remain
unchanged. Scratch files are not a durable CI fixture and are excluded from the
proposed documentation commit.

### Frontend

- Official Angular 22 constraints are Node `^22.22.3 || ^24.15.0 || ^26.0.0`,
  TypeScript `>=6.0.0 <6.1.0`, RxJS `^6.5.3 || ^7.4.0`. The registry's TypeScript
  `latest` 7.0.2 is outside that range. Angular 21's documented TypeScript range
  is `>=5.9.0 <6.0.0`; retain that intersection even though its compiler package
  advertises a wider upper bound.
- `frontend/`: `npm install --ignore-scripts --strict-peer-deps --no-audit --no-fund`
  against the public npm registry rejected the Angular 22 candidate with ERESOLVE.
  Build 22.2.7 selects optional `@oxc-parser/binding-wasm32-wasi` 0.156.0, which
  requires `@emnapi/core` 2.0.0-alpha.6. Its `@napi-rs/wasm-runtime` dependency
  resolves to 1.2.7, whose peer range excludes that alpha. Metadata also shows
  1.2.6 excludes it. Build 22.2.6 has the same parser; blindly stepping back one
  patch is not a verified repair. No `--force`, `--legacy-peer-deps`, suppression
  of strict peer checking or unsupported override was used.
- `frontend-lts/`: the same strict command installed the Angular 21 candidate
  successfully (405 packages). Lifecycle scripts stayed disabled.
  `ngc -p tsconfig.json` compiled an injectable service using signals/computed and RxJS;
  two Node assertions verified values before/after a signal update. A separate
  negative compiler case rejected a string supplied to `signal<number>` with
  TS2345. This is AOT/type/linkage evidence, not an Angular CLI application build
  or a browser feature test; native builder execution remains unverified.
- `npm audit --json`: 16 moderate affected dependency entries, zero high/critical,
  with two distinct advisory roots in the development graph (glob and minimatch).
  This is not 16 independent CVEs. `npm audit --omit=dev --json`: zero reported
  findings. CLI update advice points to 22.2.7, whose full candidate fails above;
  do not run `npm audit fix --force`. Development-tool exposure and any temporary
  exception need review before choosing a production lockfile. Registry audit is
  a point-in-time check, not a security guarantee.
- Angular 22 browser support uses Baseline date 2026-05-07; Angular 21 uses
  2025-10-20. Real browser coverage, FR/EN/IT/AR, RTL, themes and accessibility
  remain L07 acceptance work, not inferred from compiler success.

### Backend

Maven 3.9.16 was downloaded only into scratch space. Its ZIP matched the publisher's
SHA-512 before extraction:
`ed41650d42485cfc243fad22158caf9cbb5dc408ce7a09ddb94dd42a019de929ca43065bfa450612cf12bf78b5cafa3884b96c090de326ff590448c933454af3`.
This checks transfer integrity against the same publisher endpoint, not independent
signature verification. `mvn --version` confirmed it runs on the existing Microsoft
JDK. The probe uses its own empty global/user settings and local repository, not
other projects' Maven credentials/configuration. Its first dependency download
failed with connection resets; this was a transport failure, not an incompatibility
finding. A serialized-transfer retry (`-U -Dmaven.artifact.threads=1`) succeeded
without changing dependency versions or disabling verification.

- `test`, `maven-help-plugin:3.5.1:effective-pom` and
  `maven-dependency-plugin:3.8.1:tree` completed with BUILD SUCCESS on the existing
  Microsoft JDK 21. The compiler used release 21; Surefire reported **5 tests,
  0 failures, 0 errors, 0 skipped**. An offline `-o test` rerun passed the same
  five tests from the populated isolated repository; this was not a clean install.
- The tests asserted Boot 4.1.2, Framework 7.0.10, Security 7.1.2 and Hibernate
  7.4.13.Final versions; positive/negative password-encoder matching; an in-memory
  session attribute round-trip and JDBC repository class linkage; Jackson 3 writing
  a small numeric JSON value read by Jackson 2; and class linkage for Modulith,
  springdoc, Flyway and its PostgreSQL adapter, and pgjdbc. No Spring application
  context, web server or JDBC connection was opened.
- The effective POM and resolved tree confirmed Session JDBC 4.1.1, Flyway and its
  PostgreSQL adapter 12.4.0, pgjdbc 42.7.14, Modulith 2.1.2, springdoc 3.2.0,
  Tomcat 11.0.27, Jackson 3.1.7 and Jackson 2.21.7. The selected tree also contains
  ArchUnit 1.4.2 and JUnit Jupiter 6.0.3; compiler/Surefire plugins are 3.15.0/3.5.6.
  This is dependency-resolution and narrow linkage evidence, not exhaustive
  dependency convergence, an SBOM/vulnerability scan or application integration.
- The automatic Modulith annotation processor wrote an empty Javadoc artifact
  relative to the first shell working directory. That probe-generated file was
  removed; subsequent execution used the scratch backend as its working directory.
  A future build must explicitly configure processor selection/output location.

Local rerun, from `tmp/compatibility-20261117/backend/` in Git Bash while this
ignored scratch directory is retained:

```bash
MAVEN_OPTS="-Xmx512m" ../tooling/apache-maven-3.9.16/bin/mvn \
  -B -ntp -o -s settings.xml -gs settings.xml \
  -Dmaven.repo.local=../m2 -f pom.xml test
```

The local inputs are `pom.xml`, `settings.xml` and
`src/test/java/com/authoritypro/probe/CompatibilityTest.java`; outputs are
`effective-pom.xml`, `dependency-tree.txt` and the Surefire reports under `target/`.
They are disposable evidence, unavailable in a clean clone; L02 must introduce
reviewed, versioned fixtures and clean-build checks rather than depending on them.

### Remaining gates

The frontend recommendation is conditional, not approved; the rejected Angular 22
candidate and development advisories remain explicit findings. The backend cannot
claim database, session/CSRF, RLS, OpenAPI endpoint, module-boundary or audit-atomicity
acceptance from dependency metadata or a class-loading probe. No G1/G2 completion
or production readiness follows from this evidence.

## Verification and tests before pinning

Remaining work below must supplement, not relabel, the bounded evidence above:

1. Recheck candidate release/support/advisory information at the actual pinning
   date, confirm the JDK distribution/patch and approve applicable library/tool
   adoption. The 2026-11-17 source review is not an indefinite support guarantee.
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
   dependency management in future foundation work. This revision changes only
   tracked documentation; disposable probe code/configuration stays outside the
   application and is ignored by Git.

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

There is no existing Authority Pro application to upgrade. The other application
using PostgreSQL is outside this operation and must remain untouched. Establish
and test the first approved matrix before production scaffolding. Later upgrades require compatibility tests,
release-note review and schema/session serialization checks, with an application
rollback plan that respects migration compatibility. Do not silently relabel an
unverified combination as approved.

## Official sources checked — 2026-11-17

- [Spring Boot 4.1.2 GA](https://spring.io/blog/2026/10/23/spring-boot-4-1-2-available-now), [requirements](https://docs.spring.io/spring-boot/system-requirements.html), [maintenance metadata](https://spring.io/page-data/projects/spring-boot/page-data.json) and [exact BOM](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.2/spring-boot-dependencies-4.1.2.pom).
- [Modulith 2.1.2 release](https://spring.io/blog/2026/10/23/spring-modulith-2-2-rc1-2-1-2-2-0-9-and-1-4-14-released) and [exact POM](https://repo.maven.apache.org/maven2/org/springframework/modulith/spring-modulith-core/2.1.2/spring-modulith-core-2.1.2.pom).
- [springdoc compatibility](https://springdoc.org/faq.html), [3.2.0 release](https://github.com/springdoc/springdoc-openapi/releases/tag/v3.2.0) and [JSON dependencies](https://repo.maven.apache.org/maven2/org/springdoc/springdoc-openapi-starter-common/3.2.0/springdoc-openapi-starter-common-3.2.0.pom).
- [Hibernate 7.4](https://hibernate.org/orm/releases/7.4/), [Boot JSON](https://docs.spring.io/spring-boot/4.1/reference/features/json.html), [JDBC session configuration](https://docs.spring.io/spring-session/reference/configuration/jdbc.html) and [Flyway PostgreSQL adapter](https://documentation.red-gate.com/fd/postgresql-database-277579325.html).
- [Temurin support roadmap](https://adoptium.net/support), [Java 21 GA](https://github.com/adoptium/temurin21-binaries/releases/tag/jdk-21.0.13%2B9), [Maven maintenance/downloads](https://maven.apache.org/download.cgi) and [wrapper](https://maven.apache.org/wrapper/).
- [Angular compatibility and browsers](https://angular.dev/reference/versions), [release/support schedule](https://angular.dev/reference/releases), [core metadata](https://registry.npmjs.org/@angular%2Fcore), [CLI metadata](https://registry.npmjs.org/@angular%2Fcli) and [build metadata](https://registry.npmjs.org/@angular%2Fbuild).
- [Parser 0.156.0 metadata](https://registry.npmjs.org/@oxc-parser%2Fbinding-wasm32-wasi/0.156.0), [WASM runtime 1.2.7 peers](https://registry.npmjs.org/@napi-rs%2Fwasm-runtime/1.2.7), [TypeScript metadata](https://registry.npmjs.org/typescript) and [RxJS metadata](https://registry.npmjs.org/rxjs).
- [Node release status](https://nodejs.org/en/about/previous-releases) and [official lifecycle schedule](https://github.com/nodejs/Release/blob/main/schedule.json). Direct Node access to the distribution index timed out; no binary download/install is claimed from that attempt.
- [glob advisory](https://github.com/advisories/GHSA-3g3m-2fjr-6pw4), [minimatch advisory](https://github.com/advisories/GHSA-g3hv-75wg-m249), [Spring Security targeted advisory](https://spring.io/security/cve-2026-47841) and [pgjdbc 42.7.14 fixes](https://jdbc.postgresql.org/changelogs/2026-10-07-42.7.14-release/). This targeted review does not replace an exhaustive backend dependency/SBOM scan.

## Related

- [ADR-0001 — Approved technologies](ADR-0001-stack.md)
- [ADR-0002 — Proposed modularity tooling](ADR-0002-modular-monolith.md)
- [ADR-0005 — Security and JDBC sessions](ADR-0005-sessions.md)
- [ADR-0006 — Durability spike](ADR-0006-outbox.md)
- [ADR-0007 — Proposed Flyway integration](ADR-0007-flyway.md)
- [ADR-0011 — API and probes](ADR-0011-api-versioning.md)
