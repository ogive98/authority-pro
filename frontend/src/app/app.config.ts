import { provideHttpClient } from '@angular/common/http';
import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { TitleStrategy, provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { routes } from './app.routes';
import { SessionService } from './core/auth/session.service';
import { I18nService } from './core/i18n/i18n.service';
import { LocalizedTitleStrategy } from './core/i18n/localized-title.strategy';
import { PreferencesService } from './core/preferences/preferences.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(),
    provideRouter(routes),
    provideTranslateService({ fallbackLang: 'fr' }),
    { provide: TitleStrategy, useClass: LocalizedTitleStrategy },
    provideAppInitializer(() => {
      inject(I18nService);
    }),
    provideAppInitializer(() => {
      inject(PreferencesService);
    }),
    provideAppInitializer(() => inject(SessionService).restore()),
  ],
};
