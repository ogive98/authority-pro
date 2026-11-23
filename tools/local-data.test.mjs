import assert from 'node:assert/strict';
import test from 'node:test';
import http from 'node:http';
import { EventEmitter } from 'node:events';
import {
  TARGETS, sanitizedEnvironment, validateArtifactReceipt, validateBinding, validateClasspath, validateCommand,
  validateDependencyReview, validateFailsafeXml, validateJavaOutput, validateMemory,
  validatePhase, validatePreservation, validateRelease, validateRolePolicy, validateSuiteXml,
  validateTarget, validateMainRequest, validateSuccessorBuild, validatePackagedResult,
  validateListener, validateHttpProbe, validateMigratedCatalog, validateOperator,
  validateRecoveryRequest, recoveryCatalog, validateTypePrivileges, validateColumnPrivileges, requestProbe,
} from './local-data.mjs';

// Pure fixtures only: importing the guard must not read state, decrypt, spawn, or connect.
const id = 'ef431b54-099d-4032-bb1b-cb649553b5e4';
const owner = 'authority_pro_owner';
const migrator = 'authority_pro_migrator';
const runtime = 'authority_pro_runtime';
const commands = ['provision', 'qualify', 'restore', 'verify-restore'];
const copy = value => structuredClone(value);
const binding = { version: 1, id, databases: [...TARGETS], oids: { [TARGETS[0]]: '20001', [TARGETS[1]]: '20002' } };
const rows = TARGETS.map(name => ({ name, oid: binding.oids[name], owner, marker: id }));
const suite = `<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="com.authoritypro.launch.ExampleTest" tests="2" errors="0" failures="0" skipped="0" flakes="0">
  <testcase name="first" classname="com.authoritypro.launch.ExampleTest"/>
  <testcase name="second" classname="com.authoritypro.launch.ExampleTest"></testcase>
</testsuite>`;
const summary = `<?xml version="1.0" encoding="UTF-8"?>
<failsafe-summary xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" result="null" timeout="false">
<completed>2</completed><errors>0</errors><failures>0</failures><skipped>0</skipped><flakes>0</flakes>
<failureMessage xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
</failsafe-summary>`;
const release = `IMPLEMENTOR="Eclipse Adoptium"
IMPLEMENTOR_VERSION="Temurin-21.0.13+9"
JAVA_RUNTIME_VERSION="21.0.13+9-LTS"
JAVA_VERSION="21.0.13"
OS_NAME="Windows"
OS_ARCH="x86_64"
IMAGE_TYPE="JDK"
`;

function recordsBefore(command) {
  if (command === 'provision') return {};
  const records = { state: { id } };
  for (const phase of commands.slice(0, commands.indexOf(command))) {
    records[phase] = { id, status: 'passed' };
    records[`${phase}.started`] = { id, status: 'started' };
  }
  return records;
}

function javaLines(mode) {
  const checks = ['input_contract', 'effective_owner_and_fixed_endpoint', 'single_successful_history',
    'production_runtime_validation', 'final_seed_a', 'final_seed_b', 'final_migration_validation',
    mode === 'qualify' ? 'flyway_first_migration' : 'restore_validate_without_migrate'];
  const database = TARGETS[mode === 'qualify' ? 0 : 1];
  return [...checks.map(check => `PASS ${check}`), `DATA_QUALIFICATION_OK checks=${checks.length} database=${database}`].join('\n');
}

test('only the four exact one-argument commands are accepted', () => {
  for (const command of commands) assert.equal(validateCommand([command]), command);
  for (const args of [[], ['preflight'], ['run'], ['migrate'], ['migrate-local'], ['http'], ['drop'],
    ['qualify', '--force'], ['restore', TARGETS[1]], ['verify-restore', '--retry'], ['QUALIFY'], null]) {
    assert.throws(() => validateCommand(args));
  }
});

