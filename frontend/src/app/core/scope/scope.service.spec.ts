import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { describe, expect, it, beforeEach, afterEach } from 'vitest';
import { ScopedAccess } from '../models';
import { ScopeService } from './scope.service';

const scopeA: ScopedAccess = {
  tenantId: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
  tenantName: 'Tenant A',
  companyId: null,
  companyName: null,
  authorizationRevision: 1,
  roles: ['ROLE_OWNER'],
  permissions: ['audit.read.tenant'],
};

const scopeB: ScopedAccess = { ...scopeA, tenantId: 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', tenantName: 'Tenant B' };

const tick = () => new Promise((resolve) => setTimeout(resolve, 0));

describe('ScopeService', () => {
  let service: ScopeService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ScopeService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('validates a tenant scope and exposes roles and permissions', async () => {
    service.select({ tenantId: scopeA.tenantId });
    const request = http.expectOne('/api/v1/access/bootstrap');
    expect(request.request.headers.get('X-Authority-Tenant')).toBe(scopeA.tenantId);
    expect(request.request.headers.has('X-Authority-Company')).toBe(false);
    request.flush(scopeA);
    await tick();
    expect(service.status()).toBe('ready');
    expect(service.scope()?.tenantName).toBe('Tenant A');
    expect(service.scope()?.permissions).toEqual(['audit.read.tenant']);
  });

  it('sends the company header only when a company is selected', async () => {
    service.select({ tenantId: scopeA.tenantId, companyId: 'cccccccc-cccc-4ccc-8ccc-cccccccccccc' });
    const request = http.expectOne('/api/v1/access/bootstrap');
    expect(request.request.headers.get('X-Authority-Company')).toBe('cccccccc-cccc-4ccc-8ccc-cccccccccccc');
    request.flush(scopeA);
    await tick();
  });

  it('keeps the session on a refused scope', async () => {
    service.select({ tenantId: scopeA.tenantId });
    http
      .expectOne('/api/v1/access/bootstrap')
      .flush({ code: 'AUTHORIZATION_DENIED' }, { status: 403, statusText: 'Forbidden' });
    await tick();
    expect(service.status()).toBe('denied');
    expect(service.scope()).toBeNull();
  });

  it('ignores a late response after another selection', async () => {
    service.select({ tenantId: scopeA.tenantId });
    const first = http.expectOne('/api/v1/access/bootstrap');
    service.select({ tenantId: scopeB.tenantId });
    const second = http.expectOne('/api/v1/access/bootstrap');
    second.flush(scopeB);
    await tick();
    expect(service.scope()?.tenantName).toBe('Tenant B');
    try {
      first.flush(scopeA);
    } catch {
      // The first request is cancelled by switchMap; a late flush must never override.
    }
    await tick();
    expect(service.scope()?.tenantName).toBe('Tenant B');
    expect(service.status()).toBe('ready');
  });

  it('clears the active scope', async () => {
    service.select({ tenantId: scopeA.tenantId });
    http.expectOne('/api/v1/access/bootstrap').flush(scopeA);
    await tick();
    service.clear();
    await tick();
    expect(service.scope()).toBeNull();
    expect(service.status()).toBe('idle');
  });
});
