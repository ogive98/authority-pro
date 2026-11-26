import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { INVENTORY_PERMISSION, InventoryService, InventoryStatus } from '../../core/inventory/inventory.service';
import { ApiFailure } from '../../core/api/problem';
import { InventoryView, ScopedAccess } from '../../core/models';
import { ScopeService } from '../../core/scope/scope.service';
import { OverviewPage } from './overview';

const access: ScopedAccess = {
  tenantId: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
  tenantName: 'Tenant A',
  companyId: 'cccccccc-cccc-4ccc-8ccc-cccccccccccc',
  companyName: 'Société A1',
  authorizationRevision: 3,
  roles: ['ROLE_OWNER'],
  permissions: ['audit.read.tenant'],
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
      permissions: [
        { id: 'platform.inventory.read', scope: 'control-plane' },
        { id: 'platform.audit.read', scope: 'control-plane' },
      ],
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
      permissions: [{ id: 'platform.audit.read', scope: 'control-plane' }],
      dataOwnership: [{ name: 'operators', scope: 'platform' }],
    },
  ],
};

interface State {
  status?: InventoryStatus;
  inventory?: InventoryView | null;
  failure?: ApiFailure | null;
}

function render(scope: ScopedAccess | null, state: State = {}) {
  const load = vi.fn();
  TestBed.configureTestingModule({
    imports: [OverviewPage],
    providers: [
      provideRouter([]),
      {
        provide: ScopeService,
        useValue: {
          scope: signal(scope).asReadonly(),
          status: signal(scope ? 'ready' : 'idle').asReadonly(),
          failure: signal(null).asReadonly(),
          select: () => undefined,
          clear: () => undefined,
        },
      },
      {
        provide: InventoryService,
        useValue: {
          inventory: signal(state.inventory ?? null).asReadonly(),
          status: signal(state.status ?? 'idle').asReadonly(),
          failure: signal(state.failure ?? null).asReadonly(),
          load,
        },
      },
    ],
  });
  const fixture = TestBed.createComponent(OverviewPage);
  fixture.detectChanges();
  return { fixture, load };
}

describe('OverviewPage', () => {
  beforeEach(() => TestBed.resetTestingModule());

  it('shows only data returned by the bootstrap', () => {
    const { fixture } = render(access, { status: 'denied' });
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Tenant A');
    expect(text).toContain('Société A1');
    expect(text).toContain('ROLE_OWNER');
    expect(text).toContain('audit.read.tenant');
    expect(text).toContain('3');
  });

  it('never fetches or number-crunches inventory without the permission', () => {
    const { fixture, load } = render(access, { status: 'denied' });
    const text = fixture.nativeElement.textContent as string;
    expect(load).toHaveBeenCalledTimes(1);
    expect(text).toContain(INVENTORY_PERMISSION);
    expect(text).toContain('non autorisé');
    expect(text).not.toContain('Chargement de l’inventaire');
    expect(text).not.toContain('Modules compilés');
  });

  it('renders the real inventory once the permission is granted', () => {
    const { fixture } = render(
      { ...access, permissions: [...access.permissions, INVENTORY_PERMISSION] },
      { status: 'ready', inventory },
    );
    const text = fixture.nativeElement.textContent as string;
    const values = Array.from(fixture.nativeElement.querySelectorAll('.metric .value')).map(
      (value) => (value as HTMLElement).textContent?.trim(),
    );
    expect(text).toContain('rev-2026-11-26');
    expect(values).toEqual(['2', '2', '2']);
    expect(text).toContain('runtime');
    expect(text).toContain('identity');
    expect(text).toContain('REGISTERED');
  });

  it('keeps an unavailable inventory honest and offers a retry', () => {
    const { fixture, load } = render(
      { ...access, permissions: [...access.permissions, INVENTORY_PERMISSION] },
      { status: 'unavailable' },
    );
    expect(fixture.nativeElement.textContent).toContain('Inventaire indisponible');
    const retry = Array.from(fixture.nativeElement.querySelectorAll('button')).find(
      (button) => (button as HTMLElement).textContent?.trim() === 'Réessayer',
    ) as HTMLButtonElement | undefined;
    expect(retry).toBeDefined();
    retry?.click();
    expect(load).toHaveBeenCalledTimes(2);
  });

  it('surfaces the trace reference on an unexpected failure', () => {
    const { fixture } = render(
      { ...access, permissions: [...access.permissions, INVENTORY_PERMISSION] },
      {
        status: 'error',
        failure: {
          kind: 'unknown',
          status: 500,
          code: 'INTERNAL_ERROR',
          traceId: 'trace-42',
          message: 'Une erreur inattendue est survenue.',
        },
      },
    );
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('n’a pas pu être chargé');
    expect(text).toContain('trace-42');
  });

  it('keeps the audit note deferred to P5', () => {
    const { fixture } = render(access, { status: 'denied' });
    expect(fixture.nativeElement.textContent).toContain('L’audit sera branché au lot P5');
  });

  it('offers a safe path when no scope is active', () => {
    const { fixture } = render(null);
    expect(fixture.nativeElement.textContent).toContain('Aucune portée active');
  });
});