test('neither main nor old scratch databases can be execution targets', () => {
  for (const name of TARGETS) assert.equal(validateTarget(name), name);
  for (const name of ['authority_pro', 'postgres', 'authority_pro_l03_probe', 'authority_pro_l03_restore',
    'authority_pro_l03_app_test; DROP DATABASE authority_pro', `${TARGETS[0]}\n`, '', undefined]) {
    assert.throws(() => validateTarget(name));
  }
  validateTarget(TARGETS[0], 'qualify');
  validateTarget(TARGETS[1], 'verify-restore');
  assert.throws(() => validateTarget(TARGETS[1], 'qualify'));
  assert.throws(() => validateTarget(TARGETS[0], 'verify-restore'));
  assert.throws(() => validateTarget(TARGETS[0], 'migrate'));
});

test('strips ambient application, PostgreSQL and Java overrides case-insensitively', () => {
  const environment = { Path: 'safe-path', SystemRoot: 'C:\\Windows', DOCKER_CONTEXT: 'local' };
  for (const key of ['AUTHORITY_PRO_MODE', 'SPRING_APPLICATION_JSON', 'SERVER_PORT', 'MANAGEMENT_ENDPOINTS',
    'PGPASSWORD', 'PGHOST', 'PGOPTIONS', 'PGSERVICEFILE', 'JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS',
    '_JAVA_OPTIONS', 'JAVA_HOME', 'JAVA_OPTS', 'CLASSPATH', 'MAVEN_ARGS', 'MAVEN_OPTS', 'NODE_OPTIONS',
    'NODE_PATH', 'COMPOSE_FILE', 'FLYWAY_URL', 'JDBC_URL', 'HIKARI_PASSWORD', 'DATABASE_URL', 'DB_PASSWORD',
    'spring_profiles_active', 'pgpassword', 'java_tool_options']) environment[key] = 'must-not-reach-child';
  const original = copy(environment);
  assert.deepEqual(sanitizedEnvironment(environment), { Path: 'safe-path', SystemRoot: 'C:\\Windows', DOCKER_CONTEXT: 'local' });
  assert.deepEqual(environment, original);
});

test('requires exact approved Windows Temurin release, not a prefix match', () => {
  validateRelease(release);
  validateRelease(release.replaceAll('\n', '\r\n'));
  for (const changed of [release.replace('21.0.13+9-LTS', '21.0.13+90-LTS'),
    release.replace('Eclipse Adoptium', 'Other'), release.replace('x86_64', 'aarch64'),
    release.replace('Windows', 'Linux'), release.replace('IMAGE_TYPE="JDK"', ''),
    `${release}JAVA_VERSION="21.0.13"\n`]) assert.throws(() => validateRelease(changed));
});

test('classpath entries must be unique absolute jars beneath the fixed repository', () => {
  const repo = 'C:/checkout/tmp/l02/m2';
  const jar = 'C:/checkout/backend/../tmp/l02/m2/org/example/lib/1/lib-1.jar';
  assert.deepEqual(validateClasspath(`${jar}\r\n`, repo), ['C:\\checkout\\tmp\\l02\\m2\\org\\example\\lib\\1\\lib-1.jar']);
  for (const value of ['', '.', 'lib.jar', 'C:/other/lib.jar', 'C:/checkout/tmp/l02/m2-other/lib.jar',
    'C:/checkout/tmp/l02/m2/../../escape.jar', 'C:/checkout/tmp/l02/m2/*',
    'C:/checkout/tmp/l02/m2/classes', `${jar};`, `${jar};${jar.toUpperCase()}`,
    `${jar}\n--javaagent=evil`, `${jar}:hidden.jar`, '\\\\server\\share\\lib.jar']) {
    assert.throws(() => validateClasspath(value, repo));
  }
});

test('requires nonzero actual surefire cases and rejects claimed-zero failures', () => {
  assert.deepEqual(validateSuiteXml(suite), { name: 'com.authoritypro.launch.ExampleTest', tests: 2 });
  for (const xml of [suite.replace('tests="2"', 'tests="0"'), suite.replace('tests="2"', 'tests="3"'),
    suite.replace('errors="0"', 'errors="1"'), suite.replace('failures="0"', 'failures="1"'),
    suite.replace('skipped="0"', 'skipped="2"'), suite.replace('flakes="0"', 'flakes="1"'),
    suite.replace('tests="2"', 'tests="2" tests="2"'), suite.replace('errors="0"', ''),
    suite.replace('</testsuite>', '<failure message="hidden"/></testsuite>'),
    suite.replace('</testsuite>', ''), suite + suite,
    `<!DOCTYPE testsuite [<!ENTITY x SYSTEM "file:///secret">]>${suite}`]) {
    assert.throws(() => validateSuiteXml(xml));
  }
});

