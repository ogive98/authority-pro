import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiFailure } from '../../core/api/problem';
import { INVENTORY_PERMISSION, InventoryService, InventoryStatus } from '../../core/inventory/inventory.service';
import { InventoryView, ModuleView, ScopedAccess } from '../../core/models';
import { ScopeService } from '../../core/scope/scope.service';
import { InventoryPage } from './inventory';

const access: ScopedAccess = {
  tenantId: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
  tenantName: 'Tenant A',
  companyId: null,
  companyName: null,
  authorizationRevision: 5,
  roles: ['ROLE_OWNER'],
  permissions: [INVENTORY_PERMISSION, 'audit.read.tenant'],
};

function module(id: string, overrides: Partial<ModuleView> = {}): ModuleView {
  return {
    id,
    version: '0.1.0',
    digest: `sha256:${id}`,
    lifecycle: 'REGISTERED',
    availabilityReason: 'COMPILED_FIRST_PARTY',
    dependencies: [],
    capabilities: { provides: [], requires: [] },
    permissions: [{ id: `platform.${id}.read`, scope: 'control-plane' }],
    dataOwnership: [{ name: `${id}_data`, scope: 'platform' }],
    ...overrides,
  };
}

function inventory(modules: ModuleView[]): InventoryView {
  return { buildRevision: 'rev-p4', modules };
}

interface State {
  status?: InventoryStatus;
  modules?: ModuleView[];
  failure?: ApiFailure | null;
}

function render(state: State = {}, scope: ScopedAccess | null = access) {
  const load = vi.fn();
  TestBed.configureTestingModule({
    imports: [InventoryPage],
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
          inventory: signal(state.modules ? inventory(state.modules) : null).asReadonly(),
          status: signal(state.status ?? 'ready').asReadonly(),
          failure: signal(state.failure ?? null).asReadonly(),
          load,
        },
      },
    ],
  });
  const fixture = TestBed.createComponent(InventoryPage);
  fixture.detectChanges();
  return { fixture, load, element: fixture.nativeElement as HTMLElement };
}

function rowIds(element: HTMLElement): string[] {
  return Array.from(element.querySelectorAll('tbody tr:not(.detail-row) th .id')).map(
    (cell) => (cell as HTMLElement).textContent?.trim() ?? '',
  );
}

function buttonByText(element: HTMLElement, label: string): HTMLButtonElement {
  const button = Array.from(element.querySelectorAll('button')).find(
    (candidate) => (candidate as HTMLElement).textContent?.trim() === label,
  );
  if (!button) throw new Error(`Bouton introuvable : ${label}`);
  return button as HTMLButtonElement;
}

