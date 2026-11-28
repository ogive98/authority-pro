import test from 'node:test';
import assert from 'node:assert/strict';
import { resolve } from 'node:path';
import { root, readJson, manifestErrors, markdownAnchors, linkErrors, counterErrors } from './validate-docs.mjs';

const valid = () => readJson('MODULE-MANIFEST-TEMPLATE.json');

test('valid foundation manifest is accepted', () => assert.deepEqual(manifestErrors(valid()), []));
test('invalid fixture fails for missing id and malformed version', () => {
  const errors = manifestErrors(readJson('MODULE-MANIFEST-INVALID.json'));
  assert.equal(errors.length, 2);
  assert.ok(errors.some((error) => error.includes("required property 'id'")));
  assert.ok(errors.some((error) => error.startsWith('/version:')));
});

const mutations = [
  ['missing required id', (m) => { delete m.id; }],
  ['unknown root property', (m) => { m.privileged = true; }],
  ['unsupported execution', (m) => { m.execution = 'remote-script'; }],
  ['unsupported module kind', (m) => { m.kind = 'business-module'; }],
  ['untrusted publisher declaration', (m) => { m.trust = 'third-party'; }],
  ['malformed version', (m) => { m.version = 'latest'; }],
  ['oversized id', (m) => { m.id = 'a'.repeat(65); }],
  ['duplicate dependency ids', (m) => { m.dependencies = [{ id: 'other', version: '1.0.0' }, { id: 'other', version: '2.0.0' }]; }],
  ['self dependency', (m) => { m.dependencies = [{ id: m.id, version: m.version }]; }],
  ['undeclared UI permission', (m) => { m.ui.contributions = [{ id: 'runtime.test', kind: 'navigation', routeKey: 'runtime.test', permission: 'runtime.unknown' }]; }],
  ['unknown nested property', (m) => { m.dataOwnership.secret = true; }],
  ['too many dependencies', (m) => { m.dependencies = Array.from({ length: 33 }, (_, n) => ({ id: `other-${n}`, version: '1.0.0' })); }],
];
for (const [name, mutate] of mutations) {
  test(`rejects ${name}`, () => {
    const manifest = valid();
    mutate(manifest);
    assert.ok(manifestErrors(manifest).length > 0);
  });
}

test('heading anchors support punctuation, unicode and duplicate headings', () => {
  assert.deepEqual([...markdownAnchors('# Hello, World!\n## Été\n## Hello, World!')], ['hello-world', 'été', 'hello-world-1']);
});
test('headings inside code fences are not anchors', () => {
  assert.deepEqual([...markdownAnchors('```text\n# Example\n```\n# Real')], ['real']);
});
test('broken relative links are rejected without visiting external URLs', () => {
  assert.equal(linkErrors(resolve(root, 'README.md'), '[missing](missing-document.md)').length, 1);
  assert.deepEqual(linkErrors(resolve(root, 'README.md'), '[external](https://json-schema.org/)'), []);
});
test('valid local links and anchors pass', () => {
  assert.deepEqual(linkErrors(resolve(root, 'README.md'), '[docs](docs/README.md)\n[title](README.md#authority-pro)'), []);
});
test('matching documentation counters pass', () => {
  assert.deepEqual(counterErrors('0 errors: 7 documents, 9 links, 2 diagrams', { documents: 7, links: 9, diagrams: 2 }), []);
});
test('stale documentation counters are rejected with actual totals', () => {
  const errors = counterErrors('0 errors: 6 documents, 9 links, 2 diagrams', { documents: 7, links: 9, diagrams: 2 });
  assert.equal(errors.length, 1);
  assert.ok(errors[0].includes('actual 7 documents, 9 links, 2 diagrams'));
});
test('documents without counters are rejected', () => {
  assert.deepEqual(counterErrors('No counters here', { documents: 7, links: 9, diagrams: 2 }), ['Missing documentation counters']);
});
