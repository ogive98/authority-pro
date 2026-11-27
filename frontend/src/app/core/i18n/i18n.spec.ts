import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';
import { TranslationKey } from './catalog';
import { ar } from './catalogs/ar';
import { en } from './catalogs/en';
import { fr } from './catalogs/fr';
import { it as itCatalog } from './catalogs/it';
import { I18nService } from './i18n.service';
import { LANGUAGES } from './languages';
import { initI18nTesting, provideI18nTesting } from './i18n.testing';

type Catalogue = { readonly [key: string]: string | Catalogue };

function flatten(node: Catalogue, prefix = ''): Map<string, string> {
  const leaves = new Map<string, string>();
  for (const [key, value] of Object.entries(node)) {
    const path = prefix ? `${prefix}.${key}` : key;
    if (typeof value === 'string') {
      leaves.set(path, value);
    } else {
      for (const [nested, text] of flatten(value, path)) {
        leaves.set(nested, text);
      }
    }
  }
  return leaves;
}

function placeholders(text: string): string[] {
  return Array.from(text.matchAll(/\{\{\s*(\w+)\s*\}\}/g), (match) => match[1]).sort();
}

describe('Translation catalogues', () => {
  const source = flatten(fr as unknown as Catalogue);

  it.each([
    ['en', en],
    ['it', itCatalog],
    ['ar', ar],
  ] as const)('%s covers exactly the French keys', (_code, catalogue) => {
    const translated = flatten(catalogue as unknown as Catalogue);
    expect([...translated.keys()].sort()).toEqual([...source.keys()].sort());
  });

  it.each([
    ['en', en],
    ['it', itCatalog],
    ['ar', ar],
  ] as const)('%s keeps every interpolation and never ships an empty string', (_code, catalogue) => {
    const translated = flatten(catalogue as unknown as Catalogue);
    for (const [key, text] of translated) {
      expect(text.trim(), key).not.toBe('');
      expect(placeholders(text), key).toEqual(placeholders(source.get(key) ?? ''));
    }
  });

  it('declares exactly the four supported languages', () => {
    expect(LANGUAGES.map((option) => option.code)).toEqual(['fr', 'en', 'it', 'ar']);
    expect(LANGUAGES.filter((option) => option.dir === 'rtl').map((option) => option.code)).toEqual([
      'ar',
    ]);
  });
});

describe('I18nService', () => {
  beforeEach(() => {
    TestBed.resetTestingModule();
    document.documentElement.removeAttribute('lang');
    document.documentElement.removeAttribute('dir');
  });

  function setup() {
    TestBed.configureTestingModule({ providers: [...provideI18nTesting()] });
    return initI18nTesting();
  }

  it('starts in French with a left-to-right layout', () => {
    const service = setup();
    expect(service.language()).toBe('fr');
    expect(service.direction()).toBe('ltr');
    expect(service.locale()).toBe('fr-FR');
    expect(document.documentElement.lang).toBe('fr');
    expect(document.documentElement.dir).toBe('ltr');
  });

  it('switches the document to Arabic right-to-left', () => {
    const service = setup();
    service.use('ar');
    expect(service.language()).toBe('ar');
    expect(service.direction()).toBe('rtl');
    expect(document.documentElement.lang).toBe('ar');
    expect(document.documentElement.dir).toBe('rtl');
  });

  it('interpolates the trace reference in the active language', () => {
    const service = setup();
    expect(service.instant('common.reference', { trace: 'trace-9' })).toBe('Référence : trace-9');
    service.use('en');
    expect(service.instant('common.reference', { trace: 'trace-9' })).toBe('Reference: trace-9');
  });

  it('registers every dictionary so switching never leaves a raw key', () => {
    const service = setup();
    for (const option of LANGUAGES) {
      service.use(option.code);
      for (const key of flatten(fr as unknown as Catalogue).keys()) {
        const translated = service.instant(key as TranslationKey);
        expect(translated, `${option.code}:${key}`).not.toBe(key);
        expect(translated.trim(), `${option.code}:${key}`).not.toBe('');
      }
    }
  });
});
