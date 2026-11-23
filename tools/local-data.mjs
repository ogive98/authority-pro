import assert from 'node:assert/strict';
import { spawn, spawnSync } from 'node:child_process';
import net from 'node:net';
import http from 'node:http';
import { setTimeout as delay } from 'node:timers/promises';
import { createHash, randomUUID } from 'node:crypto';
import { existsSync, lstatSync, readFileSync, readdirSync, realpathSync, writeFileSync } from 'node:fs';
import { freemem } from 'node:os';
import { dirname, join, relative, resolve, win32 } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import pg from 'pg';
import { accessLocalEnvironment, protectedContainerSnapshot } from './local-environment.mjs';

const root = dirname(dirname(fileURLToPath(import.meta.url)));
const directory = join(root, 'tmp/l03-app');
const backend = join(root, 'backend');
const jdk = join(root, 'tmp/l02/tooling/jdk-21.0.13+9');
const repository = join(root, 'tmp/l02/m2');
const container = 'authority-pro-postgres';
const admin = 'authority_pro_operator';
const owner = 'authority_pro_owner';
const migrator = 'authority_pro_migrator';
const runtime = 'authority_pro_runtime';
export const TARGETS = Object.freeze(['authority_pro_l03_app_test', 'authority_pro_l03_app_restore']);
const commands = ['provision', 'qualify', 'restore', 'verify-restore'];
const mainJournals = ['main.started', 'main.migrated', 'main.noop', 'main.passed', 'main.failed'];
const recoveryJournals = ['recovery.started', 'recovery.noop', 'recovery.passed', 'recovery.failed'];
const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;
const hashPattern = /^[0-9a-f]{64}$/;
const limit = 8 * 1024 * 1024;
const sha = value => createHash('sha256').update(value).digest('hex');

export function validateCommand(args) {
  assert.ok(Array.isArray(args) && args.length === 1 && commands.includes(args[0]));
  return args[0];
}

export function validateTarget(database, mode) {
  assert.ok(TARGETS.includes(database));
  if (mode !== undefined) {
    assert.ok(['qualify', 'verify-restore'].includes(mode));
    assert.equal(database, TARGETS[mode === 'qualify' ? 0 : 1]);
  }
  return database;
}

export function sanitizedEnvironment(environment) {
  return Object.fromEntries(Object.entries(environment).filter(([key]) =>
    !/^(AUTHORITY_PRO_|SPRING_|SERVER_|MANAGEMENT_|PG|FLYWAY_|JDBC_|HIKARI_|DATABASE_|DB_|COMPOSE_)/i.test(key)
    && !/^(JAVA_HOME|JAVA_TOOL_OPTIONS|JDK_JAVA_OPTIONS|_JAVA_OPTIONS|JAVA_OPTS|CLASSPATH|MAVEN_OPTS|MAVEN_ARGS|NODE_OPTIONS|NODE_PATH)$/i.test(key)));
}

export function validateRelease(text) {
  for (const [key, value] of Object.entries({ IMPLEMENTOR: 'Eclipse Adoptium',
    IMPLEMENTOR_VERSION: 'Temurin-21.0.13+9', JAVA_RUNTIME_VERSION: '21.0.13+9-LTS',
    JAVA_VERSION: '21.0.13', OS_NAME: 'Windows', OS_ARCH: 'x86_64', IMAGE_TYPE: 'JDK' })) {
    const matches = [...text.matchAll(new RegExp(`^${key}="([^"]*)"\\r?$`, 'gm'))];
    assert.equal(matches.length, 1);
    assert.equal(matches[0][1], value);
  }
}

// Only explicit, reviewed Maven jars in this checkout's fixed repository may enter -cp.
export function validateClasspath(text, repo = repository) {
  assert.equal(typeof text, 'string');
  const entries = text.trim().split(';');
  assert.ok(entries.length > 0);
  const base = win32.resolve(repo).toLowerCase();
  const paths = entries.map(entry => {
    assert.ok(/^[A-Za-z]:[\\/]/.test(entry));
    assert.ok(!/[\x00-\x1f"*?<>|]/.test(entry));
    assert.ok(!entry.slice(2).includes(':'));
    const path = win32.normalize(entry);
    assert.ok(path.toLowerCase().startsWith(`${base}\\`));
    assert.ok(path.endsWith('.jar'));
    return path;
  });
  assert.equal(new Set(paths.map(path => path.toLowerCase())).size, paths.length);
  return paths;
}

function attributes(text) {
  const result = {};
  let remainder = text;
  const expression = /\s+([\w:.-]+)="([^"<>]*)"/g;
  for (const match of text.matchAll(expression)) {
    assert.ok(!Object.hasOwn(result, match[1]));
    result[match[1]] = match[2];
    remainder = remainder.replace(match[0], '');
  }
  assert.equal(remainder.trim(), '');
  return result;
}

function xmlRoot(text, name) {
  assert.equal(typeof text, 'string');
  assert.ok(!/<!DOCTYPE|<!ENTITY|<!--/i.test(text));
  const clean = text.replace(/^\s*<\?xml[^?]*\?>/, '').trim();
  const match = clean.match(new RegExp(`^<${name}(\\s[^>]*?)>([\\s\\S]*)<\\/${name}>$`));
  assert.ok(match);
  assert.equal((clean.match(new RegExp(`<${name}(?:\\s|>)`, 'g')) ?? []).length, 1);
  return { attrs: attributes(match[1]), body: match[2] };
}

function natural(value) {
  assert.match(value ?? '', /^(0|[1-9][0-9]*)$/);
  const number = Number(value);
  assert.ok(Number.isSafeInteger(number));
  return number;
}

export function validateSuiteXml(text) {
  const { attrs, body } = xmlRoot(text, 'testsuite');
  const tests = natural(attrs.tests);
  assert.ok(tests > 0);
  for (const key of ['errors', 'failures', 'skipped']) assert.equal(natural(attrs[key]), 0);
  if (attrs.flakes !== undefined) assert.equal(natural(attrs.flakes), 0);
  assert.ok(!/<(?:failure|error|skipped|flakyFailure|flakyError|rerunFailure|rerunError)(?:\s|\/?>)/.test(body));
  assert.equal((body.match(/<testcase(?:\s|>)/g) ?? []).length, tests);
  assert.match(attrs.name ?? '', /^com\.authoritypro\.[A-Za-z0-9_.$]+$/);
  return { name: attrs.name, tests };
}

export function validateFailsafeXml(text) {
  const { attrs, body } = xmlRoot(text, 'failsafe-summary');
  assert.ok(['null', '0'].includes(attrs.result));
  assert.equal(attrs.timeout, 'false');
  const counts = {};
  let remainder = body;
  for (const key of ['completed', 'errors', 'failures', 'skipped', 'flakes']) {
    const matches = [...body.matchAll(new RegExp(`<${key}>([0-9]+)<\\/${key}>`, 'g'))];
    assert.equal(matches.length, 1);
    counts[key] = natural(matches[0][1]);
    remainder = remainder.replace(matches[0][0], '');
  }
  assert.ok(counts.completed > 0);
  for (const key of ['errors', 'failures', 'skipped', 'flakes']) assert.equal(counts[key], 0);
  const message = remainder.trim();
  assert.match(message, /^<failureMessage\s[^<>]*\/>$/);
  assert.equal(attributes(message.slice('<failureMessage'.length, -2))['xsi:nil'], 'true');
  return counts.completed;
}

export function validatePhase(command, records) {
  validateCommand([command]);
  const index = commands.indexOf(command);
  if (command === 'provision') assert.equal(records.state, undefined);
  else assert.ok(records.state);
  for (let i = 0; i < commands.length; i++) {
    const name = commands[i];
    if (i < index) {
      assert.equal(records[name]?.status, 'passed');
      assert.equal(records[name]?.id, records.state.id);
      assert.equal(records[`${name}.started`]?.id, records.state.id);
      assert.equal(records[`${name}.failed`], undefined);
    } else {
      assert.equal(records[name], undefined);
      assert.equal(records[`${name}.started`], undefined);
      assert.equal(records[`${name}.failed`], undefined);
    }
  }
}

