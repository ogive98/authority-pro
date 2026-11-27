import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal, WritableSignal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { AuditPage, AuditRecord, ScopedAccess } from '../models';
import { ScopeService } from '../scope/scope.service';
import { AUDIT_PAGE_SIZE, AUDIT_PERMISSION, AuditService } from './audit.service';

const scope: ScopedAccess = {
  tenantId: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
  tenantName: 'Tenant A',
  companyId: null,
  companyName: null,
  authorizationRevision: 4,
  roles: ['ROLE_OWNER'],
  permissions: [AUDIT_PERMISSION],
};

const actor = 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb';

function record(id: string, occurredAt = '2026-11-27T10:00:00Z'): AuditRecord {
  return {
    recordId: id,
    occurredAt,
    actionCode: 'access.membership.created',
    actionVersion: 1,
    actorId: actor,
    actorKind: 'USER',
    targetType: 'membership',
    targetId: 'membership-ref',
    operationId: 'cccccccc-cccc-4ccc-8ccc-cccccccccccc',
    correlationId: 'dddddddd-dddd-4ddd-8ddd-dddddddddddd',
    schemaVersion: 1,
    metadata: { roleCode: 'ROLE_OWNER' },
    outcome: 'SUCCESS',
    tenantId: scope.tenantId,
    companyId: null,
    siteId: null,
  };
}

function page(items: AuditRecord[], next = false): AuditPage {
  if (!next || items.length === 0) return { items, nextOccurredAt: null, nextRecordId: null };
  const last = items[items.length - 1];
  return {
    items,
    nextOccurredAt: last.occurredAt,
    nextRecordId: last.recordId,
  };
}

const tick = () => new Promise((resolve) => setTimeout(resolve, 0));

describe('AuditService', () => {
  let service: AuditService;
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
    service = TestBed.inject(AuditService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('never calls the API without the audit permission', async () => {
    active.set({ ...scope, permissions: [] });
    service.load();
    await tick();
    http.expectNone('/api/v1/audit/tenant-records');
    expect(service.status()).toBe('denied');
    expect(service.records()).toEqual([]);
  });

  it('reads the first page with scope headers and a bounded limit', async () => {
    service.load();
    const request = http.expectOne(
      (candidate) =>
        candidate.url === '/api/v1/audit/tenant-records' &&
        candidate.params.get('limit') === String(AUDIT_PAGE_SIZE) &&
        !candidate.params.has('beforeOccurredAt'),
    );
    expect(request.request.headers.get('X-Authority-Tenant')).toBe(scope.tenantId);
    request.flush(page([record('r1'), record('r2')], true));
    await tick();
    expect(service.status()).toBe('ready');
    expect(service.records().map((item) => item.recordId)).toEqual(['r1', 'r2']);
    expect(service.next()).toEqual({ occurredAt: '2026-11-27T10:00:00Z', recordId: 'r2' });
  });

  it('appends the next page with the cursor and never duplicates a record', async () => {
    service.load();
    http
      .expectOne((candidate) => candidate.url === '/api/v1/audit/tenant-records')
      .flush(page([record('r1'), record('r2')], true));
    await tick();

    service.loadMore();
    expect(service.loadingMore()).toBe(true);
    const request = http.expectOne(
      (candidate) =>
        candidate.url === '/api/v1/audit/tenant-records' &&
        candidate.params.get('beforeOccurredAt') === '2026-11-27T10:00:00Z' &&
        candidate.params.get('beforeRecordId') === 'r2',
    );
    request.flush(page([record('r2'), record('r3')]));
    await tick();
    expect(service.records().map((item) => item.recordId)).toEqual(['r1', 'r2', 'r3']);
    expect(service.next()).toBeNull();
    expect(service.loadingMore()).toBe(false);
  });

  it('keeps the loaded records when the next page fails', async () => {
    service.load();
    http
      .expectOne((candidate) => candidate.url === '/api/v1/audit/tenant-records')
      .flush(page([record('r1'), record('r2')], true));
    await tick();

    service.loadMore();
    http
      .expectOne((candidate) => candidate.url === '/api/v1/audit/tenant-records')
      .flush({ code: 'AUDIT_UNAVAILABLE' }, { status: 503, statusText: 'Service Unavailable' });
    await tick();
    expect(service.records().map((item) => item.recordId)).toEqual(['r1', 'r2']);
    expect(service.status()).toBe('ready');
    expect(service.moreFailure()?.kind).toBe('unavailable');
    expect(service.loadingMore()).toBe(false);
  });

  it('maps refusals, sessions, invalid queries and outages distinctly', async () => {
    service.load();
    http
      .expectOne((candidate) => candidate.url === '/api/v1/audit/tenant-records')
      .flush({ code: 'AUTHORIZATION_DENIED' }, { status: 403, statusText: 'Forbidden' });
    await tick();
    expect(service.status()).toBe('denied');

    service.load();
    http
      .expectOne((candidate) => candidate.url === '/api/v1/audit/tenant-records')
      .flush({ code: 'AUTHENTICATION_REQUIRED' }, { status: 401, statusText: 'Unauthorized' });
    await tick();
    expect(service.status()).toBe('authentication');

    service.load();
    http
      .expectOne((candidate) => candidate.url === '/api/v1/audit/tenant-records')
      .flush({ code: 'AUDIT_QUERY_INVALID' }, { status: 400, statusText: 'Bad Request' });
    await tick();
    expect(service.status()).toBe('invalid');

    service.load();
    http
      .expectOne((candidate) => candidate.url === '/api/v1/audit/tenant-records')
      .flush({ code: 'AUDIT_UNAVAILABLE' }, { status: 503, statusText: 'Service Unavailable' });
    await tick();
    expect(service.status()).toBe('unavailable');
  });

  it('stays idle without an active scope', async () => {
    active.set(null);
    service.load();
    await tick();
    http.expectNone('/api/v1/audit/tenant-records');
    expect(service.status()).toBe('idle');
  });
});
