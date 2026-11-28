import { readFileSync, readdirSync, existsSync, statSync } from 'node:fs';
import { dirname, resolve, relative, extname } from 'node:path';
import { fileURLToPath } from 'node:url';
import Ajv2020 from 'ajv/dist/2020.js';
import { JSDOM } from 'jsdom';

const dom = new JSDOM('');
globalThis.window = dom.window;
globalThis.document = dom.window.document;
const { default: mermaid } = await import('mermaid');

export const root = fileURLToPath(new URL('../', import.meta.url));
const docs = resolve(root, 'docs');
const templateDir = resolve(docs, '14-TEMPLATES');
export const readJson = (name) => JSON.parse(readFileSync(resolve(templateDir, name), 'utf8'));
export const validateSchema = new Ajv2020({ allErrors: true, strict: true })
  .compile(readJson('MODULE-MANIFEST.schema.json'));

export function manifestErrors(manifest) {
  if (!validateSchema(manifest)) return validateSchema.errors.map((error) => `${error.instancePath}: ${error.message}`);
  const errors = [];
  const unique = (items, field, label) => {
    const keys = items.map((item) => item[field]);
    if (new Set(keys).size !== keys.length) errors.push(`Duplicate ${label}`);
  };
  unique(manifest.dependencies, 'id', 'dependency');
  unique(manifest.permissions, 'id', 'permission');
  unique(manifest.dataOwnership.resources, 'name', 'owned resource');
  unique(manifest.ui.contributions, 'id', 'UI contribution');
  unique(manifest.capabilities.provides, 'id', 'provided capability');
  unique(manifest.capabilities.requires, 'id', 'required capability');
  if (manifest.dependencies.some((dependency) => dependency.id === manifest.id)) errors.push('Self dependency');
  const permissions = new Set(manifest.permissions.map((permission) => permission.id));
  for (const contribution of manifest.ui.contributions) {
    if (!permissions.has(contribution.permission)) errors.push(`Undeclared UI permission: ${contribution.permission}`);
  }
  return errors;
}

export function markdownAnchors(text) {
  const seen = new Map();
  const anchors = new Set();
  const prose = text.replace(/^```[^\n]*\n[\s\S]*?^```\s*$/gm, '');
  for (const match of prose.matchAll(/^#{1,6}\s+(.+)$/gm)) {
    const base = match[1].trim().toLowerCase().replace(/[^\p{L}\p{N}\p{M}\s_-]/gu, '').replace(/\s/g, '-');
    const count = seen.get(base) ?? 0;
    seen.set(base, count + 1);
    anchors.add(count ? `${base}-${count}` : base);
  }
  return anchors;
}

export function linkErrors(file, text) {
  const errors = [];
  const prose = text.replace(/^```[^\n]*\n[\s\S]*?^```\s*$/gm, '');
  for (const match of prose.matchAll(/\[[^\]\n]*\]\(([^\s)]+)\)/g)) {
    const target = match[1];
    if (/^[a-z][a-z\d+.-]*:/i.test(target)) continue;
    const [path, fragment] = target.split('#');
    const destination = path ? resolve(dirname(file), decodeURIComponent(path)) : file;
    if (!existsSync(destination)) errors.push(`Missing link: ${target}`);
    else if (fragment && extname(destination) === '.md' && statSync(destination).isFile()) {
      if (!markdownAnchors(readFileSync(destination, 'utf8')).has(decodeURIComponent(fragment))) errors.push(`Missing anchor: ${target}`);
    }
  }
  return errors;
}

export function counterErrors(text, counts) {
  const matches = [...text.matchAll(/(\d+) documents, (\d+) links, (\d+) diagrams/g)];
  if (!matches.length) return ['Missing documentation counters'];
  return matches.flatMap((match) => {
    const [documents, links, diagrams] = match.slice(1).map(Number);
    return documents === counts.documents && links === counts.links && diagrams === counts.diagrams
      ? []
      : [`Stale counters: ${match[0]} (actual ${counts.documents} documents, ${counts.links} links, ${counts.diagrams} diagrams)`];
  });
}

