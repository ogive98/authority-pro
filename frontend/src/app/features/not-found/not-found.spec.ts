import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it } from 'vitest';
import { initI18nTesting, provideI18nTesting } from '../../core/i18n/i18n.testing';
import { NotFoundPage } from './not-found';

function render(language: 'fr' | 'ar' = 'fr') {
  TestBed.configureTestingModule({
    imports: [NotFoundPage],
    providers: [provideRouter([]), ...provideI18nTesting()],
  });
  initI18nTesting(language);
  const fixture = TestBed.createComponent(NotFoundPage);
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

describe('NotFoundPage', () => {
  beforeEach(() => {
    TestBed.resetTestingModule();
    document.documentElement.removeAttribute('lang');
    document.documentElement.removeAttribute('dir');
  });

  it('renders the translated page without pretending the address exists', () => {
    const element = render();
    expect(element.textContent).toContain('AUTHORITY');
    expect(element.querySelector('h1')?.textContent).toContain('Page introuvable');
    expect(element.textContent).toContain('L’adresse demandée n’existe pas');
  });

  it('offers a return link to the application root', () => {
    const element = render();
    const link = Array.from(element.querySelectorAll('a')).find((candidate) =>
      candidate.textContent?.includes('Revenir à l’accueil'),
    ) as HTMLAnchorElement | undefined;
    expect(link?.getAttribute('href')).toBe('/');
  });

  it('renders right-to-left in Arabic', () => {
    const element = render('ar');
    expect(document.documentElement.dir).toBe('rtl');
    expect(document.documentElement.lang).toBe('ar');
    expect(element.querySelector('h1')?.textContent).toContain('الصفحة غير موجودة');
  });
});
