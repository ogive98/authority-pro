import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it } from 'vitest';
import { initI18nTesting, provideI18nTesting } from '../../core/i18n/i18n.testing';
import { PreferencesService } from '../../core/preferences/preferences.service';
import { PreferencesPage } from './preferences';

function render() {
  TestBed.configureTestingModule({
    imports: [PreferencesPage],
    providers: [provideRouter([]), ...provideI18nTesting()],
  });
  initI18nTesting();
  const fixture = TestBed.createComponent(PreferencesPage);
  fixture.detectChanges();
  return fixture;
}

function radio(element: HTMLElement, name: string, value: string): HTMLInputElement {
  return element.querySelector(`input[name="${name}"][value="${value}"]`) as HTMLInputElement;
}

describe('PreferencesPage', () => {
  beforeEach(() => {
    TestBed.resetTestingModule();
    globalThis.localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    document.documentElement.removeAttribute('data-density');
    document.documentElement.removeAttribute('lang');
    document.documentElement.removeAttribute('dir');
  });

  it('reflects the current preferences', () => {
    const fixture = render();
    const element = fixture.nativeElement as HTMLElement;
    expect(radio(element, 'theme', 'dark').checked).toBe(true);
    expect(radio(element, 'theme', 'light').checked).toBe(false);
    expect(radio(element, 'density', 'comfortable').checked).toBe(true);
    expect(radio(element, 'language', 'fr').checked).toBe(true);
    expect(element.textContent).toContain('ne confèrent aucun droit');
    expect(element.textContent).toContain('Thème sombre, densité confortable, langue français');
  });

  it('applies the light theme immediately and persists it', () => {
    const fixture = render();
    const element = fixture.nativeElement as HTMLElement;
    const light = radio(element, 'theme', 'light');
    light.checked = true;
    light.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(document.documentElement.dataset['theme']).toBe('light');
    expect(TestBed.inject(PreferencesService).theme()).toBe('light');
    expect(element.textContent).toContain('Thème clair, densité confortable');
    expect(globalThis.localStorage.getItem('authority.pro.preferences')).toContain('"theme":"light"');
  });

  it('applies the compact density immediately', () => {
    const fixture = render();
    const element = fixture.nativeElement as HTMLElement;
    const compact = radio(element, 'density', 'compact');
    compact.checked = true;
    compact.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(document.documentElement.dataset['density']).toBe('compact');
    expect(TestBed.inject(PreferencesService).density()).toBe('compact');
    expect(element.textContent).toContain('densité compacte');
  });

  it('switches the language, direction and copy immediately', () => {
    const fixture = render();
    const element = fixture.nativeElement as HTMLElement;
    const arabic = radio(element, 'language', 'ar');
    arabic.checked = true;
    arabic.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(TestBed.inject(PreferencesService).language()).toBe('ar');
    expect(document.documentElement.lang).toBe('ar');
    expect(document.documentElement.dir).toBe('rtl');
    expect(element.textContent).toContain('التفضيلات');
    expect(globalThis.localStorage.getItem('authority.pro.preferences')).toContain('"language":"ar"');
  });

  it('returns to a left-to-right layout when switching back to French', () => {
    globalThis.localStorage.setItem(
      'authority.pro.preferences',
      JSON.stringify({ theme: 'dark', density: 'comfortable', language: 'ar' }),
    );
    const fixture = render();
    const element = fixture.nativeElement as HTMLElement;
    expect(document.documentElement.dir).toBe('rtl');
    const french = radio(element, 'language', 'fr');
    french.checked = true;
    french.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(document.documentElement.lang).toBe('fr');
    expect(document.documentElement.dir).toBe('ltr');
    expect(element.textContent).toContain('Préférences');
  });
});
