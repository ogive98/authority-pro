import { DOCUMENT, Injectable, inject, signal } from '@angular/core';
import { isLanguage, Language } from '../i18n/languages';
import { I18nService } from '../i18n/i18n.service';

export type ThemePreference = 'dark' | 'light';
export type DensityPreference = 'compact' | 'comfortable';

const STORAGE_KEY = 'authority.pro.preferences';

interface StoredPreferences {
  theme: ThemePreference;
  density: DensityPreference;
  language: Language;
}

const DEFAULTS: StoredPreferences = { theme: 'dark', density: 'comfortable', language: 'fr' };

@Injectable({ providedIn: 'root' })
export class PreferencesService {
  private readonly document = inject(DOCUMENT);
  private readonly i18n = inject(I18nService);
  private readonly themeSignal = signal<ThemePreference>(DEFAULTS.theme);
  private readonly densitySignal = signal<DensityPreference>(DEFAULTS.density);

  readonly theme = this.themeSignal.asReadonly();
  readonly density = this.densitySignal.asReadonly();
  readonly language = this.i18n.language;

  constructor() {
    const stored = read();
    this.themeSignal.set(stored.theme);
    this.densitySignal.set(stored.density);
    this.applyTheme();
    this.i18n.use(stored.language);
  }

  setTheme(theme: ThemePreference): void {
    this.themeSignal.set(theme);
    this.applyTheme();
    this.persist();
  }

  setDensity(density: DensityPreference): void {
    this.densitySignal.set(density);
    this.applyTheme();
    this.persist();
  }

  setLanguage(language: Language): void {
    this.i18n.use(language);
    this.persist();
  }

  private applyTheme(): void {
    const root = this.document.documentElement;
    root.dataset['theme'] = this.themeSignal();
    root.dataset['density'] = this.densitySignal();
  }

  private persist(): void {
    const value: StoredPreferences = {
      theme: this.themeSignal(),
      density: this.densitySignal(),
      language: this.language(),
    };
    try {
      this.document.defaultView?.localStorage.setItem(STORAGE_KEY, JSON.stringify(value));
    } catch {
      // Storage can be unavailable; the session keeps the applied values.
    }
  }
}

function read(): StoredPreferences {
  try {
    const raw = globalThis.localStorage?.getItem(STORAGE_KEY);
    if (!raw) return DEFAULTS;
    const parsed = JSON.parse(raw) as Partial<StoredPreferences>;
    return {
      theme: parsed.theme === 'light' ? 'light' : 'dark',
      density: parsed.density === 'compact' ? 'compact' : 'comfortable',
      language: isLanguage(parsed.language) ? parsed.language : DEFAULTS.language,
    };
  } catch {
    return DEFAULTS;
  }
}