export function validateBinding(state, rows) {
  assert.equal(state.version, 1);
  assert.match(state.id, uuid);
  assert.deepEqual(state.databases, TARGETS);
  assert.equal(rows.length, 2);
  assert.deepEqual(Object.keys(state.oids).sort(), [...TARGETS].sort());
  for (const name of TARGETS) {
    assert.match(state.oids[name], /^[1-9][0-9]*$/);
    const row = rows.find(item => item.name === name);
    assert.deepEqual(row, { name, oid: state.oids[name], owner, marker: state.id });
  }
  assert.notEqual(state.oids[TARGETS[0]], state.oids[TARGETS[1]]);
}

export function validatePreservation(before, after) { assert.deepEqual(after, before); }

export function validateMemory(bytes) {
  assert.ok(Number.isFinite(bytes) && bytes >= 640 * 1024 * 1024);
}

export function validateArtifactReceipt(receipt, digest, receiptTime, latestInput, summaryTime) {
  assert.match(receipt, /^[0-9a-f]{64}\r?\n$/);
  assert.equal(receipt.trim(), digest);
  assert.ok([receiptTime, latestInput, summaryTime].every(Number.isFinite));
  assert.ok(receiptTime >= latestInput && summaryTime >= receiptTime);
}

export function validateRolePolicy(flags, memberships) {
  assert.deepEqual(flags, [migrator, owner, runtime].map(rolname => ({ rolname, rolcanlogin: rolname !== owner,
    rolsuper: false, rolcreatedb: false, rolcreaterole: false, rolbypassrls: false, rolreplication: false,
    rolinherit: false, rolconnlimit: rolname === migrator ? 2 : rolname === runtime ? 5 : -1 })));
  assert.deepEqual(memberships, [{ parent: owner, member: migrator, admin_option: false, inherit_option: false, set_option: true }]);
}

export function validateJavaOutput(output, database, mode) {
  validateTarget(database, mode);
  const lines = output.trim().split(/\r?\n/);
  assert.ok(lines.length > 1);
  const passes = lines.slice(0, -1);
  for (const line of passes) assert.match(line, /^PASS [a-z][a-z0-9_]*$/);
  assert.equal(new Set(passes).size, passes.length);
  assert.equal(lines.at(-1), `DATA_QUALIFICATION_OK checks=${passes.length} database=${database}`);
  for (const check of ['input_contract', 'effective_owner_and_fixed_endpoint', 'single_successful_history',
    'production_runtime_validation', 'final_seed_a', 'final_seed_b', 'final_migration_validation',
    mode === 'qualify' ? 'flyway_first_migration' : 'restore_validate_without_migrate']) {
    assert.ok(passes.includes(`PASS ${check}`));
  }
  return lines;
}

export function validateDependencyReview(review, dependencies) {
  assert.equal(review.source, 'https://api.osv.dev/v1/querybatch');
  assert.ok(Number.isFinite(Date.parse(review.checkedAt)));
  assert.deepEqual(review.findings, []);
  assert.ok(Array.isArray(review.inventory) && review.inventory.length === dependencies.length);
  const keys = new Set();
  for (const item of review.inventory) {
    for (const key of ['group', 'artifact', 'version']) assert.match(item[key], /^[A-Za-z0-9_.+-]+$/);
    assert.match(item.sha256, hashPattern);
    assert.ok(Array.isArray(item.licenses) && item.licenses.length > 0 && item.licenses.every(x => typeof x === 'string' && x.length > 0));
    const suffix = `${item.group.replaceAll('.', '/')}/${item.artifact}/${item.version}/${item.artifact}-${item.version}.jar`;
    assert.ok(!keys.has(suffix));
    keys.add(suffix);
    const matches = dependencies.filter(dep => dep.path.replaceAll('\\', '/').endsWith(`/${suffix}`));
    assert.equal(matches.length, 1);
    assert.equal(matches[0].sha256, item.sha256);
  }
}

export function validateMainRequest(args, recorded, attempts) {
  assert.deepEqual(args, ['migrate-main']);
  assert.deepEqual(attempts, []);
  assert.equal(recorded.state?.version, 1);
  assert.match(recorded.state.id, uuid);
  assert.deepEqual(recorded.state.databases, TARGETS);
  for (const command of commands) {
    const report = recorded[command];
    assert.equal(report?.id, recorded.state.id);
    assert.equal(report.status, 'passed');
    assert.equal(report.command, command);
    assert.equal(recorded[`${command}.started`]?.id, recorded.state.id);
    assert.equal(recorded[`${command}.started`]?.status, 'started');
    assert.equal(recorded[`${command}.failed`], undefined);
    for (const key of ['protectedContainersUnchanged', 'mainDatabaseRemainsEmpty',
      'untouchedDatabaseIdentitiesUnchanged', 'artifactsUnchanged']) assert.equal(report[key], true);
  }
  assert.equal(recorded.provision.binding.id, recorded.state.id);
  validateJavaOutput(recorded.qualify.checks.join('\n'), TARGETS[0], 'qualify');
  validateJavaOutput(recorded['verify-restore'].checks.join('\n'), TARGETS[1], 'verify-restore');
}

export function validateSuccessorBuild(previous, current) {
  assert.deepEqual(current.classpath, previous.classpath);
  assert.deepEqual(current.tests, previous.tests);
  assert.deepEqual(Object.keys(current.hashes).sort(), Object.keys(previous.hashes).sort());
  const renewed = /^(?:tools\/local-data(?:\.test)?\.mjs|backend\/target\/(?:surefire|failsafe)-reports\/(?:TEST-[^/]+\.xml|failsafe-summary\.xml))$/;
  for (const [path, hash] of Object.entries(previous.hashes)) {
    assert.match(hash, hashPattern);
    assert.match(current.hashes[path], hashPattern);
    if (!renewed.test(path)) assert.equal(current.hashes[path], hash, 'Qualified input changed');
  }
}

export function validateRecoveryRequest(args, recorded, started, failed, digests, attempts) {
  assert.deepEqual(args, ['recover-main']);
  assert.deepEqual(attempts, []);
  validateMainRequest(['migrate-main'], recorded, []);
  assert.equal(started.id, '356dcc34-3a3a-4227-ba84-630691755f8c');
  assert.equal(started.qualificationId, 'd1af7045-a3f7-4042-b750-1839aedb32e8');
  assert.equal(started.qualificationId, recorded.state.id);
  assert.equal(started.status, 'started');
  assert.deepEqual(started.before, recorded.state.before);
  assert.deepEqual(failed, { id: started.id, status: 'failed', checkedAt: '2026-11-20T11:52:37.323Z',
    mutationStarted: true, preservationVerified: false, resourcesRetained: true, rawOutputWithheld: true });
  assert.deepEqual(digests, {
    'main.started': '5991a1d16cdaaf0a203aa10c5c6dfb556554200773bc02535a0b953be2b5e227',
    'main.failed': '6edf3205e83a194ce82c079693fd47630e3609babc9d629d15992726a5b05a5a',
  });
}

export function recoveryCatalog(qualified) {
  assert.equal(qualified.length, 9);
  assert.deepEqual(qualified[6], []);
  assert.deepEqual(qualified[8], [{ version: '1', type: 'SQL', script: 'V1__tenancy_foundation.sql',
    checksum: -1937331583, success: true }]);
  const expected = structuredClone(qualified);
  expected[6] = [
    { owner, nspname: null, defaclobjtype: 'T', acl: `{${owner}=U/${owner}}` },
    { owner, nspname: null, defaclobjtype: 'f', acl: `{${owner}=X/${owner}}` },
  ];
  return expected;
}

export function validateTypePrivileges(rows) {
  const types = [['public', '_flyway_schema_history'], ['public', 'flyway_schema_history'],
    ...['_companies', '_memberships', '_sites', '_tenants', 'companies', 'memberships', 'sites', 'tenants']
      .map(name => ['tenancy', name])];
  assert.deepEqual(rows, types.map(([nspname, typname]) => ({ nspname, typname, owner, acl: null,
    runtime_usage: true })));
}

