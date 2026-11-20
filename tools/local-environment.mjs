import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { createHash, createHmac, pbkdf2Sync, randomBytes, randomUUID } from 'node:crypto';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { createServer } from 'node:net';
import { setTimeout as delay } from 'node:timers/promises';
import pg from 'pg';
import { freemem } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const root = dirname(dirname(fileURLToPath(import.meta.url)));
const directory = join(root, 'tmp', 'l03');
const statePath = join(directory, 'environment.json');
const credentialsPath = join(directory, 'credentials.dpapi');
const envPath = join(directory, 'compose.env');
const label = 'com.authoritypro.environment';
const image = 'postgres@sha256:cf78e76683b9ca8c5733cbbdce6c9262b45b6767934dd0a95e671f9a0fc20685';
const container = 'authority-pro-postgres';
const volumes = ['authority-pro-pgdata', 'authority-pro-pgsecrets'];
const network = 'authority-pro-network';
const roles = ['authority_pro_operator', 'authority_pro_migrator', 'authority_pro_runtime'];
const helper = 'authority-pro-secret-init';

export function scramVerifier(password, salt = randomBytes(16)) {
  const salted = pbkdf2Sync(password, salt, 16384, 32, 'sha256');
  const client = createHmac('sha256', salted).update('Client Key').digest();
  const stored = createHash('sha256').update(client).digest('base64');
  const server = createHmac('sha256', salted).update('Server Key').digest('base64');
  return `SCRAM-SHA-256$16384:${salt.toString('base64')}$${stored}:${server}`;
}

export function validateState(state, daemon, manifest) {
  assert.equal(state.version, 1, 'Unknown environment state version');
  assert.match(state.id, /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/);
  assert.equal(state.root, root, 'Environment belongs to a different checkout');
  assert.equal(state.daemon, daemon, 'Docker daemon has changed');
  assert.equal(state.image, image, 'Unexpected PostgreSQL image');
  assert.equal(state.manifest, manifest, 'Provisioning files changed; review before continuing');
}

function run(command, args, input, timeout = 120000) {
  const env = { ...process.env };
  for (const key of Object.keys(env)) {
    if (key.startsWith('COMPOSE_') || key === 'AUTHORITY_PRO_ENVIRONMENT_ID') delete env[key];
  }
  const result = spawnSync(command, args, {
    cwd: root, env, input, encoding: 'utf8', windowsHide: true, timeout, maxBuffer: 4 * 1024 * 1024,
  });
  if (result.error) throw new Error(`${command} could not complete; no automatic cleanup was performed`);
  return result;
}

function docker(args, input, timeout) {
  const result = run('docker', args, input, timeout);
  if (result.status !== 0) throw new Error(`Docker ${args[0]} failed; retained resources require inspection (raw output withheld)`);
  return result.stdout.trim();
}

function protect(value, decrypt = false) {
  if (process.platform !== 'win32') throw new Error('This local credential store requires Windows DPAPI');
  const operation = decrypt ? 'Unprotect' : 'Protect';
  const script = `Add-Type -AssemblyName System.Security; try { $bytes = [Convert]::FromBase64String([Console]::In.ReadToEnd()); $result = [System.Security.Cryptography.ProtectedData]::${operation}($bytes, $null, [System.Security.Cryptography.DataProtectionScope]::CurrentUser); [Console]::Out.Write([Convert]::ToBase64String($result)) } catch { exit 1 }`;
  const result = run('powershell.exe', ['-NoLogo', '-NoProfile', '-NonInteractive', '-Command', script], Buffer.from(value).toString('base64'));
  if (result.status !== 0) throw new Error('Windows credential protection failed; no credential output is displayed');
  return Buffer.from(result.stdout.trim(), 'base64');
}

function manifestHash() {
  const hash = createHash('sha256');
  for (const path of ['compose.yaml', 'tools/local-postgres-init.sql']) hash.update(readFileSync(join(root, path)));
  return hash.digest('hex');
}

