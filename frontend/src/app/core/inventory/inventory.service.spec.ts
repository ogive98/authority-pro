import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal, WritableSignal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { InventoryView, ScopedAccess } from '../models';
import { ScopeService } from '../scope/scope.service';
import { INVENTORY_PERMISSION, InventoryService } from './inventory.service';

const scope: ScopedAccess = {
  tenantId: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
  tenantName: 'Tenant A',
  companyId: 'cccccccc-cccc-4ccc-8ccc-cccccccccccc',
  companyName: 'Société A1',
  authorizationRevision: 3,
  roles: ['ROLE_OWNER'],
  permissions: [INVENTORY_PERMISSION],
};

const inventory: InventoryView = {
  buildRevision: 'rev-2026-11-26',
  modules: [
    {
      id: 'runtime',
      version: '0.1.0',
      digest: 'sha256:runtime',
      lifecycle: 'REGISTERED',
      availabilityReason: 'COMPILED_FIRST_PARTY',
      dependencies: [],
      capabilities: { provides: [], requires: [] },
      permissions: [{ id: 'platform.inventory.read', scope: 'control-plane' }],
      dataOwnership: [{ name: 'module_catalogue', scope: 'platform' }],
    },
    {
      id: 'identity',
      version: '0.1.0',
      digest: 'sha256:identity',
      lifecycle: 'REGISTERED',
      availabilityReason: 'COMPILED_FIRST_PARTY',
      dependencies: [{ id: 'runtime', version: '0.1.0' }],
      capabilities: { provides: [], requires: [] },
      permissions: [{ id: 'platform.inventory.read', scope: 'control-plane' }],
      dataOwnership: [{ name: 'operators', scope: 'platform' }],
    },
  ],
};

const tick = () => new Promise((resolve) => setTimeout(resolve, 0));

describe('InventoryService', () => {
  let service: InventoryService;
  let http: HttpTestingController;
  let active: WritableSignal<ScopedAccess | null>;

  beforeEach(() => {
    active = signal<ScopedAccess | null>(scope);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ScopeService,
          useValue: { scope: active.asReadonly(), select: () => undefined, clear: () => undefined },
        },
      ],
    });
    service = TestBed.inject(InventoryService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('never calls the API without the inventory permission', async () => {
    active.set({ ...scope, permissions: [] });
    service.load();
    await tick();
    http.expectNone('/api/v1/platform/inventory');
    expect(service.status()).toBe('denied');
    expect(service.inventory()).toBeNull();
  });

  it('sends the scope headers and exposes the real inventory', async () => {
    service.load();
    const request = http.expectOne('/api/v1/platform/inventory');
    expect(request.request.headers.get('X-Authority-Tenant')).toBe(scope.tenantId);
    expect(request.request.headers.get('X-Authority-Company')).toBe(scope.companyId);
    request.flush(inventory);
    await tick();
    expect(service.status()).toBe('ready');
    expect(service.inventory()?.buildRevision).toBe('rev-2026-11-26');
    expect(service.inventory()?.modules.length).toBe(2);
  });

  it('keeps a refused inventory distinct from a lost session', async () => {
    service.load();
    http
      .expectOne('/api/v1/platform/inventory')
      .flush({ code: 'AUTHORIZATION_DENIED' }, { status: 403, statusText: 'Forbidden' });
    await tick();
    expect(service.status()).toBe('denied');
    expect(service.inventory()).toBeNull();
  });

  it('maps a missing session to authentication', async () => {
    service.load();
    http
      .expectOne('/api/v1/platform/inventory')
      .flush({ code: 'AUTHENTICATION_REQUIRED' }, { status: 401, statusText: 'Unauthorized' });
    await tick();
    expect(service.status()).toBe('authentication');
  });

  it('maps 503 and network failures to unavailable', async () => {
    service.load();
    http
      .expectOne('/api/v1/platform/inventory')
      .flush({ code: 'INVENTORY_UNAVAILABLE' }, { status: 503, statusText: 'Service Unavailable' });
    await tick();
    expect(service.status()).toBe('unavailable');

    service.load();
    http.expectOne('/api/v1/platform/inventory').error(new ProgressEvent('error'));
    await tick();
    expect(service.status()).toBe('unavailable');
    expect(service.inventory()).toBeNull();
  });

  it('stays idle when no scope is active', async () => {
    active.set(null);
    service.load();
    await tick();
    http.expectNone('/api/v1/platform/inventory');
    expect(service.status()).toBe('idle');
  });
});