export function validateColumnPrivileges(rows) {
  assert.deepEqual(rows, []);
}

export function validatePackagedResult(result, expected, exitCode = 0) {
  assert.ok(!result.error && result.status === exitCode && result.signal === null);
  assert.equal(Buffer.concat([result.stdout, result.stderr]).toString('utf8').trim(), expected);
}

export function validateListener(rows, pid, earliestStart, expectedStart) {
  assert.ok(Number.isInteger(pid) && pid > 0);
  assert.equal(rows.length, 1);
  assert.equal(rows[0].address, '127.0.0.1');
  assert.equal(rows[0].pid, pid);
  const start = Date.parse(rows[0].startedAt);
  assert.ok(Number.isFinite(start) && start >= earliestStart - 1000 && start <= Date.now());
  if (expectedStart !== undefined) assert.equal(rows[0].startedAt, expectedStart);
  return rows[0].startedAt;
}

export function validateHttpProbe(method, path, status, headers, body) {
  if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) {
    const cookies = headers['set-cookie'];
    assert.ok(Array.isArray(cookies) && cookies.length === 1
      && /^XSRF-TOKEN=[0-9a-f-]{36}; Path=\/; HttpOnly; SameSite=Strict$/.test(cookies[0]), 'Unexpected CSRF cookie policy');
  } else assert.ok(headers['set-cookie'] === undefined, 'Unexpected response cookie');
  assert.equal(headers['www-authenticate'], undefined);
  assert.equal(headers.location, undefined);
  if (method === 'GET' && path === '/actuator/health/liveness') {
    assert.equal(status, 200);
    assert.equal(body, '{"status":"UP"}');
  } else if (method === 'GET' && path === '/actuator/health/readiness') {
    assert.equal(status, 503);
    assert.equal(body, '{"status":"OUT_OF_SERVICE"}');
  } else {
    assert.equal(status, 403);
    if (method === 'HEAD') assert.equal(body, '');
    else {
      assert.match(headers['content-type'] ?? '', /^application\/problem\+json/);
      const problem = JSON.parse(body);
      assert.equal(problem.status, 403);
      assert.match(problem.traceId, /^[0-9a-f]{32}$/);
      assert.equal(problem.code, ['POST', 'PUT', 'PATCH', 'DELETE'].includes(method) ? 'CSRF_REJECTED' : 'ACCESS_DENIED');
    }
  }
}

export function validateOperator(rows) {
  assert.deepEqual(rows, [{ role: admin, superuser: true }]);
}

export function validateMigratedCatalog(actual, expected, rows) {
  assert.deepEqual(actual, expected);
  assert.deepEqual(rows, [{ tenants: 0, companies: 0, sites: 0, memberships: 0 }]);
}

function regular(path) {
  const stat = lstatSync(path);
  assert.ok(stat.isFile() && !stat.isSymbolicLink());
  assert.equal(resolve(realpathSync(path)).toLowerCase(), resolve(path).toLowerCase());
  return stat;
}
function read(path) { regular(path); return readFileSync(path); }
function tree(path) {
  assert.ok(lstatSync(path).isDirectory() && !lstatSync(path).isSymbolicLink());
  assert.equal(resolve(realpathSync(path)).toLowerCase(), resolve(path).toLowerCase());
  return readdirSync(path, { withFileTypes: true }).sort((a, b) => a.name.localeCompare(b.name)).flatMap(entry => {
    assert.ok(!entry.isSymbolicLink());
    const child = join(path, entry.name);
    if (entry.isDirectory()) return tree(child);
    regular(child);
    return [child];
  });
}

function evidence() {
  const ignoreRules = read(join(root, '.gitignore')).toString('utf8').split(/\r?\n/).map(line => line.trim());
  assert.ok(ignoreRules.includes('tmp/') || ignoreRules.includes('/tmp/'));
  assert.ok(!ignoreRules.some(line => line.startsWith('!') && /tmp|l03-app/.test(line)));
  validateRelease(read(join(jdk, 'release')).toString('utf8'));
  const classpath = validateClasspath(read(join(directory, 'classpath.txt')).toString('utf8'));
  const sources = [join(backend, 'pom.xml'), ...tree(join(backend, 'src/main')), ...tree(join(backend, 'src/test'))];
  const tools = ['package.json', 'tools/local-data.mjs', 'tools/local-data.test.mjs',
    'tools/local-environment.mjs', 'tools/local-environment.test.mjs',
    'tools/check-backend.mjs', 'tools/local-postgres-init.sql', 'compose.yaml', '.gitignore',
    'backend/.mvn/settings.xml', 'backend/.mvn/wrapper/maven-wrapper.properties', 'backend/mvnw.cmd'].map(name => join(root, name));
  const classes = [...tree(join(backend, 'target/classes')), ...tree(join(backend, 'target/test-classes'))];
  assert.ok(classes.some(path => path.endsWith('DatabaseQualification.class')));
  for (const source of sources) {
    const rel = relative(join(backend, 'src'), source).replaceAll('\\', '/');
    const match = rel.match(/^(main|test)\/(java|resources)\/(.+)$/);
    if (!match) continue;
    const destination = join(backend, match[1] === 'main' ? 'target/classes' : 'target/test-classes',
      match[2] === 'java' ? match[3].replace(/\.java$/, '.class') : match[3]);
    assert.ok(regular(destination).mtimeMs >= regular(source).mtimeMs);
    if (match[2] === 'resources') assert.equal(sha(read(destination)), sha(read(source)));
  }
  const jar = join(backend, 'target/authority-pro-backend-0.0.1-SNAPSHOT.jar');
  assert.ok(regular(jar).size > 0);
  const receipt = join(backend, 'target/packaged-artifact.sha256');
  const reports = {};
  const xmlPaths = [];
  const latestInput = Math.max(...[...sources, ...tools].map(path => regular(path).mtimeMs));
  for (const kind of ['surefire', 'failsafe']) {
    const files = tree(join(backend, `target/${kind}-reports`)).filter(path => /[\\/]TEST-[^\\/]+\.xml$/.test(path));
    assert.ok(files.length > 0);
    reports[kind] = files.map(path => {
      assert.ok(regular(path).mtimeMs >= latestInput, 'Fresh clean verify is required after source/tool edits');
      const suite = validateSuiteXml(read(path).toString('utf8'));
      assert.equal(path, join(backend, `target/${kind}-reports/TEST-${suite.name}.xml`));
      xmlPaths.push(path);
      return suite;
    });
    assert.equal(new Set(reports[kind].map(suite => suite.name)).size, files.length);
  }
  // Require a report for every ordinary discovered project test, not just an old passing subset.
  for (const path of sources.filter(path => /[\\/]src[\\/]test[\\/]java[\\/].*(?:Test|IT)\.java$/.test(path))) {
    const name = relative(join(backend, 'src/test/java'), path).replaceAll('\\', '/').replace(/\.java$/, '').replaceAll('/', '.');
    assert.ok([...reports.surefire, ...reports.failsafe].some(suite => suite.name === name));
  }
  const summary = join(backend, 'target/failsafe-reports/failsafe-summary.xml');
  assert.ok(regular(summary).mtimeMs >= Math.max(latestInput, regular(jar).mtimeMs, ...xmlPaths.map(path => regular(path).mtimeMs)));
  const completed = validateFailsafeXml(read(summary).toString('utf8'));
  assert.equal(completed, reports.failsafe.reduce((sum, suite) => sum + suite.tests, 0));
  validateArtifactReceipt(read(receipt).toString('utf8'), sha(read(jar)), regular(receipt).mtimeMs,
    Math.max(latestInput, ...classes.map(path => regular(path).mtimeMs)), regular(summary).mtimeMs);
  const dependencies = classpath.map(path => ({ path, sha256: sha(read(path)) }));
  const reviewPath = join(directory, 'dependency-review.json');
  validateDependencyReview(JSON.parse(read(reviewPath)), dependencies);
  const paths = [...sources, ...tools, ...classes, ...xmlPaths, summary, jar, receipt,
    join(backend, 'target/dependencies.txt'), join(directory, 'classpath.txt'), reviewPath,
    join(jdk, 'release'), join(jdk, 'bin/java.exe'), ...classpath];
  const hashes = Object.fromEntries([...new Set(paths)].sort().map(path => [relative(root, path).replaceAll('\\', '/'), sha(read(path))]));
  return { hashes, tests: reports, classpath };
}

