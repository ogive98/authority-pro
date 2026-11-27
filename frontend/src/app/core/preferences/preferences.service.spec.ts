import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';
import { provideI18nTesting } from '../i18n/i18n.testing';
import { PreferencesService } from './preferences.service';

const STORAGE_KEY = 'authority.pro.preferences';

describe('PreferencesService', () => {
  beforeEach(() => {
    TestBed.resetTestingModule();
    globalThis.localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    document.documentElement.removeAttribute('data-density');
    document.documentElement.removeAttribute('lang');
    document.documentElement.removeAttribute('dir');
    TestBed.configureTestingModule({ providers: [...provideI18nTesting()] });
  });

  it('applies dark and comfortable defaults on the document', () => {
    const service = TestBed.inject(PreferencesService);
    expect(service.theme()).toBe('dark');
    expect(service.density()).toBe('comfortable');
    expect(document.documentElement.dataset['theme']).toBe('dark');
    expect(document.documentElement.dataset['density']).toBe('comfortable');
  });

  it('restores a valid stored choice before the first render', () => {
    globalThis.localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({ theme: 'light', density: 'compact' }),
    );
    const service = TestBed.inject(PreferencesService);
    expect(service.theme()).toBe('light');
    expect(service.density()).toBe('compact');
    expect(document.documentElement.dataset['theme']).toBe('light');
    expect(document.documentElement.dataset['density']).toBe('compact');
  });

  it('ignores unknown stored values', () => {
    globalThis.localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({ theme: 'neon', density: 'huge' }),
    );
    const service = TestBed.inject(PreferencesService);
    expect(service.theme()).toBe('dark');
    expect(service.density()).toBe('comfortable');
  });

  it('applies and persists a theme change', () => {
    const service = TestBed.inject(PreferencesService);
    service.setTheme('light');
    expect(document.documentElement.dataset['theme']).toBe('light');
    expect(JSON.parse(globalThis.localStorage.getItem(STORAGE_KEY) ?? '{}')).toEqual({
      theme: 'light',
      density: 'comfortable',
      language: 'fr',
    });
  });

  it('applies and persists a density change without losing the theme', () => {
    const service = TestBed.inject(PreferencesService);
    service.setTheme('light');
    service.setDensity('compact');
    expect(document.documentElement.dataset['density']).toBe('compact');
    expect(JSON.parse(globalThis.localStorage.getItem(STORAGE_KEY) ?? '{}')).toEqual({
      theme: 'light',
      density: 'compact',
      language: 'fr',
    });
  });

  it('restores the stored language and applies its reading direction', () => {
    globalThis.localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({ theme: 'dark', density: 'comfortable', language: 'ar' }),
    );
    const service = TestBed.inject(PreferencesService);
    expect(service.language()).toBe('ar');
    expect(document.documentElement.lang).toBe('ar');
    expect(document.documentElement.dir).toBe('rtl');
  });

  it('applies and persists a language change without losing the theme', () => {
    const service = TestBed.inject(PreferencesService);
    service.setTheme('light');
    service.setLanguage('en');
    expect(service.language()).toBe('en');
    expect(document.documentElement.lang).toBe('en');
    expect(document.documentElement.dir).toBe('ltr');
    expect(JSON.parse(globalThis.localStorage.getItem(STORAGE_KEY) ?? '{}')).toEqual({
      theme: 'light',
      density: 'comfortable',
      language: 'en',
    });
  });

  it('falls back to French when the stored language is unknown', () => {
    globalThis.localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({ theme: 'dark', density: 'comfortable', language: 'xx' }),
    );
    const service = TestBed.inject(PreferencesService);
    expect(service.language()).toBe('fr');
  });
});