export function backendCountErrors(documents) {
  const errors = [];
  const claims = [];
  for (const [label, text] of documents) {
    const unit = [...text.matchAll(/(\d+) unit\/architecture\/contract tests/g)].map((match) => Number(match[1]));
    const packaged = [...text.matchAll(/(\d+) packaged\s+integration tests/g)].map((match) => Number(match[1]));
    if (!unit.length) errors.push(`${label}: missing backend unit test count`);
    if (!packaged.length) errors.push(`${label}: missing packaged integration count`);
    if (new Set(unit).size > 1) errors.push(`${label}: inconsistent backend unit test counts`);
    if (new Set(packaged).size > 1) errors.push(`${label}: inconsistent packaged integration counts`);
    claims.push([label, unit[0], packaged[0]]);
  }
  const complete = claims.every(([, unit, packaged]) => unit !== undefined && packaged !== undefined);
  const distinct = new Set(claims.map(([, unit, packaged]) => `${unit}/${packaged}`));
  if (complete && distinct.size > 1) {
    errors.push(`backend test counts disagree: ${claims
      .map(([label, unit, packaged]) => `${label} ${unit} unit/${packaged} packaged`).join(', ')}`);
  }
  return errors;
}

function filesIn(directory) {
  return readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const path = resolve(directory, entry.name);
    return entry.isDirectory() ? filesIn(path) : [path];
  });
}

export async function checkDocuments() {
  const errors = [];
  const files = filesIn(docs);
  const markdown = [resolve(root, 'README.md'), ...files.filter((file) => extname(file) === '.md' && !file.endsWith('MASTER-BLUEPRINT.md'))];
  let links = 0;
  let diagrams = 0;
  mermaid.initialize({ startOnLoad: false, securityLevel: 'strict' });
  for (const file of markdown) {
    const text = readFileSync(file, 'utf8');
    const label = relative(root, file);
    errors.push(...linkErrors(file, text).map((error) => `${label}: ${error}`));
    links += [...text.matchAll(/\[[^\]\n]*\]\(([^\s)]+)\)/g)].length;
    for (const block of text.matchAll(/^```mermaid\s*\n([\s\S]*?)^```\s*$/gm)) {
      diagrams++;
      try { await mermaid.parse(block[1]); }
      catch (error) { errors.push(`${label}: Mermaid syntax: ${error.message}`); }
    }
  }
  const validErrors = manifestErrors(readJson('MODULE-MANIFEST-TEMPLATE.json'));
  errors.push(...validErrors.map((error) => `Valid fixture rejected: ${error}`));
  if (!manifestErrors(readJson('MODULE-MANIFEST-INVALID.json')).length) errors.push('Invalid fixture was accepted');
  const index = readFileSync(resolve(docs, 'README.md'), 'utf8');
  for (const file of files.filter((file) => /\.(md|json)$/.test(file) && file !== resolve(docs, 'README.md'))) {
    const path = relative(docs, file).replaceAll('\\', '/');
    if (!index.includes(`](${path})`)) errors.push(`Unindexed document: ${path}`);
  }
  const counts = { documents: files.length, links, diagrams };
  for (const name of ['CURRENT-STATE.md', 'IMPLEMENTATION-EVIDENCE-MATRIX.md']) {
    const text = readFileSync(resolve(docs, '15-FOUNDATION-9.5', name), 'utf8');
    errors.push(...counterErrors(text, counts).map((error) => `15-FOUNDATION-9.5/${name}: ${error}`));
  }
  errors.push(...backendCountErrors([
    ['README.md', readFileSync(resolve(root, 'README.md'), 'utf8')],
    ['15-FOUNDATION-9.5/CURRENT-STATE.md',
      readFileSync(resolve(docs, '15-FOUNDATION-9.5', 'CURRENT-STATE.md'), 'utf8')],
  ]));
  return { errors, documents: files.length, markdown: markdown.length, links, diagrams };
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const result = await checkDocuments();
  for (const error of result.errors) console.error(error);
  console.log(JSON.stringify({ ...result, errors: result.errors.length }, null, 2));
  process.exitCode = result.errors.length ? 1 : 0;
}