function processResult(command, args, input, timeout = 30000) {
  const result = spawnSync(command, args, { cwd: directory, env: sanitizedEnvironment(process.env),
    input, windowsHide: true, timeout, maxBuffer: limit });
  assert.ok(!result.error && result.status === 0 && result.signal === null, 'Child failed; raw output withheld');
  return result.stdout;
}

function internal(passwords, operation, body = Buffer.alloc(0)) {
  const specs = {
    identity: ['psql', '--dbname=postgres', '--no-psqlrc', '--quiet', '--tuples-only', '--no-align',
      '--set=ON_ERROR_STOP=1', '--command=SELECT system_identifier FROM pg_control_system();'],
    dump: ['pg_dump', `--dbname=${TARGETS[0]}`, '--format=custom'],
    restore: ['pg_restore', `--dbname=${TARGETS[1]}`, '--single-transaction', '--exit-on-error'],
  };
  assert.ok(Object.hasOwn(specs, operation));
  const input = Buffer.concat([Buffer.from(`${passwords[admin]}\n`), body]);
  try {
    return processResult('docker', ['exec', '-i', container, 'env', '-i',
      'PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
      'PGPASSFILE=/nonexistent-authority-pro-password-file', 'PGCONNECT_TIMEOUT=5',
      'PGOPTIONS=-c statement_timeout=60000 -c lock_timeout=2000',
      'sh', '-c', 'IFS= read -r PGPASSWORD; export PGPASSWORD; exec "$@"', 'sh',
      ...specs[operation], '--host=127.0.0.1', '--port=5432', `--username=${admin}`, '--no-password'], input, 90000);
  } finally { input.fill(0); }
}

async function query(passwords, database, text, values = [], write = false) {
  assert.ok(['postgres', 'authority_pro', ...TARGETS].includes(database));
  if (write) assert.ok(database === 'postgres' || TARGETS.includes(database));
  const client = new pg.Client({ host: '127.0.0.1', port: 5434, user: admin, password: passwords[admin], database,
    ssl: false, application_name: 'authority-pro-local-data-guard', connectionTimeoutMillis: 5000,
    query_timeout: 10000, options: `-c statement_timeout=10000 -c lock_timeout=2000 -c search_path=pg_catalog -c default_transaction_read_only=${write ? 'off' : 'on'}` });
  // Idle socket failures must never become uncaught secret-bearing diagnostics.
  let socketFailure = false;
  client.on('error', () => { socketFailure = true; });
  try {
    await client.connect();
    const result = await client.query(text, values);
    assert.equal(socketFailure, false);
    return result.rows;
  } finally { await client.end(); }
}

const userNamespace = "n.nspname !~ '^pg_' AND n.nspname <> 'information_schema'";
async function emptyCatalog(passwords, database) {
  assert.ok(database === 'authority_pro' || TARGETS.includes(database));
  const schemas = await query(passwords, database, `SELECT n.nspname,pg_get_userbyid(n.nspowner) AS owner FROM pg_namespace n WHERE ${userNamespace} ORDER BY 1`);
  assert.deepEqual(schemas, [{ nspname: 'public', owner }]);
  for (const sql of [
    `SELECT count(*)::int AS count FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE ${userNamespace}`,
    `SELECT count(*)::int AS count FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace WHERE ${userNamespace}`,
    `SELECT count(*)::int AS count FROM pg_type t JOIN pg_namespace n ON n.oid=t.typnamespace WHERE ${userNamespace}`,
    "SELECT count(*)::int AS count FROM pg_extension WHERE extname <> 'plpgsql'",
    'SELECT count(*)::int AS count FROM pg_largeobject_metadata',
    'SELECT count(*)::int AS count FROM pg_event_trigger',
    'SELECT count(*)::int AS count FROM pg_foreign_data_wrapper',
    'SELECT count(*)::int AS count FROM pg_foreign_server',
    'SELECT count(*)::int AS count FROM pg_publication',
    'SELECT count(*)::int AS count FROM pg_subscription',
  ]) assert.deepEqual(await query(passwords, database, sql), [{ count: 0 }]);
  const catalog = [];
  for (const sql of [
    `SELECT to_jsonb(n) AS value FROM pg_namespace n WHERE ${userNamespace} ORDER BY n.oid`,
    'SELECT to_jsonb(d) AS value FROM pg_default_acl d ORDER BY d.oid',
    'SELECT to_jsonb(e) AS value FROM pg_extension e ORDER BY e.oid',
    "SELECT classoid::text,objoid::text,objsubid,description FROM pg_description WHERE classoid IN ('pg_namespace'::regclass,'pg_extension'::regclass) ORDER BY 1,2,3",
  ]) catalog.push(await query(passwords, database, sql));
  return sha(JSON.stringify(catalog));
}

async function schemaSnapshot(passwords, database) {
  assert.ok(database === 'authority_pro' || TARGETS.includes(database));
  const statements = [
    `SELECT n.nspname,pg_get_userbyid(n.nspowner) AS owner,n.nspacl::text AS acl FROM pg_namespace n WHERE ${userNamespace} ORDER BY 1`,
    `SELECT n.nspname,c.relname,c.relkind,c.relpersistence,c.relrowsecurity,c.relforcerowsecurity,pg_get_userbyid(c.relowner) AS owner,c.relacl::text AS acl FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE ${userNamespace} ORDER BY 1,2`,
    `SELECT n.nspname,c.relname,a.attnum,a.attname,format_type(a.atttypid,a.atttypmod) AS type,a.attnotnull,a.attidentity,a.attgenerated,pg_get_expr(d.adbin,d.adrelid) AS default_value FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid JOIN pg_namespace n ON n.oid=c.relnamespace LEFT JOIN pg_attrdef d ON d.adrelid=c.oid AND d.adnum=a.attnum WHERE ${userNamespace} AND a.attnum>0 AND NOT a.attisdropped ORDER BY 1,2,3`,
    `SELECT n.nspname,c.relname,k.conname,k.contype,k.convalidated,pg_get_constraintdef(k.oid) AS definition FROM pg_constraint k JOIN pg_class c ON c.oid=k.conrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE ${userNamespace} ORDER BY 1,2,3`,
    `SELECT n.nspname,c.relname,pg_get_indexdef(i.indexrelid) AS definition,i.indisvalid,i.indisready FROM pg_index i JOIN pg_class c ON c.oid=i.indrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE ${userNamespace} ORDER BY 1,2,3`,
    `SELECT n.nspname,c.relname,p.polname,p.polcmd,p.polpermissive,ARRAY(SELECT CASE WHEN x=0 THEN 'PUBLIC' ELSE pg_get_userbyid(x) END FROM unnest(p.polroles) x ORDER BY 1) AS roles,pg_get_expr(p.polqual,p.polrelid) AS using_expression,pg_get_expr(p.polwithcheck,p.polrelid) AS check_expression FROM pg_policy p JOIN pg_class c ON c.oid=p.polrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE ${userNamespace} ORDER BY 1,2,3`,
    `SELECT pg_get_userbyid(d.defaclrole) AS owner,n.nspname,d.defaclobjtype,d.defaclacl::text AS acl FROM pg_default_acl d LEFT JOIN pg_namespace n ON n.oid=d.defaclnamespace ORDER BY 1,2,3`,
    `SELECT n.nspname,t.typname,t.typtype,format_type(t.typbasetype,t.typtypmod) AS base_type FROM pg_type t JOIN pg_namespace n ON n.oid=t.typnamespace WHERE ${userNamespace} ORDER BY 1,2`,
    "SELECT version,type,script,checksum,success FROM public.flyway_schema_history ORDER BY installed_rank",
  ];
  const result = [];
  for (const sql of statements) result.push(await query(passwords, database, sql));
  assert.deepEqual(result[0].map(row => row.nspname), ['public', 'tenancy']);
  assert.ok(result[0].every(row => row.owner === owner));
  assert.deepEqual(result[1].filter(row => row.relkind === 'r').map(row => `${row.nspname}.${row.relname}`),
    ['public.flyway_schema_history', 'tenancy.companies', 'tenancy.memberships', 'tenancy.sites', 'tenancy.tenants']);
  assert.ok(result[1].filter(row => row.nspname === 'tenancy' && row.relkind === 'r')
    .every(row => row.owner === owner && row.relrowsecurity && row.relforcerowsecurity));
  const history = result.at(-1);
  assert.equal(history.length, 1);
  assert.deepEqual(history[0], { version: '1', type: 'SQL', script: 'V1__tenancy_foundation.sql',
    checksum: history[0].checksum, success: true });
  assert.ok(Number.isInteger(history[0].checksum));
  for (const sql of [
    `SELECT count(*)::int AS count FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace WHERE ${userNamespace}`,
    `SELECT count(*)::int AS count FROM pg_trigger t JOIN pg_class c ON c.oid=t.tgrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE ${userNamespace} AND NOT t.tgisinternal`,
    "SELECT count(*)::int AS count FROM pg_extension WHERE extname <> 'plpgsql'",
    'SELECT count(*)::int AS count FROM pg_largeobject_metadata',
    'SELECT count(*)::int AS count FROM pg_event_trigger',
    'SELECT count(*)::int AS count FROM pg_foreign_data_wrapper',
    'SELECT count(*)::int AS count FROM pg_foreign_server',
    'SELECT count(*)::int AS count FROM pg_publication',
    'SELECT count(*)::int AS count FROM pg_subscription',
  ]) assert.deepEqual(await query(passwords, database, sql), [{ count: 0 }]);
  return result;
}

