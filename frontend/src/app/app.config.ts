import { provideHttpClient } from '@angular/common/http';
import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { SessionService } from './core/auth/session.service';
import { PreferencesService } from './core/preferences/preferences.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(),
    provideRouter(routes),
    provideAppInitializer(() => {
      inject(PreferencesService);
    }),
    provideAppInitializer(() => inject(SessionService).restore()),
  ],
};