test('requires successful nonempty failsafe verification summary', () => {
  assert.equal(validateFailsafeXml(summary), 2);
  assert.equal(validateFailsafeXml(summary.replace('result="null"', 'result="0"')), 2);
  for (const xml of [summary.replace('result="null"', 'result="255"'), summary.replace('timeout="false"', 'timeout="true"'),
    summary.replace('<completed>2', '<completed>0'), summary.replace('<errors>0', '<errors>1'),
    summary.replace('<failures>0', '<failures>1'), summary.replace('<skipped>0', '<skipped>2'),
    summary.replace('<flakes>0', '<flakes>1'), summary.replace('xsi:nil="true"', 'xsi:nil="false"'),
    summary.replace('<errors>0</errors>', '<errors>0</errors><errors>0</errors>'),
    summary.replace('</failsafe-summary>', '<failureMessage>failed</failureMessage></failsafe-summary>'),
    summary.replace('<completed>2</completed>', ''), summary.replace('</failsafe-summary>', '')]) {
    assert.throws(() => validateFailsafeXml(xml));
  }
});

test('phases are strictly ordered and any claimed attempt prevents implicit rerun', () => {
  for (const command of commands) {
    const records = recordsBefore(command);
    validatePhase(command, records);
    for (const suffix of ['', '.started', '.failed']) {
      assert.throws(() => validatePhase(command, { ...records, [`${command}${suffix}`]: { id, status: 'passed' } }));
    }
    if (command !== 'provision') {
      const previous = commands[commands.indexOf(command) - 1];
      for (const mutation of [value => { delete value[previous]; },
        value => { value[previous].status = 'failed'; },
        value => { value[previous].id = 'another-run'; },
        value => { delete value[`${previous}.started`]; },
        value => { value[`${previous}.failed`] = { id }; },
        value => { delete value.state; }]) {
        const changed = copy(records); mutation(changed);
        assert.throws(() => validatePhase(command, changed));
      }
    }
  }
  assert.throws(() => validatePhase('provision', { state: { id } }));
  assert.throws(() => validatePhase('qualify', { ...recordsBefore('qualify'), restore: { id, status: 'passed' } }));
});

test('OID plus UUID comment and exact owner bind both newly created databases', () => {
  validateBinding(binding, rows);
  for (const mutation of [value => { value.version = 2; }, value => { value.id = "x'; ALTER ROLE x"; },
    value => { value.databases[0] = 'authority_pro'; }, value => { value.oids[TARGETS[0]] = '29999'; },
    value => { value.oids[TARGETS[0]] = value.oids[TARGETS[1]]; },
    value => { value.oids.authority_pro = '1'; }]) {
    const changed = copy(binding); mutation(changed);
    assert.throws(() => validateBinding(changed, rows));
  }
  for (const key of ['oid', 'owner', 'marker', 'name']) {
    const changed = copy(rows); changed[0][key] = 'collision';
    assert.throws(() => validateBinding(binding, changed));
  }
  assert.throws(() => validateBinding(binding, rows.slice(0, 1)));
  assert.throws(() => validateBinding(binding, [...rows, rows[0]]));
});

test('preservation compares complete identities and file sets, not a success boolean', () => {
  const before = { mainCatalog: 'catalog-hash', untouchedDatabases: [{ name: 'authority_pro_l03_probe', oid: '17' }],
    protectedContainers: [{ id: 'protected', startedAt: 'original', restarts: 0 }], hashes: { source: 'a', jar: 'b' } };
  validatePreservation(before, copy(before));
  for (const mutation of [value => { value.mainCatalog = 'changed'; },
    value => { value.untouchedDatabases[0].oid = '18'; },
    value => { value.protectedContainers[0].restarts = 1; },
    value => { value.hashes.source = 'changed'; }, value => { delete value.hashes.jar; },
    value => { value.hashes.extra = 'new'; }]) {
    const changed = copy(before); mutation(changed);
    assert.throws(() => validatePreservation(before, changed));
  }
});