async function migratedCatalog(passwords, expected) {
  const actual = await schemaSnapshot(passwords, 'authority_pro');
  // Operator counts are not filtered by tenant RLS: main must contain no fixtures.
  const rows = await query(passwords, 'authority_pro', 'SELECT (SELECT count(*)::int FROM tenancy.tenants) AS tenants,(SELECT count(*)::int FROM tenancy.companies) AS companies,(SELECT count(*)::int FROM tenancy.sites) AS sites,(SELECT count(*)::int FROM tenancy.memberships) AS memberships');
  validateMigratedCatalog(actual, expected, rows);
  const history = await query(passwords, 'authority_pro', 'SELECT to_jsonb(h) AS value FROM public.flyway_schema_history h ORDER BY installed_rank');
  return sha(JSON.stringify({ actual, rows, history }));
}

async function guard(expectedMain) {
  assert.equal(process.platform, 'win32');
  validateMemory(freemem());
  const protectedContainers = protectedContainerSnapshot();
  const { state, passwords } = accessLocalEnvironment();
  for (const role of [admin, migrator, runtime]) assert.match(passwords[role], /^[A-Za-z0-9_-]{32,256}$/);
  validateOperator(await query(passwords, 'postgres', 'SELECT current_user AS role,rolsuper AS superuser FROM pg_roles WHERE rolname=current_user'));
  const id = (await query(passwords, 'postgres', 'SELECT system_identifier::text AS id FROM pg_control_system()'))[0].id;
  assert.match(id, /^[0-9]+$/);
  assert.equal(internal(passwords, 'identity').toString('utf8').trim(), id);
  assert.deepEqual(await query(passwords, 'postgres', 'SHOW server_version_num'), [{ server_version_num: '160015' }]);
  const flags = await query(passwords, 'postgres', `SELECT rolname,rolcanlogin,rolsuper,rolcreatedb,rolcreaterole,rolbypassrls,rolreplication,rolinherit,rolconnlimit FROM pg_roles WHERE rolname=ANY($1::text[]) ORDER BY rolname`, [[owner, migrator, runtime]]);
  const memberships = await query(passwords, 'postgres', `SELECT parent.rolname AS parent,member.rolname AS member,m.admin_option,m.inherit_option,m.set_option FROM pg_auth_members m JOIN pg_roles parent ON parent.oid=m.roleid JOIN pg_roles member ON member.oid=m.member WHERE member.rolname=ANY($1::text[]) OR parent.rolname=ANY($1::text[]) ORDER BY 1,2`, [[owner, migrator, runtime]]);
  validateRolePolicy(flags, memberships);
  assert.deepEqual(await query(passwords, 'postgres', "SELECT count(*)::int AS count FROM pg_hba_file_rules WHERE error IS NOT NULL OR auth_method <> 'scram-sha-256'"), [{ count: 0 }]);
  assert.deepEqual(await query(passwords, 'postgres', "SELECT count(*)::int AS count FROM pg_database d CROSS JOIN LATERAL aclexplode(COALESCE(d.datacl,acldefault('d',d.datdba))) a WHERE d.datallowconn AND a.grantee=0 AND a.privilege_type IN ('CONNECT','CREATE','TEMPORARY')"), [{ count: 0 }]);
  // This includes both old scratch databases, without ever opening a connection to either.
  const untouchedDatabases = await query(passwords, 'postgres', `SELECT d.oid::text AS oid,d.datname,pg_get_userbyid(d.datdba) AS owner,d.datacl::text,d.datallowconn,d.datconnlimit,d.encoding,d.datcollate,d.datctype,shobj_description(d.oid,'pg_database') AS marker FROM pg_database d WHERE NOT (d.datname=ANY($1::text[])) ORDER BY d.datname`, [TARGETS]);
  assert.equal(untouchedDatabases.find(row => row.datname === 'authority_pro')?.owner, owner);
  const mainCatalog = expectedMain === undefined ? await emptyCatalog(passwords, 'authority_pro')
    : await migratedCatalog(passwords, expectedMain);
  await databasePrivileges(passwords, 'authority_pro');
  validatePreservation(protectedContainers, protectedContainerSnapshot());
  return { passwords, snapshot: { environmentId: state.id, containerId: state.containerId, daemon: state.daemon,
    systemIdentifier: id, protectedContainers, untouchedDatabases, mainCatalog } };
}

async function destinations(passwords, state) {
  const rows = await query(passwords, 'postgres', "SELECT datname AS name,oid::text AS oid,pg_get_userbyid(datdba) AS owner,shobj_description(oid,'pg_database') AS marker FROM pg_database WHERE datname=ANY($1::text[]) ORDER BY datname", [TARGETS]);
  validateBinding(state, rows);
  for (const name of TARGETS) await databasePrivileges(passwords, name);
}

async function databasePrivileges(passwords, name) {
  assert.ok(name === 'authority_pro' || TARGETS.includes(name));
  const db = await query(passwords, 'postgres', 'SELECT datallowconn,datconnlimit FROM pg_database WHERE datname=$1', [name]);
  assert.deepEqual(db, [{ datallowconn: true, datconnlimit: name === 'authority_pro' ? 12 : 8 }]);
  const acl = await query(passwords, name, `SELECT r.rolname,has_database_privilege(r.oid,current_database(),'CONNECT') AS connect,has_database_privilege(r.oid,current_database(),'CREATE') AS create,has_database_privilege(r.oid,current_database(),'TEMP') AS temp,has_schema_privilege(r.oid,'public','USAGE') AS usage,has_schema_privilege(r.oid,'public','CREATE') AS schema_create FROM pg_roles r WHERE r.rolname=ANY($1::text[]) ORDER BY r.rolname`, [[migrator, runtime]]);
  assert.deepEqual(acl, [migrator, runtime].map(rolname => ({ rolname, connect: true, create: false, temp: false, usage: true, schema_create: false })));
  assert.deepEqual(await query(passwords, name, "SELECT pg_get_userbyid(nspowner) AS owner FROM pg_namespace WHERE nspname='public'"), [{ owner }]);
  assert.deepEqual(await query(passwords, name, "SELECT count(*)::int AS count FROM pg_namespace n CROSS JOIN LATERAL aclexplode(COALESCE(n.nspacl,acldefault('n',n.nspowner))) a WHERE n.nspname='public' AND a.grantee=0"), [{ count: 0 }]);
}