function snapshot() {
  const ids = docker(['container', 'ls', '-a', '--format', '{{.ID}} {{.Names}}']).split(/\r?\n/)
    .filter(line => line && ![container, helper].includes(line.split(' ')[1]))
    .map(line => line.split(' ')[0]);
  return ids.map(id => JSON.parse(docker(['container', 'inspect', '--format',
    '{"name":{{json .Name}},"id":{{json .Id}},"image":{{json .Image}},"startedAt":{{json .State.StartedAt}},"running":{{json .State.Running}},"restarts":{{.RestartCount}},"mounts":{{json .Mounts}}}', id])))
    .sort((left, right) => left.id.localeCompare(right.id));
}

export function assertNoCollisions(containers, existingVolumes, networks, composeContainers) {
  if (containers.includes(container) || containers.includes(helper) || volumes.some(name => existingVolumes.includes(name))
      || networks.includes(network) || composeContainers.length > 0) {
    throw new Error('Dedicated Docker name or Compose project collision; resources will not be adopted or removed');
  }
}

function localDaemon() {
  const configuredHost = process.env.DOCKER_HOST;
  if (configuredHost && !configuredHost.startsWith('npipe://')) throw new Error('Only the local Windows Docker daemon is allowed');
  const context = docker(['context', 'show']);
  const endpoint = docker(['context', 'inspect', '--format', '{{.Endpoints.docker.Host}}', context]);
  if (!endpoint.startsWith('npipe://')) throw new Error('A remote Docker context is not allowed');
  return docker(['info', '--format', '{{.ID}}']);
}

function checkVolumeOwnership(state) {
  for (const name of volumes) assert.equal(resource('volume', name).Labels?.[label], state.id, 'Volume ownership mismatch');
  assert.equal(resource('network', network).Labels?.[label], state.id, 'Network ownership mismatch');
}

function resource(kind, name) {
  return JSON.parse(docker([kind, 'inspect', '--format', '{{json .}}', name]));
}

function checkOwnership(state) {
  checkVolumeOwnership(state);
  const raw = docker(['container', 'inspect', '--format',
    '{"id":{{json .Id}},"label":{{json (index .Config.Labels "com.authoritypro.environment")}},"image":{{json .Image}},"memory":{{.HostConfig.Memory}},"memorySwap":{{.HostConfig.MemorySwap}},"nanoCpus":{{.HostConfig.NanoCpus}},"pids":{{.HostConfig.PidsLimit}},"privileged":{{.HostConfig.Privileged}},"ports":{{json .HostConfig.PortBindings}},"mounts":{{json .Mounts}},"networks":{{json .NetworkSettings.Networks}}}', container]);
  const current = JSON.parse(raw);
  assert.equal(current.label, state.id, 'Container ownership mismatch');
  assert.equal(current.image, state.imageId, 'Container image mismatch');
  if (state.containerId) assert.equal(current.id, state.containerId, 'Container was replaced');
  assert.equal(current.memory, 256 * 1024 * 1024);
  assert.equal(current.memorySwap, current.memory);
  assert.equal(current.nanoCpus, 500000000);
  assert.equal(current.pids, 128);
  assert.equal(current.privileged, false);
  assert.deepEqual(current.ports, { '5432/tcp': [{ HostIp: '127.0.0.1', HostPort: '5434' }] });
  assert.deepEqual(Object.keys(current.networks), [network]);
  const data = current.mounts.find(mount => mount.Destination === '/var/lib/postgresql/data');
  const secrets = current.mounts.find(mount => mount.Destination === '/run/authority-pro-secrets');
  assert.equal(data?.Name, volumes[0]);
  assert.equal(secrets?.Name, volumes[1]);
  assert.equal(secrets?.RW, false);
  assert.equal(current.mounts.length, 3);
  return current.id;
}

