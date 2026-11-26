import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, RouterStateSnapshot } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { SessionService } from './session.service';
import { ScopeService } from '../scope/scope.service';
import { authGuard } from './auth.guard';
import { scopeGuard } from '../scope/scope.guard';

const route = {} as ActivatedRouteSnapshot;
const state = { url: '/app/overview' } as RouterStateSnapshot;

describe('authGuard', () => {
  beforeEach(() => TestBed.resetTestingModule());

  it('allows an authenticated session', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: SessionService, useValue: { restore: vi.fn(async () => true) } },
      ],
    });
    const result = await TestBed.runInInjectionContext(() => authGuard(route, state));
    expect(result).toBe(true);
  });

  it('redirects to login with a return URL', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: SessionService, useValue: { restore: vi.fn(async () => false) } },
      ],
    });
    const result = await TestBed.runInInjectionContext(() => authGuard(route, state));
    expect(result.toString()).toBe('/login?returnUrl=%2Fapp%2Foverview');
  });
});

describe('scopeGuard', () => {
  beforeEach(() => TestBed.resetTestingModule());

  it('redirects to the scope page without an active scope', () => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: ScopeService, useValue: { scope: () => null } }],
    });
    const result = TestBed.runInInjectionContext(() => scopeGuard(route, state));
    expect(result.toString()).toBe('/app/tenants');
  });

  it('allows an active scope', () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: ScopeService, useValue: { scope: () => ({ tenantId: 't' }) } },
      ],
    });
    const result = TestBed.runInInjectionContext(() => scopeGuard(route, state));
    expect(result).toBe(true);
  });
});