describe('InventoryPage', () => {
  beforeEach(() => TestBed.resetTestingModule());

  it('names the missing permission without rendering the table', () => {
    const { element, load } = render(
      { status: 'denied', modules: [module('runtime')] },
      { ...access, permissions: ['audit.read.tenant'] },
    );
    expect(load).toHaveBeenCalledTimes(1);
    expect(element.textContent).toContain(INVENTORY_PERMISSION);
    expect(element.textContent).toContain('non autorisé');
    expect(element.querySelector('table')).toBeNull();
  });

  it('sorts by identifier ascending by default and keeps ties stable', () => {
    const { element } = render({
      modules: [module('zeta', { version: '1.0.0' }), module('alpha', { version: '1.0.0' })],
    });
    expect(rowIds(element)).toEqual(['alpha', 'zeta']);
    const idHeader = Array.from(element.querySelectorAll('th')).find((th) =>
      (th.textContent ?? '').includes('Module'),
    );
    expect(idHeader?.getAttribute('aria-sort')).toBe('ascending');
  });

  it('toggles a version sort while preserving the original order of equal versions', () => {
    const { fixture, element } = render({
      modules: [
        module('zeta', { version: '1.0.0' }),
        module('alpha', { version: '1.0.0' }),
        module('beta', { version: '0.9.0' }),
      ],
    });
    const versionHeader = () => {
      const header = Array.from(element.querySelectorAll('th')).find((th) =>
        (th.textContent ?? '').includes('Version'),
      );
      return header as HTMLElement;
    };
    buttonByText(versionHeader(), 'Version').click();
    fixture.detectChanges();
    expect(rowIds(element)).toEqual(['beta', 'zeta', 'alpha']);
    expect(versionHeader().getAttribute('aria-sort')).toBe('ascending');
    buttonByText(versionHeader(), 'Version').click();
    fixture.detectChanges();
    expect(rowIds(element)).toEqual(['zeta', 'alpha', 'beta']);
    expect(versionHeader().getAttribute('aria-sort')).toBe('descending');
  });

  it('filters by text and by lifecycle, and resets the page', () => {
    const { fixture, element } = render({
      modules: [
        module('payments', { lifecycle: 'REGISTERED' }),
        module('identity', { lifecycle: 'REGISTERED' }),
        module('archive', { lifecycle: 'DEPRECATED' }),
      ],
    });
    const filter = element.querySelector('#inventory-filter') as HTMLInputElement;
    filter.value = 'ident';
    filter.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(rowIds(element)).toEqual(['identity']);
    expect(element.textContent).toContain('1 module');

    filter.value = '';
    filter.dispatchEvent(new Event('input'));
    const lifecycle = element.querySelector('#inventory-lifecycle') as HTMLSelectElement;
    lifecycle.value = 'DEPRECATED';
    lifecycle.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(rowIds(element)).toEqual(['archive']);
  });

  it('bounds pagination to ten rows per page', () => {
    const modules = Array.from({ length: 12 }, (_, index) =>
      module(`m${String(index).padStart(2, '0')}`),
    );
    const { fixture, element } = render({ modules });
    expect(rowIds(element)).toHaveLength(10);
    expect(element.textContent).toContain('Page 1 sur 2');
    buttonByText(element, 'Suivant').click();
    fixture.detectChanges();
    expect(rowIds(element)).toEqual(['m10', 'm11']);
    expect(buttonByText(element, 'Suivant').disabled).toBe(true);
  });

  it('opens a detail panel from the keyboard and closes it with Escape', () => {
    const { fixture, element } = render({
      modules: [module('runtime', { digest: 'sha256:runtime-digest' })],
    });
    const toggle = buttonByText(element, 'Détail');
    expect(toggle.getAttribute('aria-expanded')).toBe('false');
    toggle.focus();
    toggle.click();
    fixture.detectChanges();
    expect(buttonByText(element, 'Fermer').getAttribute('aria-expanded')).toBe('true');
    expect(element.textContent).toContain('sha256:runtime-digest');

    const scroll = element.querySelector('.table-scroll') as HTMLElement;
    scroll.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    fixture.detectChanges();
    expect(element.querySelector('.detail')).toBeNull();
    expect(buttonByText(element, 'Détail').getAttribute('aria-expanded')).toBe('false');
  });

  it('copies the digest and reports the result in text', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true });
    const { fixture, element } = render({
      modules: [module('runtime', { digest: 'sha256:runtime-digest' })],
    });
    buttonByText(element, 'Détail').click();
    fixture.detectChanges();
    buttonByText(element, 'Copier le digest').click();
    await new Promise((resolve) => setTimeout(resolve, 0));
    fixture.detectChanges();
    expect(writeText).toHaveBeenCalledWith('sha256:runtime-digest');
    expect(element.textContent).toContain('Digest copié.');
  });

  it('reports a clipboard failure without pretending to copy', async () => {
    Object.defineProperty(navigator, 'clipboard', {
      value: { writeText: vi.fn().mockRejectedValue(new Error('denied')) },
      configurable: true,
    });
    const { fixture, element } = render({ modules: [module('runtime')] });
    buttonByText(element, 'Détail').click();
    fixture.detectChanges();
    buttonByText(element, 'Copier le digest').click();
    await new Promise((resolve) => setTimeout(resolve, 0));
    fixture.detectChanges();
    expect(element.textContent).toContain('Copie impossible');
  });

  it('keeps an unavailable inventory honest and retries', () => {
    const { element, load } = render({ status: 'unavailable' });
    expect(element.textContent).toContain('Inventaire indisponible');
    buttonByText(element, 'Réessayer').click();
    expect(load).toHaveBeenCalledTimes(2);
  });

  it('shows an empty state when the inventory has no modules', () => {
    const { element } = render({ modules: [] });
    expect(element.textContent).toContain('Aucun module compilé');
    expect(element.querySelector('table')).toBeNull();
  });
});