function postgres(passwords, role, database, sql) {
  assert.ok(roles.includes(role), 'Unknown project role');
  assert.ok(['authority_pro', 'postgres', 'template1'].includes(database), 'Unknown project database');
  return run('docker', ['exec', '-i', '--env', 'PGPASSFILE=/nonexistent-authority-pro-password-file', container,
    'sh', '-c', 'unset PGPASSWORD PGSERVICE PGSERVICEFILE PGHOSTADDR; IFS= read -r PGPASSWORD; export PGPASSWORD; exec psql --no-psqlrc --no-password --host=127.0.0.1 --port=5432 --username="$1" --dbname="$2" --set=ON_ERROR_STOP=1 --set=VERBOSITY=sqlstate --quiet --tuples-only --no-align',
    'sh', role, database], `${passwords[role]}\nSET statement_timeout = '5s'; SET lock_timeout = '1s';\n${sql}\n`, 30000);
}

function sql(passwords, role, database, query) {
  const result = postgres(passwords, role, database, query);
  if (result.status !== 0) throw new Error(`PostgreSQL check failed for ${role} on ${database}; raw output withheld`);
  return result.stdout.trim();
}

async function portFree() {
  await new Promise((resolvePromise, reject) => {
    const server = createServer();
    server.once('error', () => reject(new Error('Loopback port 5434 is occupied; no service will be stopped')));
    server.listen({ host: '127.0.0.1', port: 5434, exclusive: true }, () => server.close(resolvePromise));
  });
}

async function hostQuery(password, user, database, query) {
  const client = new pg.Client({ host: '127.0.0.1', port: 5434, user, database, password,
    ssl: false, connectionTimeoutMillis: 5000, query_timeout: 5000,
    application_name: 'authority-pro-environment-check' });
  try {
    await client.connect();
    return (await client.query(query)).rows;
  } finally {
    await client.end();
  }
}

async function waitHealthy(id) {
  const deadline = Date.now() + 90000;
  while (Date.now() < deadline) {
    const status = JSON.parse(docker(['container', 'inspect', '--format', '{{json .State}}', id]));
    if (!status.Running || status.Health?.Status === 'unhealthy') throw new Error('Dedicated PostgreSQL failed startup; partial resources are retained');
    if (status.Health?.Status === 'healthy') return;
    await delay(500);
  }
  throw new Error('Dedicated PostgreSQL startup timed out; partial resources are retained');
}

export function accessLocalEnvironment() {
  const state = JSON.parse(readFileSync(statePath, 'utf8'));
  assert.equal(state.phase, 'verified', 'Environment must already be verified');
  validateState(state, localDaemon(), manifestHash());
  checkOwnership(state);
  const credentials = JSON.parse(protect(readFileSync(credentialsPath), true).toString('utf8'));
  assert.equal(credentials.id, state.id, 'Credential ownership mismatch');
  return { state, passwords: credentials.passwords };
}

export function protectedContainerSnapshot() { return snapshot(); }