function journal(name, data) {
  assert.ok(['state', 'database-test', 'database-restore', ...mainJournals, ...recoveryJournals,
    ...commands.flatMap(command => [command, `${command}.started`, `${command}.failed`])].includes(name));
  assert.ok(lstatSync(directory).isDirectory() && !lstatSync(directory).isSymbolicLink());
  assert.equal(resolve(realpathSync(directory)).toLowerCase(), resolve(directory).toLowerCase());
  writeFileSync(join(directory, `${name}.json`), `${JSON.stringify(data, null, 2)}\n`, { flag: 'wx', mode: 0o600 });
}
function records() {
  const names = ['state', ...commands.flatMap(command => [command, `${command}.started`, `${command}.failed`])];
  return Object.fromEntries(names.filter(name => existsSync(join(directory, `${name}.json`)))
    .map(name => [name, JSON.parse(read(join(directory, `${name}.json`)))]));
}
function java(passwords, build, mode) {
  const database = validateTarget(TARGETS[mode === 'qualify' ? 0 : 1], mode);
  const input = Buffer.from(`${passwords[migrator]}\n${passwords[runtime]}\n${database}\n`);
  try {
    const output = processResult(join(jdk, 'bin/java.exe'), ['-Xmx128m', '-XX:MaxMetaspaceSize=128m',
      '-Djava.awt.headless=true', '-XX:-CreateCoredumpOnCrash', '-XX:ErrorFile=NUL',
      '-cp', [join(backend, 'target/classes'), join(backend, 'target/test-classes'), ...build.classpath].join(';'),
      'com.authoritypro.persistence.DatabaseQualification', mode], input, 180000).toString('utf8');
    return validateJavaOutput(output, database, mode);
  } finally { input.fill(0); }
}

async function execute(command) {
  const recorded = records();
  validatePhase(command, recorded);
  const build = evidence();
  processResult(process.execPath, ['--test', join(root, 'tools/local-data.test.mjs'),
    join(root, 'tools/local-environment.test.mjs')]);
  const before = await guard();
  let state = recorded.state;
  let binding;
  let claimed = false;
  let result;
  let failure;
  try {
    if (command === 'provision') {
      assert.equal((await query(before.passwords, 'postgres', 'SELECT datname FROM pg_database WHERE datname=ANY($1::text[])', [TARGETS])).length, 0);
      for (const name of ['database-test', 'database-restore']) assert.equal(existsSync(join(directory, `${name}.json`)), false);
      assert.deepEqual(evidence(), build);
      state = { version: 1, id: randomUUID(), databases: TARGETS, root, createdAt: new Date().toISOString(),
        before: before.snapshot, build };
      journal('state', state); // Durable claim BEFORE any CREATE; even a partial attempt is never adopted.
      claimed = true;
      journal('provision.started', { id: state.id, status: 'started' });
      const oids = {};
      for (const name of TARGETS) {
        validateTarget(name);
        await query(before.passwords, 'postgres', `CREATE DATABASE ${name} OWNER ${owner} TEMPLATE template0 ALLOW_CONNECTIONS false CONNECTION LIMIT 8`, [], true);
        const row = (await query(before.passwords, 'postgres', 'SELECT oid::text AS oid FROM pg_database WHERE datname=$1', [name]))[0];
        assert.match(row.oid, /^[1-9][0-9]*$/);
        oids[name] = row.oid;
        journal(name === TARGETS[0] ? 'database-test' : 'database-restore', { id: state.id, name, oid: row.oid, owner });
        await query(before.passwords, 'postgres', `REVOKE ALL ON DATABASE ${name} FROM PUBLIC`, [], true);
        await query(before.passwords, 'postgres', `GRANT CONNECT ON DATABASE ${name} TO ${migrator}, ${runtime}`, [], true);
        await query(before.passwords, 'postgres', `COMMENT ON DATABASE ${name} IS '${state.id}'`, [], true);
        await query(before.passwords, 'postgres', `ALTER DATABASE ${name} ALLOW_CONNECTIONS true`, [], true);
        await query(before.passwords, name, `ALTER SCHEMA public OWNER TO ${owner}; REVOKE ALL ON SCHEMA public FROM PUBLIC; GRANT USAGE ON SCHEMA public TO ${migrator}, ${runtime}`, [], true);
      }
      binding = { version: 1, id: state.id, databases: TARGETS, oids };
      await destinations(before.passwords, binding);
      for (const name of TARGETS) await emptyCatalog(before.passwords, name);
      result = { binding };
    } else {
      assert.equal(state.version, 1);
      assert.equal(state.root, root);
      assert.match(state.id, uuid);
      assert.deepEqual(state.databases, TARGETS);
      validatePreservation(state.before, before.snapshot);
      assert.deepEqual(build, state.build);
      binding = recorded.provision.binding;
      assert.equal(binding.id, state.id);
      await destinations(before.passwords, binding);
      if (command === 'qualify') {
        for (const name of TARGETS) await emptyCatalog(before.passwords, name);
      }
      if (command === 'restore') await emptyCatalog(before.passwords, TARGETS[1]);
      journal(`${command}.started`, { id: state.id, status: 'started', at: new Date().toISOString() });
      claimed = true;
      // Recheck the exact file set immediately before executing any reviewed bytecode or restore.
      assert.deepEqual(evidence(), build);
      if (command === 'restore') {
        const dump = internal(before.passwords, 'dump');
        try {
          assert.ok(dump.length > 5 && dump.length < limit);
          assert.equal(dump.subarray(0, 5).toString('ascii'), 'PGDMP');
          await destinations(before.passwords, binding);
          await emptyCatalog(before.passwords, TARGETS[1]);
          internal(before.passwords, 'restore', dump);
          result = { bytes: dump.length, sha256: sha(dump),
            scope: 'same-cluster technical fixtures only; not disaster recovery or PITR' };
        } finally { dump.fill(0); }
      } else {
        result = { checks: java(before.passwords, build, command) };
      }
    }
  } catch (error) {
    failure = error;
    result = undefined;
  } finally {
    let preserved = false;
    try {
      const after = await guard();
      validatePreservation(before.snapshot, after.snapshot);
      assert.deepEqual(evidence(), build);
      if (binding) await destinations(after.passwords, binding);
      preserved = true;
    } catch (error) { failure ??= error; result = undefined; }
    if (!result && claimed) journal(`${command}.failed`, { id: state.id, status: 'failed', preservationVerified: preserved,
      resourcesRetained: true, rawOutputWithheld: true });
  }
  if (failure) throw failure;
  assert.ok(result, 'Local data phase failed; resources retained');
  const report = { id: state.id, status: 'passed', command, checkedAt: new Date().toISOString(),
    protectedContainersUnchanged: true, mainDatabaseRemainsEmpty: true, untouchedDatabaseIdentitiesUnchanged: true,
    artifactsUnchanged: true, ...result };
  journal(command, report);
  return { command, status: 'passed', report: `tmp/l03-app/${command}.json` };
}

function packagedOptions(mode) {
  assert.ok(['data-local', 'migrate-local'].includes(mode));
  return { cwd: directory, windowsHide: true,
    env: { ...sanitizedEnvironment(process.env), AUTHORITY_PRO_MODE: mode } };
}
function packagedArguments() {
  return ['-Xmx256m', '-XX:MaxMetaspaceSize=128m', '-Djava.awt.headless=true',
    '-XX:-CreateCoredumpOnCrash', '-XX:ErrorFile=NUL', '-jar',
    join(backend, 'target/authority-pro-backend-0.0.1-SNAPSHOT.jar')];
}
function packaged(password, mode, expected, exitCode = 0) {
  const input = Buffer.from(`${password}\n`);
  let result;
  try {
    result = spawnSync(join(jdk, 'bin/java.exe'), packagedArguments(), {
      ...packagedOptions(mode), input, timeout: 90000, maxBuffer: limit });
    validatePackagedResult(result, expected, exitCode);
  } finally {
    input.fill(0);
    result?.stdout?.fill(0);
    result?.stderr?.fill(0);
  }
}

