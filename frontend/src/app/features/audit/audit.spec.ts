import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AUDIT_PERMISSION, AuditStatus } from '../../core/audit/audit.service';
import { ApiFailure } from '../../core/api/problem';
import { AuditRecord, ScopedAccess } from '../../core/models';
import { ScopeService } from '../../core/scope/scope.service';
import { AuditService } from '../../core/audit/audit.service';
import { AuditPage } from './audit';

const access: ScopedAccess = {
  tenantId: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
  tenantName: 'Tenant A',
  companyId: null,
  companyName: null,
  authorizationRevision: 4,
  roles: ['ROLE_OWNER'],
  permissions: [AUDIT_PERMISSION],
};

function record(id: string, actionCode: string, outcome = 'SUCCESS'): AuditRecord {
  return {
    recordId: id,
    occurredAt: '2026-11-27T10:00:00Z',
    actionCode,
    actionVersion: 1,
    actorId: 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb',
    actorKind: 'USER',
    targetType: 'membership',
    targetId: 'membership-ref',
    operationId: 'cccccccc-cccc-4ccc-8ccc-cccccccccccc',
    correlationId: 'dddddddd-dddd-4ddd-8ddd-dddddddddddd',
    schemaVersion: 1,
    metadata: {},
    outcome,
    tenantId: access.tenantId,
    companyId: null,
    siteId: null,
  };
}

interface State {
  status?: AuditStatus;
  records?: AuditRecord[];
  next?: { occurredAt: string; recordId: string } | null;
  loadingMore?: boolean;
  failure?: ApiFailure | null;
  moreFailure?: ApiFailure | null;
}

function render(state: State = {}, scope: ScopedAccess | null = access) {
  const load = vi.fn();
  const loadMore = vi.fn();
  TestBed.configureTestingModule({
    imports: [AuditPage],
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
        provide: AuditService,
        useValue: {
          records: signal(state.records ?? []).asReadonly(),
          status: signal(state.status ?? 'ready').asReadonly(),
          failure: signal(state.failure ?? null).asReadonly(),
          next: signal(state.next ?? null).asReadonly(),
          loadingMore: signal(state.loadingMore ?? false).asReadonly(),
          moreFailure: signal(state.moreFailure ?? null).asReadonly(),
          load,
          loadMore,
        },
      },
    ],
  });
  const fixture = TestBed.createComponent(AuditPage);
  fixture.detectChanges();
  return { fixture, load, loadMore, element: fixture.nativeElement as HTMLElement };
}

function buttonByText(element: HTMLElement, label: string): HTMLButtonElement {
  const button = Array.from(element.querySelectorAll('button')).find(
    (candidate) => (candidate as HTMLElement).textContent?.trim() === label,
  );
  if (!button) throw new Error(`Bouton introuvable : ${label}`);
  return button as HTMLButtonElement;
}

describe('AuditPage', () => {
  beforeEach(() => TestBed.resetTestingModule());

  it('names the missing permission and never renders the journal', () => {
    const { element, load } = render(
      { status: 'denied', records: [record('r1', 'access.membership.created')] },
      { ...access, permissions: [] },
    );
    expect(load).toHaveBeenCalledTimes(1);
    expect(element.textContent).toContain(AUDIT_PERMISSION);
    expect(element.textContent).toContain('non autorisé');
    expect(element.querySelector('table')).toBeNull();
  });

  it('renders the real records with a French timestamp and readable identifiers', () => {
    const { element } = render({
      records: [
        record('r1', 'access.membership.created'),
        record('r2', 'identity.session.revoked', 'FAILURE'),
      ],
      next: { occurredAt: '2026-11-27T10:00:00Z', recordId: 'r2' },
    });
    const rows = element.querySelectorAll('tbody tr');
    expect(rows).toHaveLength(2);
    const text = element.textContent as string;
    expect(text).toMatch(/\d{2}\/\d{2}\/2026/);
    expect(text).toContain('access.membership.created');
    expect(text).toContain('identity.session.revoked');
    expect(text).toContain('FAILURE');
    expect(text).toContain('bbbbbbbb…');
    expect(text).toContain('2 enregistrements chargés');
  });

  it('filters the loaded page by family', () => {
    const { fixture, element } = render({
      records: [
        record('r1', 'access.membership.created'),
        record('r2', 'identity.session.revoked'),
        record('r3', 'access.grant.created'),
      ],
    });
    const select = element.querySelector('#audit-family') as HTMLSelectElement;
    expect(select.textContent).toContain('access');
    expect(select.textContent).toContain('identity');
    select.value = 'identity';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(element.querySelectorAll('tbody tr')).toHaveLength(1);
    expect(element.textContent).toContain('1 sur 3 enregistrements chargés');
  });

  it('shows an honest empty state', () => {
    const { element } = render({ records: [] });
    expect(element.textContent).toContain('Aucun enregistrement d’audit');
    expect(element.querySelector('table')).toBeNull();
  });

  it('distinguishes refused and invalid reads from a lost session', () => {
    const denied = render({ status: 'denied' });
    expect(denied.element.textContent).toContain('Le service a refusé la lecture d’audit');
    TestBed.resetTestingModule();

    const invalid = render({ status: 'invalid' });
    expect(invalid.element.textContent).toContain('refusée comme invalide');
    TestBed.resetTestingModule();

    const expired = render({ status: 'authentication' });
    expect(expired.element.textContent).toContain('Session expirée');
  });

  it('keeps an unavailable journal honest and retries', () => {
    const { element, load } = render({ status: 'unavailable' });
    expect(element.textContent).toContain('Journal d’audit indisponible');
    buttonByText(element, 'Réessayer').click();
    expect(load).toHaveBeenCalledTimes(2);
  });

  it('loads the next page by cursor and reports the end of the journal', () => {
    const first = render({
      records: [record('r1', 'access.membership.created')],
      next: { occurredAt: '2026-11-27T10:00:00Z', recordId: 'r1' },
    });
    buttonByText(first.element, 'Charger plus').click();
    expect(first.loadMore).toHaveBeenCalledTimes(1);
    TestBed.resetTestingModule();

    const loading = render({
      records: [record('r1', 'access.membership.created')],
      next: { occurredAt: '2026-11-27T10:00:00Z', recordId: 'r1' },
      loadingMore: true,
    });
    expect(buttonByText(loading.element, 'Chargement…').disabled).toBe(true);
    TestBed.resetTestingModule();

    const end = render({ records: [record('r1', 'access.membership.created')] });
    expect(end.element.textContent).toContain('Fin des enregistrements disponibles.');
  });

  it('keeps the loaded records visible when the next page fails', () => {
    const { element } = render({
      records: [record('r1', 'access.membership.created')],
      next: { occurredAt: '2026-11-27T10:00:00Z', recordId: 'r1' },
      moreFailure: {
        kind: 'unavailable',
        status: 503,
        code: 'AUDIT_UNAVAILABLE',
        traceId: 'trace-audit-1',
        message: 'Le service est temporairement indisponible.',
      },
    });
    expect(element.querySelectorAll('tbody tr')).toHaveLength(1);
    expect(element.textContent).toContain('Le chargement suivant a échoué');
    expect(element.textContent).toContain('trace-audit-1');
  });
});