async function verify(state, protectedBefore = snapshot()) {
  validateState(state, localDaemon(), manifestHash());
  const containerId = checkOwnership(state);
  const credentials = JSON.parse(protect(readFileSync(credentialsPath), true).toString('utf8'));
  assert.equal(credentials.id, state.id, 'Credential ownership mismatch');
  const passwords = credentials.passwords;
  const admin = roles[0];
  const migrator = roles[1];
  const runtime = roles[2];
  assert.equal(sql(passwords, admin, 'postgres', "SELECT current_setting('server_version_num');"), '160015');
  const flags = sql(passwords, admin, 'postgres', "SELECT rolname || ':' || rolcanlogin || ':' || rolsuper || ':' || rolcreatedb || ':' || rolcreaterole || ':' || rolbypassrls || ':' || rolreplication || ':' || rolinherit || ':' || rolconnlimit FROM pg_roles WHERE rolname IN ('authority_pro_owner','authority_pro_migrator','authority_pro_runtime') ORDER BY rolname;");
  assert.equal(flags, 'authority_pro_migrator:true:false:false:false:false:false:false:2\nauthority_pro_owner:false:false:false:false:false:false:false:-1\nauthority_pro_runtime:true:false:false:false:false:false:false:5');
  const membership = sql(passwords, admin, 'postgres', "SELECT parent.rolname || ':' || member.rolname || ':' || m.admin_option || ':' || m.inherit_option || ':' || m.set_option FROM pg_auth_members m JOIN pg_roles parent ON parent.oid=m.roleid JOIN pg_roles member ON member.oid=m.member WHERE member.rolname IN ('authority_pro_runtime','authority_pro_migrator') ORDER BY member.rolname;");
  assert.equal(membership, 'authority_pro_owner:authority_pro_migrator:false:false:true');
  assert.equal(sql(passwords, runtime, 'authority_pro', "SELECT current_user || ':' || current_database() || ':' || has_database_privilege(current_user,current_database(),'CONNECT') || ':' || has_database_privilege(current_user,current_database(),'CREATE') || ':' || has_database_privilege(current_user,current_database(),'TEMPORARY') || ':' || has_schema_privilege(current_user,'public','USAGE') || ':' || has_schema_privilege(current_user,'public','CREATE');"), 'authority_pro_runtime:authority_pro:true:false:false:true:false');
  assert.equal(sql(passwords, admin, 'postgres', "SELECT count(*) FROM pg_database d CROSS JOIN LATERAL aclexplode(COALESCE(d.datacl,acldefault('d',d.datdba))) a WHERE d.datallowconn AND a.grantee=0 AND a.privilege_type IN ('CONNECT','CREATE','TEMPORARY');"), '0');
  assert.equal(sql(passwords, admin, 'postgres', "SELECT count(*) FROM pg_hba_file_rules WHERE error IS NOT NULL OR auth_method <> 'scram-sha-256';"), '0');
  const publicCatalogQuery = "SELECT c.oid::text || ':' || c.relname::text || ':' || c.relkind::text FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public' ORDER BY c.oid;";
  const publicBefore = sql(passwords, admin, 'authority_pro', publicCatalogQuery);
  sql(passwords, migrator, 'authority_pro', 'BEGIN; SET LOCAL ROLE authority_pro_owner; CREATE TABLE public.environment_probe (id integer PRIMARY KEY); ROLLBACK;');
  assert.equal(sql(passwords, admin, 'authority_pro', publicCatalogQuery), publicBefore);
  const denied = [
    [runtime, 'authority_pro', 'CREATE TABLE public.environment_probe (id integer);'],
    [runtime, 'authority_pro', 'CREATE TEMP TABLE environment_probe (id integer);'],
    [runtime, 'authority_pro', 'CREATE SCHEMA environment_probe;'],
    [runtime, 'authority_pro', 'SET ROLE authority_pro_owner;'],
    [runtime, 'authority_pro', 'SET ROLE authority_pro_migrator;'],
    [runtime, 'authority_pro', 'SET ROLE authority_pro_operator;'],
    [runtime, 'authority_pro', 'SELECT rolname FROM pg_authid;'],
    [migrator, 'authority_pro', 'CREATE TABLE public.environment_probe (id integer);'],
    [runtime, 'postgres', 'SELECT 1;'],
    [runtime, 'template1', 'SELECT 1;'],
    [migrator, 'postgres', 'SELECT 1;'],
  ];
  for (const [role, database, query] of denied) {
    const result = postgres(passwords, role, database, `BEGIN; ${query} ROLLBACK;`);
    assert.notEqual(result.status, 0, `Expected denial for ${role}: ${query}`);
    assert.match(result.stderr, /42501|permission denied for database/, 'Failure was not a privilege denial');
  }
  const incorrect = { ...passwords, [runtime]: randomBytes(32).toString('base64url') };
  const refused = postgres(incorrect, runtime, 'authority_pro', 'SELECT 1;');
  assert.notEqual(refused.status, 0);
  assert.match(refused.stderr, /28P01|password authentication failed/, 'Invalid password did not reach authentication');
  const systemIdentifier = sql(passwords, admin, 'postgres', 'SELECT system_identifier FROM pg_control_system();');
  const hostIdentity = await hostQuery(passwords[admin], admin, 'postgres', 'SELECT system_identifier::text AS id FROM pg_control_system()');
  assert.equal(hostIdentity[0].id, systemIdentifier, 'Published endpoint belongs to a different PostgreSQL cluster');
  const hostRuntime = await hostQuery(passwords[runtime], runtime, 'authority_pro', 'SELECT current_user AS role, current_database() AS database');
  assert.deepEqual(hostRuntime, [{ role: runtime, database: 'authority_pro' }]);
  await assert.rejects(hostQuery(randomBytes(32).toString('base64url'), runtime, 'authority_pro', 'SELECT 1'), error => error.code === '28P01');
  const protectedAfter = snapshot();
  assert.deepEqual(protectedAfter, protectedBefore, 'Concurrent container lifecycle changes observed');
  const protectedUnchanged = true;
  const report = { checkedAt: new Date().toISOString(), environmentId: state.id, containerId, postgres: '16.15',
    listener: '127.0.0.1:5434', database: 'authority_pro', runtimeRole: runtime, deniedPrivilegeChecks: denied.length,
    wrongPasswordRejected: true, migrationOwnerRollback: true, publicObjects: publicBefore ? publicBefore.split(/\r?\n/).length : 0, scramOnly: true,
    publicCatalogUnchanged: true,
    memoryLimitMiB: 256, cpuLimit: 0.5, hostAuthenticationVerified: true,
    protectedContainersObserved: protectedBefore.length, protectedContainersUnchanged: protectedUnchanged,
    concurrentChangesObserved: !protectedUnchanged,
    scope: 'local environment only; no application migrations, RLS, sessions, backup/restore or G2 qualification' };
  writeFileSync(join(directory, 'verification.json'), `${JSON.stringify(report, null, 2)}\n`);
  console.log(JSON.stringify(report, null, 2));
  return containerId;
}

