import { DOCUMENT, Injectable, inject, signal } from '@angular/core';

export type ThemePreference = 'dark' | 'light';
export type DensityPreference = 'compact' | 'comfortable';

const STORAGE_KEY = 'authority.pro.preferences';

interface StoredPreferences {
  theme: ThemePreference;
  density: DensityPreference;
}

const DEFAULTS: StoredPreferences = { theme: 'dark', density: 'comfortable' };

@Injectable({ providedIn: 'root' })
export class PreferencesService {
  private readonly document = inject(DOCUMENT);
  private readonly themeSignal = signal<ThemePreference>(DEFAULTS.theme);
  private readonly densitySignal = signal<DensityPreference>(DEFAULTS.density);

  readonly theme = this.themeSignal.asReadonly();
  readonly density = this.densitySignal.asReadonly();

  constructor() {
    const stored = read();
    this.themeSignal.set(stored.theme);
    this.densitySignal.set(stored.density);
    this.apply();
  }

  setTheme(theme: ThemePreference): void {
    this.themeSignal.set(theme);
    this.apply();
    this.persist();
  }

  setDensity(density: DensityPreference): void {
    this.densitySignal.set(density);
    this.apply();
    this.persist();
  }

  private apply(): void {
    const root = this.document.documentElement;
    root.dataset['theme'] = this.themeSignal();
    root.dataset['density'] = this.densitySignal();
  }

  private persist(): void {
    const value: StoredPreferences = {
      theme: this.themeSignal(),
      density: this.densitySignal(),
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
    };
  } catch {
    return DEFAULTS;
  }
}
