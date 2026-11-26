import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { catchError, map, of, Subject, switchMap } from 'rxjs';
import { ApiFailure, toFailure } from '../api/problem';
import { ScopedAccess } from '../models';

export interface ScopeSelection {
  tenantId: string;
  companyId?: string;
}

export type ScopeStatus =
  | 'idle'
  | 'loading'
  | 'ready'
  | 'authentication'
  | 'denied'
  | 'invalid'
  | 'unavailable'
  | 'network';

type Result =
  | { kind: 'cleared' }
  | { kind: 'ok'; scope: ScopedAccess }
  | { kind: 'failure'; failure: ApiFailure };

@Injectable({ providedIn: 'root' })
export class ScopeService {
  private readonly scopeSignal = signal<ScopedAccess | null>(null);
  private readonly statusSignal = signal<ScopeStatus>('idle');
  private readonly failureSignal = signal<ApiFailure | null>(null);
  private readonly selections = new Subject<ScopeSelection | null>();

  readonly scope = this.scopeSignal.asReadonly();
  readonly status = this.statusSignal.asReadonly();
  readonly failure = this.failureSignal.asReadonly();

  constructor(private readonly http: HttpClient) {
    this.selections
      .pipe(
        switchMap((selection) => {
          if (!selection) return of<Result>({ kind: 'cleared' });
          const headers: Record<string, string> = { 'X-Authority-Tenant': selection.tenantId };
          if (selection.companyId) headers['X-Authority-Company'] = selection.companyId;
          return this.http
            .get<ScopedAccess>('/api/v1/access/bootstrap', { headers })
            .pipe(
              map((scope): Result => ({ kind: 'ok', scope })),
              catchError((error) => of<Result>({ kind: 'failure', failure: toFailure(error) })),
            );
        }),
      )
      .subscribe((result) => this.apply(result));
  }

  select(selection: ScopeSelection): void {
    this.scopeSignal.set(null);
    this.failureSignal.set(null);
    this.statusSignal.set('loading');
    this.selections.next(selection);
  }

  clear(): void {
    this.selections.next(null);
  }

  private apply(result: Result): void {
    if (result.kind === 'cleared') {
      this.scopeSignal.set(null);
      this.failureSignal.set(null);
      this.statusSignal.set('idle');
      return;
    }
    if (result.kind === 'ok') {
      this.scopeSignal.set(result.scope);
      this.failureSignal.set(null);
      this.statusSignal.set('ready');
      return;
    }
    this.scopeSignal.set(null);
    this.failureSignal.set(result.failure);
    this.statusSignal.set(statusFor(result.failure));
  }
}

function statusFor(failure: ApiFailure): ScopeStatus {
  switch (failure.kind) {
    case 'authentication':
      return 'authentication';
    case 'denied':
      return 'denied';
    case 'invalid':
      return 'invalid';
    case 'unavailable':
      return 'unavailable';
    case 'network':
      return 'network';
    default:
      return 'denied';
  }
}