async function prepare() {
  if (existsSync(statePath)) {
    const state = JSON.parse(readFileSync(statePath, 'utf8'));
    if (state.phase !== 'verified') throw new Error('Partial provisioning exists; inspect it without deleting or reinitializing resources');
    await verify(state);
    return;
  }
  if (existsSync(credentialsPath) || existsSync(envPath)) throw new Error('Unclaimed environment files exist; no overwrite is allowed');
  if (process.platform !== 'win32') throw new Error('Preparation requires Windows DPAPI');
  if (freemem() < 384 * 1024 * 1024) throw new Error('Insufficient free host memory; no existing process will be stopped');
  const containers = docker(['container', 'ls', '-a', '--format', '{{.Names}}']).split(/\r?\n/);
  const currentVolumes = docker(['volume', 'ls', '--format', '{{.Name}}']).split(/\r?\n/);
  const networks = docker(['network', 'ls', '--format', '{{.Name}}']).split(/\r?\n/);
  const composeContainers = docker(['container', 'ls', '-a', '--filter', 'label=com.docker.compose.project=authority-pro', '--format', '{{.ID}}']).split(/\r?\n/).filter(Boolean);
  assertNoCollisions(containers, currentVolumes, networks, composeContainers);
  await portFree();
  const state = { version: 1, id: randomUUID(), root, daemon: localDaemon(), image,
    imageId: docker(['image', 'inspect', '--format', '{{.Id}}', image]), manifest: manifestHash(),
    protectedContainers: snapshot(), phase: 'preparing' };
  const passwords = Object.fromEntries(roles.map(role => [role, randomBytes(32).toString('base64url')]));
  const encrypted = protect(Buffer.from(JSON.stringify({ id: state.id, passwords })));
  mkdirSync(directory, { recursive: true });
  writeFileSync(credentialsPath, encrypted, { flag: 'wx' });
  writeFileSync(envPath, `AUTHORITY_PRO_ENVIRONMENT_ID=${state.id}\n`, { flag: 'wx' });
  writeFileSync(statePath, `${JSON.stringify(state, null, 2)}\n`, { flag: 'wx' });
  for (const name of volumes) {
    docker(['volume', 'create', '--label', `${label}=${state.id}`, name]);
    assert.equal(resource('volume', name).Labels?.[label], state.id, 'New volume ownership mismatch');
  }
  docker(['network', 'create', '--driver', 'bridge', '--label', `${label}=${state.id}`, network]);
  checkVolumeOwnership(state);
  const writer = 'set -eu; umask 077; IFS= read -r operator; IFS= read -r migrator; IFS= read -r runtime; printf "%s" "$operator" > /secrets/operator-password; printf "%s" "$migrator" > /secrets/migrator-verifier; printf "%s" "$runtime" > /secrets/runtime-verifier; chown postgres:postgres /secrets /secrets/*; chmod 700 /secrets; chmod 400 /secrets/*';
  docker(['run', '-i', '--pull=never', '--name', helper, '--label', `${label}=${state.id}`,
    '--network', 'none', '--memory', '32m', '--memory-swap', '32m', '--cpus', '0.5', '--pids-limit', '32',
    '--mount', `type=volume,src=${volumes[1]},dst=/secrets`, '--entrypoint', 'sh', image, '-c', writer],
    `${passwords[roles[0]]}\n${scramVerifier(passwords[roles[1]])}\n${scramVerifier(passwords[roles[2]])}\n`);
  const helperId = docker(['container', 'inspect', '--format', '{{.Id}}', helper]);
  assert.equal(docker(['container', 'inspect', '--format', '{{index .Config.Labels "com.authoritypro.environment"}}', helperId]), state.id);
  docker(['container', 'rm', helperId]);
  checkVolumeOwnership(state);
  const projectCollisions = docker(['container', 'ls', '-a', '--filter', 'label=com.docker.compose.project=authority-pro', '--format', '{{.ID}}']);
  assert.equal(projectCollisions, '', 'Compose project appeared during preparation');
  docker(['compose', '--project-directory', root, '--env-file', envPath, '--file', join(root, 'compose.yaml'),
    '--project-name', 'authority-pro', 'create', '--no-build', '--pull', 'never', '--no-recreate', 'db']);
  const createdId = checkOwnership(state);
  docker(['container', 'start', createdId]);
  await waitHealthy(createdId);
  state.containerId = await verify(state, state.protectedContainers);
  state.phase = 'verified';
  writeFileSync(statePath, `${JSON.stringify(state, null, 2)}\n`);
}

export async function main(args) {
  if (args.length !== 1 || !['prepare', 'verify'].includes(args[0])) throw new Error('Choose prepare or verify; no destructive operation is provided');
  if (args[0] === 'prepare') await prepare();
  else {
    if (!existsSync(statePath)) throw new Error('No project environment has been prepared');
    const state = JSON.parse(readFileSync(statePath, 'utf8'));
    const containerId = await verify(state);
    if (state.phase !== 'verified') {
      state.containerId = containerId;
      state.phase = 'verified';
      writeFileSync(statePath, `${JSON.stringify(state, null, 2)}\n`);
    }
  }
}

if (process.argv[1] && pathToFileURL(resolve(process.argv[1])).href === import.meta.url) {
  main(process.argv.slice(2)).catch(error => {
    console.error(error instanceof assert.AssertionError ? 'Environment invariant failed; review targeted non-secret diagnostics. No resources were removed.' : error.message);
    process.exitCode = 1;
  });
}