test('reproducible artifacts require the exact digest from fresh packaged tests, not jar timestamps', () => {
  const digest = 'a'.repeat(64);
  validateArtifactReceipt(`${digest}\n`, digest, 200, 100, 300);
  validateArtifactReceipt(`${digest}\r\n`, digest, 200, 200, 200);
  for (const args of [[`${digest}\n`, 'b'.repeat(64), 200, 100, 300],
    [`${digest}\n`, digest, 99, 100, 300], [`${digest}\n`, digest, 301, 100, 300],
    [`${digest}\n`, digest, NaN, 100, 300], [`${digest}\n`, digest, 200, Infinity, 300],
    [`${digest}\n`, digest, 200, 100, NaN], [digest, digest, 200, 100, 300],
    [`${digest}\nextra`, digest, 200, 100, 300]]) assert.throws(() => validateArtifactReceipt(...args));
});

test('memory budget is at least 640 MiB with no NaN or infinity bypass', () => {
  validateMemory(640 * 1024 * 1024);
  for (const value of [0, 640 * 1024 * 1024 - 1, NaN, Infinity, '671088640']) assert.throws(() => validateMemory(value));
});

test('role policy rejects elevated flags, login owner and extra membership paths', () => {
  const roles = [migrator, owner, runtime].map(rolname => ({ rolname, rolcanlogin: rolname !== owner,
    rolsuper: false, rolcreatedb: false, rolcreaterole: false, rolbypassrls: false, rolreplication: false,
    rolinherit: false, rolconnlimit: rolname === migrator ? 2 : rolname === runtime ? 5 : -1 }));
  const memberships = [{ parent: owner, member: migrator, admin_option: false, inherit_option: false, set_option: true }];
  validateRolePolicy(roles, memberships);
  for (const index of [0, 1, 2]) {
    for (const flag of ['rolcanlogin', 'rolsuper', 'rolcreatedb', 'rolcreaterole', 'rolbypassrls', 'rolreplication', 'rolinherit']) {
      const changed = copy(roles); changed[index][flag] = !changed[index][flag];
      assert.throws(() => validateRolePolicy(changed, memberships));
    }
    const changed = copy(roles); changed[index].rolconnlimit = 99;
    assert.throws(() => validateRolePolicy(changed, memberships));
  }
  for (const flag of ['admin_option', 'inherit_option', 'set_option']) {
    const changed = copy(memberships); changed[0][flag] = !changed[0][flag];
    assert.throws(() => validateRolePolicy(roles, changed));
  }
  assert.throws(() => validateRolePolicy(roles, []));
  assert.throws(() => validateRolePolicy(roles, [...memberships, { parent: owner, member: runtime }]));
});

test('Java output must be allowlisted, complete and bound to the precise mode and database', () => {
  for (const mode of ['qualify', 'verify-restore']) {
    const database = TARGETS[mode === 'qualify' ? 0 : 1];
    const output = javaLines(mode);
    assert.equal(validateJavaOutput(output, database, mode).length, 9);
    for (const text of [`secret diagnostic\n${output}`, `${output}\nraw stderr`,
      output.replace('checks=8', 'checks=9'), output.replace('PASS final_seed_a', 'PASS arbitrary'),
      output.replace('PASS final_seed_b', 'PASS final_seed_a'),
      output.replace(database, 'authority_pro'), output.replace('DATA_QUALIFICATION_OK', 'DATA_QUALIFICATION_FAILED'),
      'DATA_QUALIFICATION_FAILED check=input_contract type=Failure sqlState=none',
      output.replace('PASS input_contract', 'PASS input_contract\u001b[31m')]) {
      assert.throws(() => validateJavaOutput(text, database, mode));
    }
  }
});

function qualifiedRecords() {
  const records = { state: { version: 1, id, databases: [...TARGETS] } };
  for (const command of commands) {
    records[command] = { id, command, status: 'passed', protectedContainersUnchanged: true,
      mainDatabaseRemainsEmpty: true, untouchedDatabaseIdentitiesUnchanged: true, artifactsUnchanged: true };
    records[`${command}.started`] = { id, status: 'started' };
  }
  records.provision.binding = copy(binding);
  records.qualify.checks = javaLines('qualify').split('\n');
  records['verify-restore'].checks = javaLines('verify-restore').split('\n');
  return records;
}

