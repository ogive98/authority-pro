import { Injectable, effect, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';
import { TranslationKey } from './catalog';
import { I18nService } from './i18n.service';

@Injectable({ providedIn: 'root' })
export class LocalizedTitleStrategy extends TitleStrategy {
  private readonly pageTitle = inject(Title);
  private readonly i18n = inject(I18nService);
  private current: RouterStateSnapshot | null = null;

  constructor() {
    super();
    effect(() => {
      this.i18n.language();
      if (this.current) this.apply(this.current);
    });
  }

  override updateTitle(snapshot: RouterStateSnapshot): void {
    this.current = snapshot;
    this.apply(snapshot);
  }

  private apply(snapshot: RouterStateSnapshot): void {
    const key = this.titleKey(snapshot);
    const label = key ? this.i18n.instant(key) : null;
    this.pageTitle.setTitle(label ? `${label} — AUTHORITY` : 'AUTHORITY');
  }

  private titleKey(snapshot: RouterStateSnapshot): TranslationKey | null {
    let route = snapshot.root;
    let key: string | null = null;
    while (route.firstChild) {
      route = route.firstChild;
      const candidate = route.data['titleKey'];
      if (typeof candidate === 'string') key = candidate;
    }
    return key as TranslationKey | null;
  }
}