async function reserveMainPort() {
  const server = net.createServer(socket => socket.destroy());
  await new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen({ host: '127.0.0.1', port: 8080, exclusive: true }, resolve);
  });
  return server;
}
function releasePort(server) { return new Promise((resolve, reject) => server.close(error => error ? reject(error) : resolve())); }

function listeners() {
  const script = "$ErrorActionPreference='Stop'; $rows=@(Get-NetTCPConnection -State Listen | Where-Object {$_.LocalPort -eq 8080} | ForEach-Object { $p=Get-Process -Id $_.OwningProcess; [pscustomobject]@{address=$_.LocalAddress;pid=[int]$_.OwningProcess;startedAt=$p.StartTime.ToUniversalTime().ToString('o')} }); ConvertTo-Json -InputObject $rows -Compress";
  return JSON.parse(processResult('powershell.exe', ['-NoLogo', '-NoProfile', '-NonInteractive', '-Command', script], undefined).toString('utf8'));
}
export function requestProbe(method, path) {
  return new Promise((resolve, reject) => {
    const request = http.request({ host: '127.0.0.1', port: 8080, method, path, agent: false }, response => {
      const chunks = [];
      let size = 0;
      response.on('data', chunk => {
        size += chunk.length;
        if (size > 8192) request.destroy(new Error('HTTP_PROBE_TOO_LARGE'));
        else chunks.push(chunk);
      });
      response.on('error', () => reject(new Error('HTTP_PROBE_FAILED')));
      response.on('end', () => resolve({ status: response.statusCode, headers: response.headers,
        body: Buffer.concat(chunks).toString('utf8') }));
    });
    const deadline = setTimeout(() => request.destroy(new Error('HTTP_PROBE_TIMEOUT')), 3000);
    request.once('close', () => clearTimeout(deadline));
    request.setTimeout(3000, () => request.destroy(new Error('HTTP_PROBE_TIMEOUT')));
    request.on('error', () => reject(new Error('HTTP_PROBE_FAILED')));
    request.end();
  });
}

async function packagedHttp(password, build) {
  const started = Date.now();
  const child = spawn(join(jdk, 'bin/java.exe'), packagedArguments(), packagedOptions('data-local'));
  const input = Buffer.from(`${password}\n`);
  const chunks = [];
  let size = 0;
  let failed = false;
  let closed = false;
  const finished = new Promise(resolve => child.once('close', () => { closed = true; resolve(); }));
  child.on('error', () => { failed = true; });
  child.stdin.on('error', () => { failed = true; });
  for (const stream of [child.stdout, child.stderr]) stream.on('data', chunk => {
    size += chunk.length;
    if (size > limit) { failed = true; child.kill(); }
    else chunks.push(chunk);
  });
  child.stdin.end(input, () => input.fill(0));
  const checks = [];
  try {
    let ready = false;
    while (Date.now() - started < 45000) {
      assert.ok(!failed && !closed && child.exitCode === null);
      try { ready = (await requestProbe('GET', '/actuator/health/liveness')).status === 200; }
      catch { ready = false; }
      if (ready) break;
      await delay(150);
    }
    assert.ok(ready && !failed && !closed);
    const startIdentity = validateListener(listeners(), child.pid, started);
    assert.deepEqual(evidence(), build);
    const paths = ['/', '/actuator', '/actuator/health', '/actuator/env', '/v3/api-docs',
      '/swagger-ui/index.html', '/api/v1/auth/me', '/api/v1/auth/bootstrap'];
    const probes = ['/actuator/health/liveness', '/actuator/health/readiness'];
    const cases = [...probes, ...paths].map(path => ['GET', path]);
    for (const method of ['HEAD', 'POST', 'OPTIONS', 'PUT', 'PATCH', 'DELETE', 'TRACE']) {
      for (const path of [...probes, '/api/v1/auth/me']) cases.push([method, path]);
    }
    for (const [method, path] of cases) {
      assert.ok(!failed && !closed && child.exitCode === null);
      const response = await requestProbe(method, path);
      validateHttpProbe(method, path, response.status, response.headers, response.body);
      checks.push({ method, path, status: response.status });
    }
    validateListener(listeners(), child.pid, started, startIdentity);
    assert.ok(!failed && !closed);
    assert.equal(Buffer.concat(chunks).toString('utf8').trim(), '');
  } finally {
    input.fill(0);
    if (!closed) child.kill();
    if (!closed) await Promise.race([finished, delay(10000)]);
    if (!closed) { child.kill('SIGKILL'); await Promise.race([finished, delay(5000)]); }
    for (const chunk of chunks) chunk.fill(0);
    assert.ok(closed, 'Owned process did not stop');
  }
  assert.ok(!listeners().some(row => row.pid === child.pid), 'Owned listener remains');
  return { pid: child.pid, stopped: true, checks };
}

function mainPreservation(before, after) {
  const { mainCatalog: beforeMain, ...beforeRest } = before;
  const { mainCatalog: afterMain, ...afterRest } = after;
  validatePreservation(beforeRest, afterRest);
}

async function executeMain(args) {
  const recorded = records();
  validateMainRequest(args, recorded, mainJournals.filter(name => existsSync(join(directory, `${name}.json`))));
  assert.equal(recorded.state.root, root);
  const build = evidence();
  validateSuccessorBuild(recorded.state.build, build);
  processResult(process.execPath, ['--test', join(root, 'tools/local-data.test.mjs'),
    join(root, 'tools/local-environment.test.mjs')]);
  const before = await guard();
  validatePreservation(recorded.state.before, before.snapshot);
  await destinations(before.passwords, recorded.provision.binding);
  const expected = await schemaSnapshot(before.passwords, TARGETS[0]);
  assert.deepEqual(await schemaSnapshot(before.passwords, TARGETS[1]), expected);
  const journals = Object.fromEntries(['state', ...commands.flatMap(name => [name, `${name}.started`])]
    .map(name => [name, sha(read(join(directory, `${name}.json`)))]));
  const recheck = () => {
    assert.deepEqual(evidence(), build);
    for (const [name, digest] of Object.entries(journals)) assert.equal(sha(read(join(directory, `${name}.json`))), digest);
  };
  assert.deepEqual(listeners(), []);
  let reservation = await reserveMainPort();
  const id = randomUUID();
  let claimed = false;
  let mutationStarted = false;
  let result;
  let failure;
  let preservationVerified = false;
  try {
    recheck();
    journal('main.started', { id, qualificationId: recorded.state.id, status: 'started', at: new Date().toISOString(),
      before: before.snapshot, build, qualificationJournals: journals, expectedCatalog: expected });
    claimed = true;
    packaged(before.passwords[runtime], 'data-local', 'DATABASE_STARTUP_REFUSED', 2);
    validatePreservation(before.snapshot, (await guard()).snapshot);
    recheck();
    mutationStarted = true;
    packaged(before.passwords[migrator], 'migrate-local', 'MIGRATION_OK count=1');
    const migrated = await guard(expected);
    mainPreservation(before.snapshot, migrated.snapshot);
    recheck();
    journal('main.migrated', { id, status: 'passed', checkedAt: new Date().toISOString(),
      migrationsExecuted: 1, mainCatalog: migrated.snapshot.mainCatalog });
    packaged(before.passwords[migrator], 'migrate-local', 'MIGRATION_OK count=0');
    const repeated = await guard(expected);
    validatePreservation(migrated.snapshot, repeated.snapshot);
    recheck();
    journal('main.noop', { id, status: 'passed', checkedAt: new Date().toISOString(), migrationsExecuted: 0,
      mainCatalog: repeated.snapshot.mainCatalog });
    await releasePort(reservation);
    reservation = undefined;
    assert.deepEqual(listeners(), []);
    recheck();
    const http = await packagedHttp(before.passwords[runtime], build);
    const after = await guard(expected);
    validatePreservation(migrated.snapshot, after.snapshot);
    await destinations(after.passwords, recorded.provision.binding);
    assert.deepEqual(await schemaSnapshot(after.passwords, TARGETS[0]), expected);
    assert.deepEqual(await schemaSnapshot(after.passwords, TARGETS[1]), expected);
    recheck();
    preservationVerified = true;
    result = { id, qualificationId: recorded.state.id, command: 'migrate-main', status: 'passed',
      checkedAt: new Date().toISOString(), migrationsExecuted: 1, repeatMigrationsExecuted: 0,
      emptyRuntimeRefused: true, noTenantRows: true, http, mainCatalog: after.snapshot.mainCatalog,
      protectedContainersUnchanged: true, untouchedDatabaseIdentitiesUnchanged: true, artifactsUnchanged: true };
    journal('main.passed', result);
  } catch (error) {
    failure = error;
    try {
      const after = await guard(mutationStarted ? expected : undefined);
      mainPreservation(before.snapshot, after.snapshot);
      recheck();
      preservationVerified = true;
    } catch { preservationVerified = false; }
    if (claimed && !existsSync(join(directory, 'main.passed.json'))) journal('main.failed', {
      id, status: 'failed', checkedAt: new Date().toISOString(), mutationStarted, preservationVerified,
      resourcesRetained: true, rawOutputWithheld: true });
  } finally {
    if (reservation) await releasePort(reservation);
  }
  if (failure) throw failure;
  assert.ok(result);
  return { command: 'migrate-main', status: 'passed', report: 'tmp/l03-app/main.passed.json',
    httpChecks: result.http.checks.length, ownedProcessStopped: result.http.stopped };
}