test('main migration needs all successful qualification journals and rejects every prior main claim', () => {
  validateMainRequest(['migrate-main'], qualifiedRecords(), []);
  for (const args of [[], ['migrate-main', '--retry'], ['migrate-main', 'other'], ['drop'], ['MIGRATE-MAIN']]) {
    assert.throws(() => validateMainRequest(args, qualifiedRecords(), []));
  }
  for (const name of ['main.started', 'main.migrated', 'main.noop', 'main.passed', 'main.failed']) {
    assert.throws(() => validateMainRequest(['migrate-main'], qualifiedRecords(), [name]));
  }
  for (const command of commands) {
    for (const mutate of [r => { delete r[command]; }, r => { r[command].id = 'foreign'; },
      r => { r[command].status = 'failed'; }, r => { r[`${command}.failed`] = { id }; },
      r => { delete r[`${command}.started`]; }, r => { r[command].artifactsUnchanged = false; },
      r => { r[command].mainDatabaseRemainsEmpty = false; }]) {
      const records = qualifiedRecords(); mutate(records);
      assert.throws(() => validateMainRequest(['migrate-main'], records, []));
    }
  }
});

test('successor permits only two reviewed runner inputs and fresh reports while keeping all executable bytes', () => {
  const paths = ['backend/pom.xml', 'backend/src/main/java/Example.java', 'backend/src/test/java/Test.java',
    'backend/target/classes/Example.class', 'backend/target/test-classes/Test.class',
    'backend/target/authority-pro-backend-0.0.1-SNAPSHOT.jar', 'backend/target/packaged-artifact.sha256',
    'tmp/l03-app/classpath.txt', 'tmp/l03-app/dependency-review.json', 'tmp/l02/m2/lib.jar',
    'tmp/l02/tooling/jdk/bin/java.exe', 'package.json', 'tools/local-environment.mjs',
    'tools/local-data.mjs', 'tools/local-data.test.mjs',
    'backend/target/surefire-reports/TEST-com.authoritypro.Test.xml',
    'backend/target/failsafe-reports/failsafe-summary.xml'];
  const previous = { hashes: Object.fromEntries(paths.map(path => [path, 'a'.repeat(64)])),
    classpath: ['fixed.jar'], tests: { surefire: [{ name: 'Test', tests: 1 }] } };
  validateSuccessorBuild(previous, copy(previous));
  for (const path of paths) {
    const changed = copy(previous); changed.hashes[path] = 'b'.repeat(64);
    if (path.startsWith('tools/local-data') || path.includes('-reports/')) validateSuccessorBuild(previous, changed);
    else assert.throws(() => validateSuccessorBuild(previous, changed));
  }
  for (const mutate of [r => { delete r.hashes['backend/pom.xml']; }, r => { r.hashes.extra = 'a'.repeat(64); },
    r => { r.classpath = ['foreign.jar']; }, r => { r.tests.surefire[0].tests = 0; },
    r => { r.hashes['tools/local-data.mjs'] = 'invalid'; }]) {
    const changed = copy(previous); mutate(changed);
    assert.throws(() => validateSuccessorBuild(previous, changed));
  }
});

test('packaged migration requires exact sanitized output, exit status and no timeout', () => {
  const result = { status: 0, signal: null, stdout: Buffer.from('MIGRATION_OK count=1\n'), stderr: Buffer.alloc(0) };
  validatePackagedResult(result, 'MIGRATION_OK count=1');
  validatePackagedResult({ ...result, status: 2, stdout: Buffer.alloc(0), stderr: Buffer.from('DATABASE_STARTUP_REFUSED\n') }, 'DATABASE_STARTUP_REFUSED', 2);
  for (const changed of [{ ...result, status: 1 }, { ...result, signal: 'SIGTERM' },
    { ...result, error: new Error('timeout') }, { ...result, stderr: Buffer.from('unexpected') },
    { ...result, stdout: Buffer.from('MIGRATION_OK count=0') }]) {
    assert.throws(() => validatePackagedResult(changed, 'MIGRATION_OK count=1'));
  }
});

