import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { catchError, map, Observable, of, Subject, switchMap } from 'rxjs';
import { ApiFailure, toFailure } from '../api/problem';
import { AuditPage, AuditRecord } from '../models';
import { ScopeService } from '../scope/scope.service';
import { scopeHeaders } from '../scope/scope.headers';

export const AUDIT_PERMISSION = 'audit.read.tenant';
export const AUDIT_PAGE_SIZE = 25;

export type AuditStatus =
  | 'idle'
  | 'loading'
  | 'ready'
  | 'authentication'
  | 'denied'
  | 'invalid'
  | 'unavailable'
  | 'error';

export interface AuditCursor {
  occurredAt: string;
  recordId: string;
}

type Request = { kind: 'first' } | { kind: 'more' };

type Result =
  | { kind: 'cleared' }
  | { kind: 'denied' }
  | { kind: 'page'; page: AuditPage; append: boolean }
  | { kind: 'failure'; failure: ApiFailure; append: boolean };

@Injectable({ providedIn: 'root' })
export class AuditService {
  private readonly recordsSignal = signal<AuditRecord[]>([]);
  private readonly nextSignal = signal<AuditCursor | null>(null);
  private readonly statusSignal = signal<AuditStatus>('idle');
  private readonly failureSignal = signal<ApiFailure | null>(null);
  private readonly loadingMoreSignal = signal(false);
  private readonly moreFailureSignal = signal<ApiFailure | null>(null);
  private readonly requests = new Subject<Request>();

  readonly records = this.recordsSignal.asReadonly();
  readonly next = this.nextSignal.asReadonly();
  readonly status = this.statusSignal.asReadonly();
  readonly failure = this.failureSignal.asReadonly();
  readonly loadingMore = this.loadingMoreSignal.asReadonly();
  readonly moreFailure = this.moreFailureSignal.asReadonly();

  constructor(
    private readonly http: HttpClient,
    private readonly scope: ScopeService,
  ) {
    this.requests.pipe(switchMap((request) => this.run(request))).subscribe((result) => this.apply(result));
  }

  load(): void {
    const active = this.scope.scope();
    this.recordsSignal.set([]);
    this.nextSignal.set(null);
    this.failureSignal.set(null);
    this.moreFailureSignal.set(null);
    this.loadingMoreSignal.set(false);
    this.statusSignal.set(
      !active
        ? 'idle'
        : active.permissions.includes(AUDIT_PERMISSION)
          ? 'loading'
          : 'denied',
    );
    this.requests.next({ kind: 'first' });
  }

  loadMore(): void {
    if (this.statusSignal() !== 'ready' || !this.nextSignal() || this.loadingMoreSignal()) return;
    this.moreFailureSignal.set(null);
    this.loadingMoreSignal.set(true);
    this.requests.next({ kind: 'more' });
  }

  private run(request: Request): Observable<Result> {
    const active = this.scope.scope();
    if (!active) return of<Result>({ kind: 'cleared' });
    if (!active.permissions.includes(AUDIT_PERMISSION)) return of<Result>({ kind: 'denied' });

    const append = request.kind === 'more';
    let params = new HttpParams().set('limit', String(AUDIT_PAGE_SIZE));
    if (append) {
      const cursor = this.nextSignal();
      if (!cursor) return of<Result>({ kind: 'cleared' });
      params = params
        .set('beforeOccurredAt', cursor.occurredAt)
        .set('beforeRecordId', cursor.recordId);
    }
    return this.http
      .get<AuditPage>('/api/v1/audit/tenant-records', { headers: scopeHeaders(active), params })
      .pipe(
        map((page): Result => ({ kind: 'page', page, append })),
        catchError((error) => of<Result>({ kind: 'failure', failure: toFailure(error), append })),
      );
  }

  private apply(result: Result): void {
    if (result.kind === 'cleared') {
      this.recordsSignal.set([]);
      this.nextSignal.set(null);
      this.failureSignal.set(null);
      this.moreFailureSignal.set(null);
      this.loadingMoreSignal.set(false);
      this.statusSignal.set('idle');
      return;
    }
    if (result.kind === 'denied') {
      this.recordsSignal.set([]);
      this.nextSignal.set(null);
      this.failureSignal.set(null);
      this.moreFailureSignal.set(null);
      this.loadingMoreSignal.set(false);
      this.statusSignal.set('denied');
      return;
    }
    if (result.kind === 'page') {
      const records = result.append
        ? mergeUnique(this.recordsSignal(), result.page.items)
        : result.page.items;
      this.recordsSignal.set(records);
      this.nextSignal.set(
        result.page.nextOccurredAt && result.page.nextRecordId
          ? { occurredAt: result.page.nextOccurredAt, recordId: result.page.nextRecordId }
          : null,
      );
      this.failureSignal.set(null);
      this.moreFailureSignal.set(null);
      this.loadingMoreSignal.set(false);
      this.statusSignal.set('ready');
      return;
    }
    this.loadingMoreSignal.set(false);
    if (result.append) {
      this.moreFailureSignal.set(result.failure);
      return;
    }
    this.recordsSignal.set([]);
    this.nextSignal.set(null);
    this.failureSignal.set(result.failure);
    this.statusSignal.set(statusFor(result.failure));
  }
}

function mergeUnique(current: AuditRecord[], incoming: AuditRecord[]): AuditRecord[] {
  const seen = new Set(current.map((record) => record.recordId));
  const merged = [...current];
  for (const record of incoming) {
    if (!seen.has(record.recordId)) {
      seen.add(record.recordId);
      merged.push(record);
    }
  }
  return merged;
}

function statusFor(failure: ApiFailure): AuditStatus {
  switch (failure.kind) {
    case 'authentication':
      return 'authentication';
    case 'denied':
      return 'denied';
    case 'invalid':
      return 'invalid';
    case 'unavailable':
    case 'network':
      return 'unavailable';
    default:
      return 'error';
  }
}
