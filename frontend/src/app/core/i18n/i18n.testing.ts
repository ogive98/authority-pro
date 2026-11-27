import { Provider } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { I18nService } from './i18n.service';
import { Language } from './languages';

export function provideI18nTesting(): Provider[] {
  return [provideTranslateService({ fallbackLang: 'fr' }), I18nService];
}

export function initI18nTesting(language: Language = 'fr'): I18nService {
  const service = TestBed.inject(I18nService);
  service.use(language);
  return service;
}