test('HTTP evidence rejects a foreign reused PID, wildcard listener, additional socket or missing listener', () => {
  const started = Date.now() - 5000;
  const time = new Date(started + 10).toISOString();
  const row = { address: '127.0.0.1', pid: 12345, startedAt: time };
  assert.equal(validateListener([row], 12345, started), time);
  validateListener([row], 12345, started, time);
  for (const rows of [[], [row, row], [{ ...row, address: '0.0.0.0' }], [{ ...row, address: '::' }],
    [{ ...row, pid: 12346 }], [{ ...row, startedAt: new Date(started - 2000).toISOString() }],
    [{ ...row, startedAt: 'invalid' }]]) assert.throws(() => validateListener(rows, 12345, started));
  assert.throws(() => validateListener([row], 12345, started, new Date(started).toISOString()));
});

test('real HTTP checks retain closed probes, CSRF denials and absence of sessions or redirects', () => {
  const live = '/actuator/health/liveness';
  validateHttpProbe('GET', live, 200, {}, '{"status":"UP"}');
  validateHttpProbe('GET', '/actuator/health/readiness', 503, {}, '{"status":"OUT_OF_SERVICE"}');
  const headers = { 'content-type': 'application/problem+json' };
  const body = JSON.stringify({ status: 403, traceId: 'a'.repeat(32), code: 'ACCESS_DENIED' });
  validateHttpProbe('GET', '/', 403, headers, body);
  validateHttpProbe('HEAD', live, 403, headers, '');
  validateHttpProbe('TRACE', live, 403, headers, body);
  assert.throws(() => validateHttpProbe('TRACE', live, 200, {}, 'TRACE accepted'));
  const csrfHeaders = { ...headers, 'set-cookie': [`XSRF-TOKEN=${id}; Path=/; HttpOnly; SameSite=Strict`] };
  validateHttpProbe('POST', live, 403, csrfHeaders, body.replace('ACCESS_DENIED', 'CSRF_REJECTED'));
  for (const cookie of [`JSESSIONID=${id}; Path=/; HttpOnly; SameSite=Strict`,
    `XSRF-TOKEN=${id}; Path=/; SameSite=Strict`, `XSRF-TOKEN=${id}; Path=/; HttpOnly; SameSite=None`]) {
    assert.throws(() => validateHttpProbe('POST', live, 403, { ...headers, 'set-cookie': [cookie] }, body));
  }
  for (const args of [['GET', live, 503, {}, '{"status":"UP"}'],
    ['GET', live, 200, { 'set-cookie': 'session' }, '{"status":"UP"}'],
    ['GET', '/', 302, { location: '/login' }, body], ['GET', '/', 403, { 'www-authenticate': 'Basic' }, body],
    ['GET', '/', 403, headers, body.replace('403', '200')], ['POST', live, 403, headers, body],
    ['HEAD', live, 403, headers, body]]) assert.throws(() => validateHttpProbe(...args));
});

test('operator counts require the exact effective superuser so RLS cannot hide fixture rows', () => {
  validateOperator([{ role: 'authority_pro_operator', superuser: true }]);
  for (const rows of [[], [{ role: 'authority_pro_operator', superuser: false }],
    [{ role: 'authority_pro_runtime', superuser: true }], [{ role: 'authority_pro_operator', superuser: 'true' }]]) {
    assert.throws(() => validateOperator(rows));
  }
});

test('migrated main must match qualified catalog and contain no tenant fixtures', () => {
  const expected = [[{ schema: 'tenancy', force: true }], [{ version: '1', checksum: 123 }]];
  const empty = [{ tenants: 0, companies: 0, sites: 0, memberships: 0 }];
  validateMigratedCatalog(copy(expected), expected, empty);
  const changed = copy(expected); changed[0][0].force = false;
  assert.throws(() => validateMigratedCatalog(changed, expected, empty));
  for (const key of Object.keys(empty[0])) {
    assert.throws(() => validateMigratedCatalog(expected, expected, [{ ...empty[0], [key]: 1 }]));
  }
  assert.throws(() => validateMigratedCatalog(expected, expected, []));
});