async function recoveryGuard(expected, qualified, binding) {
  const checked = await guard(expected);
  await destinations(checked.passwords, binding);
  for (const database of ['authority_pro', ...TARGETS]) {
    if (database !== 'authority_pro') assert.deepEqual(await schemaSnapshot(checked.passwords, database), qualified);
    validateColumnPrivileges(await query(checked.passwords, database,
      `SELECT n.nspname,c.relname,a.attname,a.attacl::text AS acl FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE ${userNamespace} AND a.attacl IS NOT NULL ORDER BY 1,2,3`));
    validateTypePrivileges(await query(checked.passwords, database,
      `SELECT n.nspname,t.typname,pg_get_userbyid(t.typowner) AS owner,t.typacl::text AS acl,has_type_privilege('${runtime}',t.oid,'USAGE') AS runtime_usage FROM pg_type t JOIN pg_namespace n ON n.oid=t.typnamespace WHERE ${userNamespace} ORDER BY 1,2`));
  }
  return checked;
}

async function executeRecovery(args) {
  const recorded = records();
  const originals = ['main.started', 'main.failed'];
  assert.deepEqual(mainJournals.filter(name => existsSync(join(directory, `${name}.json`))), originals);
  const started = JSON.parse(read(join(directory, 'main.started.json')));
  const failed = JSON.parse(read(join(directory, 'main.failed.json')));
  const digests = Object.fromEntries(originals.map(name => [name, sha(read(join(directory, `${name}.json`)))]));
  validateRecoveryRequest(args, recorded, started, failed, digests,
    recoveryJournals.filter(name => existsSync(join(directory, `${name}.json`))));
  assert.equal(recorded.state.root, root);
  const build = evidence();
  validateSuccessorBuild(recorded.state.build, build);
  validateSuccessorBuild(started.build, build);
  processResult(process.execPath, ['--test', join(root, 'tools/local-data.test.mjs'),
    join(root, 'tools/local-environment.test.mjs')]);
  const journalNames = ['state', ...commands.flatMap(name => [name, `${name}.started`])];
  assert.deepEqual(Object.keys(started.qualificationJournals).sort(), [...journalNames].sort());
  const immutable = { ...started.qualificationJournals, ...digests };
  const recheck = () => {
    assert.deepEqual(evidence(), build);
    for (const [name, digest] of Object.entries(immutable)) assert.equal(sha(read(join(directory, `${name}.json`))), digest);
    assert.deepEqual(mainJournals.filter(name => existsSync(join(directory, `${name}.json`))), originals);
  };
  recheck();
  const expected = recoveryCatalog(started.expectedCatalog);
  const before = await recoveryGuard(expected, started.expectedCatalog, recorded.provision.binding);
  mainPreservation(started.before, before.snapshot);
  assert.deepEqual(listeners(), []);
  let reservation = await reserveMainPort();
  const id = randomUUID();
  let claimed = false;
  let result;
  let failure;
  try {
    recheck();
    journal('recovery.started', { id, failedAttemptId: started.id, qualificationId: recorded.state.id,
      status: 'started', at: new Date().toISOString(), before: before.snapshot, build,
      immutableJournals: immutable, expectedCatalog: expected });
    claimed = true;
    packaged(before.passwords[migrator], 'migrate-local', 'MIGRATION_OK count=0');
    const repeated = await recoveryGuard(expected, started.expectedCatalog, recorded.provision.binding);
    validatePreservation(before.snapshot, repeated.snapshot);
    recheck();
    journal('recovery.noop', { id, status: 'passed', checkedAt: new Date().toISOString(),
      migrationsExecuted: 0, mainCatalog: repeated.snapshot.mainCatalog });
    await releasePort(reservation);
    reservation = undefined;
    assert.deepEqual(listeners(), []);
    recheck();
    const http = await packagedHttp(before.passwords[runtime], build);
    const after = await recoveryGuard(expected, started.expectedCatalog, recorded.provision.binding);
    validatePreservation(before.snapshot, after.snapshot);
    recheck();
    result = { id, failedAttemptId: started.id, qualificationId: recorded.state.id,
      command: 'recover-main', status: 'passed', checkedAt: new Date().toISOString(),
      migrationsExecuted: 0, noTenantRows: true, defaultAndTypePrivilegesVerified: true,
      http, mainCatalog: after.snapshot.mainCatalog, protectedContainersUnchanged: true,
      untouchedDatabaseIdentitiesUnchanged: true, artifactsUnchanged: true, originalJournalsUnchanged: true };
    journal('recovery.passed', result);
  } catch (error) {
    failure = error;
    let preservationVerified = false;
    try {
      const after = await recoveryGuard(expected, started.expectedCatalog, recorded.provision.binding);
      validatePreservation(before.snapshot, after.snapshot);
      recheck();
      preservationVerified = true;
    } catch { preservationVerified = false; }
    if (claimed && !existsSync(join(directory, 'recovery.passed.json'))) journal('recovery.failed', {
      id, failedAttemptId: started.id, status: 'failed', checkedAt: new Date().toISOString(),
      preservationVerified, resourcesRetained: true, rawOutputWithheld: true });
  } finally {
    if (reservation) await releasePort(reservation);
  }
  if (failure) throw failure;
  assert.ok(result);
  return { command: 'recover-main', status: 'passed', report: 'tmp/l03-app/recovery.passed.json',
    httpChecks: result.http.checks.length, ownedProcessStopped: result.http.stopped };
}

export async function main(args) {
  try {
    if (args[0] === 'recover-main') return await executeRecovery(args);
    return args[0] === 'migrate-main' ? await executeMain(args) : await execute(validateCommand(args));
  }
  catch (error) {
    const line = error.stack?.match(/local-data\.mjs:(\d+):\d+/)?.[1] ?? 'unknown';
    throw new Error(`LOCAL_DATA_FAILED guardLine=${line}; raw output withheld; partial resources retained; no implicit retry`);
  }
}

if (process.argv[1] && pathToFileURL(resolve(process.argv[1])).href === import.meta.url) {
  main(process.argv.slice(2)).then(report => console.log(JSON.stringify(report))).catch(error => {
    console.error(error.message);
    process.exitCode = 1;
  });
}
