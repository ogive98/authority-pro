import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { SessionService } from '../../core/auth/session.service';
import { initI18nTesting, provideI18nTesting } from '../../core/i18n/i18n.testing';
import { ScopeService } from '../../core/scope/scope.service';
import { Shell } from './shell';

describe('Shell', () => {
  beforeEach(() => TestBed.resetTestingModule());

  it('logs out, clears the scope and returns to login', async () => {
    const session = {
      state: signal('authenticated'),
      profile: signal({
        userId: 'u',
        displayName: 'Opérateur',
        login: 'operator',
        memberships: [],
      }),
      login: vi.fn(),
      logout: vi.fn(async () => undefined),
      restore: vi.fn(),
    };
    const clear = vi.fn();
    TestBed.configureTestingModule({
      imports: [Shell],
      providers: [
        provideRouter([]),
        ...provideI18nTesting(),
        { provide: SessionService, useValue: session },
        {
          provide: ScopeService,
          useValue: {
            scope: signal(null).asReadonly(),
            status: signal('idle').asReadonly(),
            failure: signal(null).asReadonly(),
            select: vi.fn(),
            clear,
          },
        },
      ],
    });
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    initI18nTesting();
    const fixture = TestBed.createComponent(Shell);
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
    button.click();
    await fixture.whenStable();

    expect(session.logout).toHaveBeenCalledTimes(1);
    expect(clear).toHaveBeenCalledTimes(1);
    expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
  });

  it('renders the active scope label', () => {
    const session = {
      state: signal('authenticated'),
      profile: signal({ userId: 'u', displayName: 'Opérateur', login: 'operator', memberships: [] }),
      login: vi.fn(),
      logout: vi.fn(),
      restore: vi.fn(),
    };
    TestBed.configureTestingModule({
      imports: [Shell],
      providers: [
        provideRouter([]),
        ...provideI18nTesting(),
        { provide: SessionService, useValue: session },
        {
          provide: ScopeService,
          useValue: {
            scope: signal({
              tenantId: 't',
              tenantName: 'Tenant A',
              companyId: 'c',
              companyName: 'Société A1',
              authorizationRevision: 1,
              roles: [],
              permissions: [],
            }).asReadonly(),
            status: signal('ready').asReadonly(),
            failure: signal(null).asReadonly(),
            select: vi.fn(),
            clear: vi.fn(),
          },
        },
      ],
    });
    initI18nTesting();
    const fixture = TestBed.createComponent(Shell);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Tenant A — Société A1');
  });

  function mount(options: { language?: 'fr' | 'en' | 'ar'; logout?: () => Promise<void> } = {}) {
    const session = {
      state: signal('authenticated'),
      profile: signal({ userId: 'u', displayName: 'Opérateur', login: 'operator', memberships: [] }),
      login: vi.fn(),
      logout: vi.fn(options.logout ?? (async () => undefined)),
      restore: vi.fn(),
    };
    TestBed.configureTestingModule({
      imports: [Shell],
      providers: [
        provideRouter([]),
        ...provideI18nTesting(),
        { provide: SessionService, useValue: session },
        {
          provide: ScopeService,
          useValue: {
            scope: signal(null).asReadonly(),
            status: signal('idle').asReadonly(),
            failure: signal(null).asReadonly(),
            select: vi.fn(),
            clear: vi.fn(),
          },
        },
      ],
    });
    vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    initI18nTesting(options.language ?? 'fr');
    const fixture = TestBed.createComponent(Shell);
    fixture.detectChanges();
    return { fixture, element: fixture.nativeElement as HTMLElement, session };
  }

  it('offers a skip link into a focusable main landmark', () => {
    const { element } = mount();
    const skip = element.querySelector('a.skip') as HTMLAnchorElement;
    expect(skip.getAttribute('href')).toBe('#main');
    const main = element.querySelector('main#main') as HTMLElement;
    expect(main).not.toBeNull();
    expect(main.getAttribute('tabindex')).toBe('-1');
  });

  it('labels the navigation and the live scope region from the active language', () => {
    const { element } = mount({ language: 'en' });
    const nav = element.querySelector('nav') as HTMLElement;
    expect(nav.getAttribute('aria-label')).toBe('Main navigation');
    expect(nav.textContent).toContain('Overview');
    expect(nav.textContent).toContain('Inventory');
    expect(nav.textContent).toContain('Audit');
    expect(element.textContent).toContain('No active scope');
    const scope = element.querySelector('.scope') as HTMLElement;
    expect(scope.getAttribute('aria-live')).toBe('polite');
  });

  it('keeps the logout button disabled and single-shot while signing out', async () => {
    const { element, fixture, session } = mount({ logout: () => new Promise(() => undefined) });
    const button = element.querySelector('header button') as HTMLButtonElement;
    button.click();
    fixture.detectChanges();
    expect(session.logout).toHaveBeenCalledTimes(1);
    expect(button.disabled).toBe(true);
    expect(button.textContent).toContain('Déconnexion…');
    button.click();
    expect(session.logout).toHaveBeenCalledTimes(1);
  });
});