test('recovery binds the exact failed attempt, original journal digests and qualification without permitting replay', () => {
  const records = JSON.parse(JSON.stringify(qualifiedRecords()).replaceAll(id, 'd1af7045-a3f7-4042-b750-1839aedb32e8'));
  records.state.before = { containerId: 'fixed' };
  const started = { id: '356dcc34-3a3a-4227-ba84-630691755f8c',
    qualificationId: records.state.id, status: 'started', before: copy(records.state.before) };
  const failed = { id: started.id, status: 'failed', checkedAt: '2026-11-20T11:52:37.323Z',
    mutationStarted: true, preservationVerified: false, resourcesRetained: true, rawOutputWithheld: true };
  const digests = {
    'main.started': '5991a1d16cdaaf0a203aa10c5c6dfb556554200773bc02535a0b953be2b5e227',
    'main.failed': '6edf3205e83a194ce82c079693fd47630e3609babc9d629d15992726a5b05a5a',
  };
  const args = [['recover-main'], records, started, failed, digests, []];
  validateRecoveryRequest(...args);
  for (const command of [[], ['recover-main', '--force'], ['migrate-main'], ['RECOVER-MAIN']]) {
    assert.throws(() => validateRecoveryRequest(command, ...args.slice(1)));
  }
  for (const name of ['recovery.started', 'recovery.noop', 'recovery.failed', 'recovery.passed']) {
    assert.throws(() => validateRecoveryRequest(...args.slice(0, 5), [name]));
  }
  for (const mutate of [r => { r[2].id = id; }, r => { r[2].qualificationId = id; },
    r => { r[2].status = 'passed'; }, r => { r[2].before.containerId = 'foreign'; },
    r => { r[3].id = id; }, r => { r[3].status = 'passed'; }, r => { r[3].mutationStarted = false; },
    r => { r[3].preservationVerified = true; }, r => { r[3].resourcesRetained = false; },
    r => { r[3].checkedAt = '2026-11-21T11:52:37.323Z'; }, r => { delete r[4]['main.failed']; },
    r => { r[4]['main.started'] = 'a'.repeat(64); }, r => { r[4].extra = 'a'.repeat(64); },
    r => { r[1].qualify.status = 'failed'; }]) {
    const changed = copy(args); mutate(changed);
    assert.throws(() => validateRecoveryRequest(...changed));
  }
});

test('recovery accepts only the two initialized default ACLs while rejecting any unrelated catalog drift', () => {
  const qualified = Array.from({ length: 9 }, () => []);
  qualified[0] = [{ nspname: 'public', owner }];
  qualified[8] = [{ version: '1', type: 'SQL', script: 'V1__tenancy_foundation.sql', checksum: -1937331583, success: true }];
  const before = copy(qualified);
  const expected = recoveryCatalog(qualified);
  assert.deepEqual(qualified, before);
  assert.deepEqual(expected[6], [
    { owner, nspname: null, defaclobjtype: 'T', acl: `{${owner}=U/${owner}}` },
    { owner, nspname: null, defaclobjtype: 'f', acl: `{${owner}=X/${owner}}` },
  ]);
  const empty = [{ tenants: 0, companies: 0, sites: 0, memberships: 0 }];
  validateMigratedCatalog(copy(expected), expected, empty);
  for (const mutate of [r => { r[6] = []; }, r => { r[6].pop(); }, r => { r[6].push(copy(r[6][0])); },
    r => { r[6][0].acl = `{=U/${owner},${owner}=U/${owner}}`; },
    r => { r[6][1].acl = `{${runtime}=X/${owner},${owner}=X/${owner}}`; },
    r => { r[6][0].owner = runtime; }, r => { r[6][0].nspname = 'tenancy'; },
    r => { r[8][0].checksum = 123; }, r => { r[8].push(copy(r[8][0])); },
    r => { r[0][0].owner = runtime; }]) {
    const changed = copy(expected); mutate(changed);
    assert.throws(() => validateMigratedCatalog(changed, expected, empty));
  }
  for (let index = 0; index < 9; index++) {
    const changed = copy(expected); changed[index].push({ unrelated: true });
    assert.throws(() => validateMigratedCatalog(changed, expected, empty));
  }
  for (const mutate of [r => { r.pop(); }, r => { r[6] = copy(expected[6]); },
    r => { r[8][0].checksum = 123; }, r => { r[8][0].success = false; }]) {
    const changed = copy(qualified); mutate(changed);
    assert.throws(() => recoveryCatalog(changed));
  }
});

