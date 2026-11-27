import { Component, computed, inject } from '@angular/core';
import {
  DensityPreference,
  PreferencesService,
  ThemePreference,
} from '../../core/preferences/preferences.service';

@Component({
  selector: 'app-preferences-page',
  templateUrl: './preferences.html',
  styleUrl: './preferences.css',
})
export class PreferencesPage {
  private readonly preferences = inject(PreferencesService);

  readonly theme = this.preferences.theme;
  readonly density = this.preferences.density;
  readonly summary = computed(() => {
    const theme = this.theme() === 'light' ? 'Thème clair' : 'Thème sombre';
    const density = this.density() === 'compact' ? 'densité compacte' : 'densité confortable';
    return `${theme}, ${density}. Enregistré sur cet appareil.`;
  });

  setTheme(theme: ThemePreference): void {
    this.preferences.setTheme(theme);
  }

  setDensity(density: DensityPreference): void {
    this.preferences.setDensity(density);
  }
}
