import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { OwnProfile, ScopedAccess } from '../../core/models';
import { SessionService } from '../../core/auth/session.service';
import { ScopeService, ScopeStatus } from '../../core/scope/scope.service';
import { TenantsPage } from './tenants';

const profile: OwnProfile = {
  userId: '11111111-1111-4111-8111-111111111111',
  displayName: 'Opérateur',
  login: 'operator',
  memberships: [
    {
      tenantId: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
      tenantName: 'Tenant A',
      roles: ['ROLE_OWNER'],
      companies: [{ companyId: 'cccccccc-cccc-4ccc-8ccc-cccccccccccc', name: 'Société A1' }],
    },
  ],
};

function setup(profileValue: OwnProfile | null = profile, status: ScopeStatus = 'idle') {
  const scopeSignal = signal<ScopedAccess | null>(null);
  const statusSignal = signal<ScopeStatus>(status);
  const failureSignal = signal(null);
  const select = vi.fn();
  const clear = vi.fn();
  const session = {
    state: signal('authenticated'),
    profile: signal(profileValue),
    login: vi.fn(),
    logout: vi.fn(),
    restore: vi.fn(),
  };
  TestBed.configureTestingModule({
    imports: [TenantsPage],
    providers: [
      provideRouter([]),
      { provide: SessionService, useValue: session },
      {
        provide: ScopeService,
        useValue: {
          scope: scopeSignal.asReadonly(),
          status: statusSignal.asReadonly(),
          failure: failureSignal.asReadonly(),
          select,
          clear,
        },
      },
    ],
  });
  const router = TestBed.inject(Router);
  vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
  const fixture = TestBed.createComponent(TenantsPage);
  fixture.detectChanges();
  return { fixture, select, scopeSignal, statusSignal };
}

describe('TenantsPage', () => {
  beforeEach(() => TestBed.resetTestingModule());

  it('lists only the memberships returned by the profile', async () => {
    const { fixture } = setup();
    await fixture.whenStable();
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Tenant A');
    expect(text).toContain('ROLE_OWNER');
    expect(text).toContain('Société A1');
  });

  it('selects a tenant scope without a company header', async () => {
    const { fixture, select } = setup();
    const buttons = fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>;
    buttons[0].click();
    expect(select).toHaveBeenCalledWith({ tenantId: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa' });
  });

  it('selects a company scope', async () => {
    const { fixture, select } = setup();
    const buttons = fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>;
    buttons[1].click();
    expect(select).toHaveBeenCalledWith({
      tenantId: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
      companyId: 'cccccccc-cccc-4ccc-8ccc-cccccccccccc',
    });
  });

  it('shows an explicit empty state when no membership exists', async () => {
    const { fixture } = setup({ ...profile, memberships: [] });
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Aucune portée autorisée');
  });

  it('keeps the session visible when a scope is refused', async () => {
    const { fixture } = setup(profile, 'denied');
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Cette portée est refusée');
    expect(fixture.nativeElement.textContent).toContain('Tenant A');
  });

  it('disables choices while validating', async () => {
    const { fixture } = setup(profile, 'loading');
    await fixture.whenStable();
    const buttons = fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>;
    expect(buttons.length).toBeGreaterThan(0);
    expect(buttons[0].disabled).toBe(true);
  });
});
