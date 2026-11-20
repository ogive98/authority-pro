import assert from 'node:assert/strict';
import test from 'node:test';
import {
  TARGETS, sanitizedEnvironment, validateArtifactReceipt, validateBinding, validateClasspath, validateCommand,
  validateDependencyReview, validateFailsafeXml, validateJavaOutput, validateMemory,
  validatePhase, validatePreservation, validateRelease, validateRolePolicy, validateSuiteXml,
  validateTarget,
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
