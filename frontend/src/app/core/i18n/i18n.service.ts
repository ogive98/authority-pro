import { DOCUMENT, Injectable, computed, inject, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { DICTIONARIES, TranslationKey } from './catalog';
import {
  DEFAULT_LANGUAGE,
  Direction,
  Language,
  LanguageOption,
  languageOption,
} from './languages';

@Injectable({ providedIn: 'root' })
export class I18nService {
  private readonly document = inject(DOCUMENT);
  private readonly translate = inject(TranslateService);
  private readonly languageSignal = signal<Language>(DEFAULT_LANGUAGE);

  readonly language = this.languageSignal.asReadonly();
  readonly option = computed<LanguageOption>(() => languageOption(this.language()));
  readonly direction = computed<Direction>(() => this.option().dir);
  readonly locale = computed(() => this.option().locale);

  constructor() {
    for (const language of Object.keys(DICTIONARIES) as Language[]) {
      this.translate.setTranslation(language, DICTIONARIES[language]);
    }
    this.use(DEFAULT_LANGUAGE);
  }

  use(language: Language): void {
    this.languageSignal.set(language);
    this.translate.use(language);
    const root = this.document.documentElement;
    root.lang = language;
    root.dir = languageOption(language).dir;
  }

  instant(key: TranslationKey, params: Record<string, unknown> = {}): string {
    return this.translate.instant(key, params);
  }
}
