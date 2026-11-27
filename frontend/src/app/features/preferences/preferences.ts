import { Component, computed, inject } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { TranslationKey } from '../../core/i18n/catalog';
import { I18nService } from '../../core/i18n/i18n.service';
import { LANGUAGES, Language } from '../../core/i18n/languages';
import {
  DensityPreference,
  PreferencesService,
  ThemePreference,
} from '../../core/preferences/preferences.service';

@Component({
  selector: 'app-preferences-page',
  imports: [TranslatePipe],
  templateUrl: './preferences.html',
  styleUrl: './preferences.css',
})
export class PreferencesPage {
  private readonly preferences = inject(PreferencesService);
  private readonly i18n = inject(I18nService);

  readonly languages = LANGUAGES;
  readonly theme = this.preferences.theme;
  readonly density = this.preferences.density;
  readonly language = this.preferences.language;
  readonly summary = computed(() => {
    const theme = this.theme() === 'light' ? 'preferences.themeNames.light' : 'preferences.themeNames.dark';
    const density =
      this.density() === 'compact' ? 'preferences.densityNames.compact' : 'preferences.densityNames.comfortable';
    const language = `preferences.languageNames.${this.language()}` as TranslationKey;
    return this.i18n.instant('preferences.summary', {
      theme: this.i18n.instant(theme),
      density: this.i18n.instant(density),
      language: this.i18n.instant(language),
    });
  });

  setTheme(theme: ThemePreference): void {
    this.preferences.setTheme(theme);
  }

  setDensity(density: DensityPreference): void {
    this.preferences.setDensity(density);
  }

  setLanguage(language: Language): void {
    this.preferences.setLanguage(language);
  }
}
