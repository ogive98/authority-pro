import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { createHash, randomUUID } from 'node:crypto';
import { existsSync, lstatSync, mkdirSync, readFileSync, readdirSync, realpathSync, writeFileSync } from 'node:fs';
import { freemem } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import pg from 'pg';
import { accessLocalEnvironment, ensureProvisioner, protectedContainerSnapshot } from './local-environment.mjs';
import { sanitizedEnvironment, validateClasspath, validateMemory } from './local-data.mjs';

const root = dirname(dirname(fileURLToPath(import.meta.url)));
const backend = join(root, 'backend');
const directory = join(root, 'tmp/l05-identity');
const jdk = join(root, 'tmp/l02/tooling/jdk-21.0.13+9');
const maven = join(root, 'tmp/l02/tooling/apache-maven-3.9.16/bin/mvn.cmd');
const repository = join(root, 'tmp/l02/m2');
const databaseName = 'authority_pro_l05_identity_test';
const mainDatabase = 'authority_pro';
const admin = 'authority_pro_operator';
const owner = 'authority_pro_owner';
const migrator = 'authority_pro_migrator';
const runtime = 'authority_pro_runtime';
const provisioner = 'authority_pro_provisioner';
const marker = /^l05-identity-[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;
const sha = value => createHash('sha256').update(value).digest('hex');

function regular(path) {
  const stat = lstatSync(path);
  assert.ok(stat.isFile() && !stat.isSymbolicLink());
  assert.equal(resolve(realpathSync(path)).toLowerCase(), resolve(path).toLowerCase());
  return stat;
}

function requireCommand(args) {
  assert.ok(Array.isArray(args) && args.length === 1 && args[0] === 'qualify');
  return args[0];
}

function client(passwords, database) {
  return new pg.Client({
    host: '127.0.0.1', port: 5434, user: admin, password: passwords[admin], database, ssl: false,
    application_name: 'authority-pro-l05-guard', connectionTimeoutMillis: 5000, query_timeout: 15000,
    options: '-c statement_timeout=15000 -c lock_timeout=3000 -c search_path=pg_catalog -c default_transaction_read_only=on',
  });
}

async function query(passwords, database, text, values = [], write = false) {
  assert.ok([databaseName, mainDatabase, 'postgres'].includes(database));
  const connection = client(passwords, database);
  let failure = false;
  connection.on('error', () => { failure = true; });
  try {
    await connection.connect();
    if (write) await connection.query('SET default_transaction_read_only = off');
    const result = await connection.query(text, values);
    assert.equal(failure, false);
    return result.rows;
  } finally {
    await connection.end();
  }
}

async function provision(passwords, runId) {
  const existing = await query(passwords, 'postgres',
    'SELECT datname, pg_get_userbyid(datdba) AS db_owner, shobj_description(oid, \'pg_database\') AS marker FROM pg_database WHERE datname = $1',
    [databaseName]);
  if (existing.length === 1) {
    assert.equal(existing[0].db_owner, owner);
    assert.match(existing[0].marker ?? '', marker);
    // The retained database predates the provisioner role; CONNECT is granted idempotently.
    await query(passwords, 'postgres',
      `GRANT CONNECT ON DATABASE ${databaseName} TO ${migrator}, ${runtime}, ${provisioner}`, [], true);
    return { created: false, marker: existing[0].marker };
  }
  assert.equal(existing.length, 0);
  const state = { version: 1, runId, database: databaseName, createdAt: new Date().toISOString(),
    owner, protectedContainers: protectedContainerSnapshot() };
  writeFileSync(join(directory, `provision-${runId}.json`), `${JSON.stringify(state, null, 2)}\n`,
    { flag: 'wx', mode: 0o600 });
  await query(passwords, 'postgres',
    `CREATE DATABASE ${databaseName} OWNER ${owner} TEMPLATE template0 ALLOW_CONNECTIONS false CONNECTION LIMIT 8`,
    [], true);
  await query(passwords, 'postgres', `REVOKE ALL ON DATABASE ${databaseName} FROM PUBLIC`, [], true);
  await query(passwords, 'postgres',
    `GRANT CONNECT ON DATABASE ${databaseName} TO ${migrator}, ${runtime}, ${provisioner}`, [], true);
  await query(passwords, 'postgres', `COMMENT ON DATABASE ${databaseName} IS 'l05-identity-${runId}'`, [], true);
  await query(passwords, 'postgres', `ALTER DATABASE ${databaseName} ALLOW_CONNECTIONS true`, [], true);
  await query(passwords, databaseName,
    `ALTER SCHEMA public OWNER TO ${owner}; REVOKE ALL ON SCHEMA public FROM PUBLIC; GRANT USAGE ON SCHEMA public TO ${migrator}, ${runtime}`,
    [], true);
  return { created: true, marker: `l05-identity-${runId}` };
}

async function destination(passwords) {
  const rows = await query(passwords, 'postgres',
    'SELECT pg_get_userbyid(datdba) AS db_owner, shobj_description(oid, \'pg_database\') AS marker FROM pg_database WHERE datname = $1',
    [databaseName]);
  assert.equal(rows.length, 1);
  assert.equal(rows[0].db_owner, owner);
  assert.match(rows[0].marker ?? '', marker);
  const acl = await query(passwords, 'postgres',
    `SELECT r.rolname, has_database_privilege(r.oid, $1, 'CONNECT') AS connect,
        has_database_privilege(r.oid, $1, 'CREATE') AS create, has_database_privilege(r.oid, $1, 'TEMP') AS temp
     FROM pg_roles r WHERE r.rolname = ANY($2::text[]) ORDER BY r.rolname`,
    [databaseName, [migrator, runtime, provisioner]]);
  assert.deepEqual(acl, [migrator, provisioner, runtime].map(rolname =>
    ({ rolname, connect: true, create: false, temp: false })));
  const schema = await query(passwords, databaseName,
    `SELECT pg_get_userbyid(nspowner) AS schema_owner, has_schema_privilege($1, 'public', 'USAGE') AS usage,
        has_schema_privilege($1, 'public', 'CREATE') AS create
     FROM pg_namespace WHERE nspname = 'public'`, [runtime]);
  assert.deepEqual(schema, [{ schema_owner: owner, usage: true, create: false }]);
}

async function untouched(passwords) {
  const databases = await query(passwords, 'postgres',
    `SELECT datname, pg_get_userbyid(datdba) AS db_owner, datacl::text, datallowconn, datconnlimit, datcollate, datctype
     FROM pg_database WHERE datname <> $1 ORDER BY datname`, [databaseName]);
  const main = await query(passwords, mainDatabase, `
    SELECT md5(string_agg(value, '|' ORDER BY value)) AS fingerprint FROM (
      SELECT n.nspname || '.' || c.relname || ':' || c.relkind::text || ':' || c.relrowsecurity::text
        || ':' || c.relforcerowsecurity::text AS value
      FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
      WHERE n.nspname !~ '^pg_' AND n.nspname <> 'information_schema'
      UNION ALL
      SELECT 'history:' || version || ':' || script || ':' || checksum FROM public.flyway_schema_history
      UNION ALL
      SELECT 'rows:' || (SELECT count(*) FROM tenancy.tenants) || ':' || (SELECT count(*) FROM tenancy.companies)
        || ':' || (SELECT count(*) FROM tenancy.sites) || ':' || (SELECT count(*) FROM tenancy.memberships)
    ) catalog`);
  return { databases, main: main[0].fingerprint };
}

function buildClasspath() {
  const output = join(directory, 'classpath.txt');
  const mavenHome = join(root, 'tmp', 'l02', 'wrapper-home');
  mkdirSync(mavenHome, { recursive: true });
  const environment = { ...sanitizedEnvironment(process.env), JAVA_HOME: jdk, MAVEN_USER_HOME: mavenHome,
    MAVEN_SKIP_RC: 'true', MAVEN_OPTS: '-Xmx192m -XX:MaxMetaspaceSize=128m -Djava.awt.headless=true' };
  for (const key of Object.keys(environment)) {
    if (/^(AUTHORITY_PRO_|SPRING_|SERVER_|MANAGEMENT_|PG)/.test(key)) delete environment[key];
  }
  const args = ['--batch-mode', '--no-transfer-progress', '--settings', '.mvn/settings.xml',
    '--global-settings', '.mvn/settings.xml', '--offline', 'dependency:build-classpath',
    `-Dmdep.outputFile=${output}`, '-Dmdep.includeScope=test'];
  const commandLine = `"${maven}" ${args.map(argument => `"${argument}"`).join(' ')}`;
  const result = spawnSync('cmd.exe', ['/d', '/s', '/c', `"${commandLine}"`],
    { cwd: backend, env: environment, windowsHide: true, timeout: 240000, maxBuffer: 4 * 1024 * 1024,
      windowsVerbatimArguments: true });
  assert.ok(!result.error && result.status === 0,
    `Maven classpath generation failed (status=${result.status ?? 'none'}${result.error ? ` error=${result.error.code}` : ''}): `
    + `${String(result.stderr ?? '').trim().slice(0, 400)}`);
  const entries = validateClasspath(readFileSync(output, 'utf8'), repository);
  for (const entry of entries) assert.ok(existsSync(entry));
  return entries;
}

function verifyBuild() {
  const classes = join(backend, 'target/classes');
  const tests = join(backend, 'target/test-classes');
  const qualifier = join(tests, 'com/authoritypro/persistence/IdentityQualification.class');
  const migrations = ['V3__identity_access_foundation.sql', 'V4__session_store_and_provisioning.sql'];
  for (const path of [qualifier]) regular(path);
  for (const name of migrations) {
    const packaged = join(classes, 'db/migration', name);
    const source = join(backend, 'src/main/resources/db/migration', name);
    regular(packaged);
    regular(source);
    assert.deepEqual(readFileSync(packaged), readFileSync(source),
      `Packaged ${name} differs from its source; rebuild the backend`);
  }
  const sourceRoots = [join(backend, 'src/main/java'), join(backend, 'src/test/java')];
  const newest = sourceRoots.flatMap(path => tree(path)).reduce((latest, path) =>
    Math.max(latest, regular(path).mtimeMs), 0);
  assert.ok(regular(qualifier).mtimeMs >= newest, 'Backend build is stale; run the backend verification first');
  return { qualifier: sha(readFileSync(qualifier)),
    migrations: migrations.map(name => sha(readFileSync(join(backend, 'src/main/resources/db/migration', name)))) };
}

function tree(path) {
  const stat = lstatSync(path);
  assert.ok(stat.isDirectory() && !stat.isSymbolicLink());
  assert.equal(resolve(realpathSync(path)).toLowerCase(), resolve(path).toLowerCase());
  const entries = [];
  for (const name of readdirSync(path)) {
    const child = join(path, name);
    const childStat = lstatSync(child);
    assert.ok(!childStat.isSymbolicLink());
    if (childStat.isDirectory()) entries.push(...tree(child));
    else { regular(child); entries.push(child); }
  }
  return entries;
}

function qualify(passwords, classpath) {
  const input = Buffer.from(
    `${passwords[migrator]}\n${passwords[runtime]}\n${passwords[provisioner]}\n${databaseName}\n`);
  const cp = [join(backend, 'target/classes'), join(backend, 'target/test-classes'), ...classpath].join(';');
  try {
    const result = spawnSync(join(jdk, 'bin/java.exe'),
      ['-Xmx160m', '-XX:MaxMetaspaceSize=160m', '-Djava.awt.headless=true', '-XX:-CreateCoredumpOnCrash',
        '-XX:ErrorFile=NUL', '-cp', cp, 'com.authoritypro.persistence.IdentityQualification', 'qualify'],
      { cwd: directory, env: sanitizedEnvironment(process.env), input, windowsHide: true, timeout: 240000,
        maxBuffer: 8 * 1024 * 1024 });
    const output = result.stdout.toString('utf8');
    const lines = output.trim().split(/\r?\n/).filter(Boolean);
    const passed = lines.filter(line => /^PASS [a-z0-9_]+$/.test(line)).map(line => line.slice(5));
    const ok = lines.filter(line => /^IDENTITY_QUALIFICATION_OK checks=[0-9]+ database=authority_pro_l05_identity_test$/.test(line));
    const failed = lines.filter(line => /^IDENTITY_QUALIFICATION_FAILED check=[a-z0-9_]+ type=[A-Za-z0-9_$]+ sqlState=([A-Z0-9]{5}|none)$/.test(line));
    if (result.error || result.status !== 0 || ok.length !== 1) {
      throw new Error(failed.at(-1) ?? 'L05 qualification failed');
    }
    assert.equal(passed.length, new Set(passed).size);
    return { checks: passed, output: lines.at(-1) };
  } finally {
    input.fill(0);
  }
}

export async function main(args) {
  requireCommand(args);
  assert.equal(process.platform, 'win32');
  validateMemory(freemem());
  mkdirSync(directory, { recursive: true });
  const runId = randomUUID();
  const beforeContainers = protectedContainerSnapshot();
  const { passwords } = accessLocalEnvironment();
  for (const role of [admin, migrator, runtime]) assert.match(passwords[role], /^[A-Za-z0-9_-]{32,256}$/);
  passwords[provisioner] = ensureProvisioner(passwords);
  const beforeMain = await untouched(passwords);
  const build = verifyBuild();
  const classpath = buildClasspath();
  const created = await provision(passwords, runId);
  await destination(passwords);
  const result = qualify(passwords, classpath);
  await destination(passwords);
  const afterMain = await untouched(passwords);
  assert.deepEqual(afterMain, beforeMain);
  assert.deepEqual(protectedContainerSnapshot(), beforeContainers);
  const evidence = { version: 1, runId, database: databaseName, databaseCreated: created.created,
    createdAt: new Date().toISOString(), checks: result.checks, checkCount: result.checks.length,
    build, mainDatabaseUnchanged: true, otherDatabasesUnchanged: true, protectedContainersUnchanged: true,
    databaseRetained: true };
  writeFileSync(join(directory, `run-${runId}.json`), `${JSON.stringify(evidence, null, 2)}\n`,
    { flag: 'wx', mode: 0o600 });
  console.log(`L05_IDENTITY_QUALIFICATION_OK checks=${result.checks.length} database=${databaseName}`);
  console.log(`L05_EVIDENCE tmp/l05-identity/run-${runId}.json`);
  return 0;
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main(process.argv.slice(2)).then(code => { process.exitCode = code; }).catch(error => {
    const detail = String(error && error.message ? error.message : error).replace(/[A-Za-z0-9_-]{32,256}/g, '<redacted>');
    console.error(`L05_IDENTITY_QUALIFICATION_FAILED ${detail}`);
    process.exitCode = 1;
  });
}
