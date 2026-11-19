import assert from 'node:assert/strict';
import { createHash, createHmac, pbkdf2Sync } from 'node:crypto';
import { dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';
import { assertNoCollisions, main, scramVerifier, validateState } from './local-environment.mjs';

const root = dirname(dirname(fileURLToPath(import.meta.url)));
const baseline = {
  version: 1,
  id: 'ef431b54-099d-4032-bb1b-cb649553b5e4',
  root,
  daemon: 'test-daemon',
  image: 'postgres@sha256:cf78e76683b9ca8c5733cbbdce6c9262b45b6767934dd0a95e671f9a0fc20685',
  manifest: 'test-manifest',
};

test('accepts only the exact checkout, daemon, image and manifest', () => {
  assert.doesNotThrow(() => validateState(baseline, baseline.daemon, baseline.manifest));
});

for (const [key, value] of Object.entries({ version: 2, id: 'untrusted\nvalue', root: '/another-project', daemon: 'other-daemon', image: 'postgres:latest', manifest: 'changed' })) {
  test(`rejects a changed ${key} before environment access`, () => {
    assert.throws(() => validateState({ ...baseline, [key]: value }, baseline.daemon, baseline.manifest));
  });
}

test('creates independently verifiable SCRAM-SHA-256 keys', () => {
  const password = 'test-fixture-only';
  const salt = Buffer.from('00112233445566778899aabbccddeeff', 'hex');
  const verifier = scramVerifier(password, salt);
  const [mechanism, parameters, keys] = verifier.split('$');
  const [iterations, encodedSalt] = parameters.split(':');
  const [storedKey, serverKey] = keys.split(':');
  assert.equal(mechanism, 'SCRAM-SHA-256');
  assert.equal(Number(iterations), 16384);
  assert.deepEqual(Buffer.from(encodedSalt, 'base64'), salt);
  const derived = pbkdf2Sync(password, Buffer.from(encodedSalt, 'base64'), Number(iterations), 32, 'sha256');
  assert.equal(storedKey, createHash('sha256').update(createHmac('sha256', derived).update('Client Key').digest()).digest('base64'));
  assert.equal(serverKey, createHmac('sha256', derived).update('Server Key').digest('base64'));
  assert.ok(!verifier.includes(password));
});

test('allows unrelated Docker projects without adopting them', () => {
  assert.doesNotThrow(() => assertNoCollisions(['authority-postgres', 'another-project-db'], ['other-volume'], ['other-network'], []));
});

for (const inputs of [
  [['authority-pro-postgres'], [], [], []],
  [['authority-pro-secret-init'], [], [], []],
  [[], ['authority-pro-pgdata'], [], []],
  [[], ['authority-pro-pgsecrets'], [], []],
  [[], [], ['authority-pro-network'], []],
  [['differently-named-db'], [], [], ['existing-compose-container-id']],
]) {
  test(`rejects dedicated resource or Compose-label collision ${JSON.stringify(inputs)}`, () => {
    assert.throws(() => assertNoCollisions(...inputs), /collision/);
  });
}

test('generates distinct salts for repeated inputs', () => {
  assert.notEqual(scramVerifier('test-fixture-only'), scramVerifier('test-fixture-only'));
});

for (const args of [[], ['drop'], ['reset'], ['stop'], ['prepare', '--force'], ['verify', '--ignore-errors']]) {
  test(`rejects unsupported arguments ${JSON.stringify(args)} without Docker access`, async () => {
    await assert.rejects(main(args), /Choose prepare or verify/);
  });
}