test('type privilege inspection requires the exact owner, row and array type set and unchanged ACLs', () => {
  const rows = [['public', '_flyway_schema_history'], ['public', 'flyway_schema_history'],
    ...['_companies', '_memberships', '_sites', '_tenants', 'companies', 'memberships', 'sites', 'tenants']
      .map(name => ['tenancy', name])].map(([nspname, typname]) => ({ nspname, typname, owner, acl: null, runtime_usage: true }));
  validateTypePrivileges(rows);
  for (const mutate of [r => { r.pop(); }, r => { r.push(copy(r[0])); }, r => { r[0].owner = runtime; },
    r => { r[0].acl = `{${runtime}=U/${owner}}`; }, r => { r[0].runtime_usage = false; },
    r => { r[0].nspname = 'foreign'; }, r => { r[0].typname = 'extra'; }]) {
    const changed = copy(rows); mutate(changed);
    assert.throws(() => validateTypePrivileges(changed));
  }
});

test('column grants cannot silently allow runtime writes to migration history', () => {
  validateColumnPrivileges([]);
  for (const row of [{ nspname: 'public', relname: 'flyway_schema_history', attname: 'checksum', acl: `{${runtime}=w/${owner}}` },
    { nspname: 'tenancy', relname: 'memberships', attname: 'active', acl: `{${runtime}=w/${owner}}` },
    { nspname: 'tenancy', relname: 'tenants', attname: 'name', acl: '{}' }]) {
    assert.throws(() => validateColumnPrivileges([row]));
  }
});

test('a trickling HTTP response cannot extend the absolute probe deadline', async t => {
  t.mock.timers.enable({ apis: ['setTimeout'] });
  const request = new EventEmitter();
  const response = new EventEmitter();
  response.statusCode = 200;
  response.headers = {};
  let destroyed = false;
  request.setTimeout = () => request;
  request.destroy = error => { destroyed = true; request.emit('error', error); request.emit('close'); };
  t.mock.method(http, 'request', (options, callback) => {
    assert.equal(options.host, '127.0.0.1');
    assert.equal(options.port, 8080);
    request.end = () => callback(response);
    return request;
  });
  const rejected = assert.rejects(requestProbe('GET', '/actuator/health/liveness'), /HTTP_PROBE_FAILED/);
  for (let i = 0; i < 11; i++) {
    response.emit('data', Buffer.from('x'));
    t.mock.timers.tick(250);
    assert.equal(destroyed, false);
  }
  response.emit('data', Buffer.from('x'));
  t.mock.timers.tick(250);
  await rejected;
  assert.equal(destroyed, true);
});

test('dependency review covers exactly the executed jar bytes, with no findings', () => {
  const dependencies = [{ path: 'C:/repo/org/example/library/1.0/library-1.0.jar', sha256: 'a'.repeat(64) }];
  const review = { source: 'https://api.osv.dev/v1/querybatch', checkedAt: '2026-11-19T00:00:00Z', findings: [],
    inventory: [{ group: 'org.example', artifact: 'library', version: '1.0', sha256: 'a'.repeat(64), licenses: ['Apache-2.0'] }] };
  validateDependencyReview(review, dependencies);
  for (const mutation of [value => { value.findings.push({ id: 'unresolved' }); },
    value => { value.inventory[0].sha256 = 'b'.repeat(64); }, value => { value.inventory[0].licenses = []; },
    value => { value.inventory[0].version = '2.0'; }, value => { value.inventory = []; },
    value => { value.checkedAt = 'invalid'; }, value => { value.source = 'unreviewed'; }]) {
    const changed = copy(review); mutation(changed);
    assert.throws(() => validateDependencyReview(changed, dependencies));
  }
  assert.throws(() => validateDependencyReview(review, [...dependencies, { path: 'C:/repo/extra.jar', sha256: 'b'.repeat(64) }]));
});
