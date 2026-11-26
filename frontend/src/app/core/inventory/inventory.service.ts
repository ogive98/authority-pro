import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { catchError, map, of, Subject, switchMap } from 'rxjs';
import { ApiFailure, toFailure } from '../api/problem';
import { InventoryView } from '../models';
import { ScopeService } from '../scope/scope.service';
import { scopeHeaders } from '../scope/scope.headers';

export const INVENTORY_PERMISSION = 'platform.inventory.read';

export type InventoryStatus =
  | 'idle'
  | 'loading'
  | 'ready'
  | 'authentication'
  | 'denied'
  | 'unavailable'
  | 'error';

type Result =
  | { kind: 'cleared' }
  | { kind: 'denied' }
  | { kind: 'ok'; inventory: InventoryView }
  | { kind: 'failure'; failure: ApiFailure };

@Injectable({ providedIn: 'root' })
export class InventoryService {
  private readonly inventorySignal = signal<InventoryView | null>(null);
  private readonly statusSignal = signal<InventoryStatus>('idle');
  private readonly failureSignal = signal<ApiFailure | null>(null);
  private readonly loads = new Subject<void>();

  readonly inventory = this.inventorySignal.asReadonly();
  readonly status = this.statusSignal.asReadonly();
  readonly failure = this.failureSignal.asReadonly();

  constructor(
    private readonly http: HttpClient,
    private readonly scope: ScopeService,
  ) {
    this.loads
      .pipe(
        switchMap(() => {
          const active = this.scope.scope();
          if (!active) return of<Result>({ kind: 'cleared' });
          if (!active.permissions.includes(INVENTORY_PERMISSION)) return of<Result>({ kind: 'denied' });
          return this.http
            .get<InventoryView>('/api/v1/platform/inventory', { headers: scopeHeaders(active) })
            .pipe(
              map((inventory): Result => ({ kind: 'ok', inventory })),
              catchError((error) => of<Result>({ kind: 'failure', failure: toFailure(error) })),
            );
        }),
      )
      .subscribe((result) => this.apply(result));
  }

  load(): void {
    const active = this.scope.scope();
    this.inventorySignal.set(null);
    this.failureSignal.set(null);
    this.statusSignal.set(
      !active
        ? 'idle'
        : active.permissions.includes(INVENTORY_PERMISSION)
          ? 'loading'
          : 'denied',
    );
    this.loads.next();
  }

  private apply(result: Result): void {
    if (result.kind === 'cleared') {
      this.inventorySignal.set(null);
      this.failureSignal.set(null);
      this.statusSignal.set('idle');
      return;
    }
    if (result.kind === 'denied') {
      this.inventorySignal.set(null);
      this.failureSignal.set(null);
      this.statusSignal.set('denied');
      return;
    }
    if (result.kind === 'ok') {
      this.inventorySignal.set(result.inventory);
      this.failureSignal.set(null);
      this.statusSignal.set('ready');
      return;
    }
    this.inventorySignal.set(null);
    this.failureSignal.set(result.failure);
    this.statusSignal.set(statusFor(result.failure));
  }
}

function statusFor(failure: ApiFailure): InventoryStatus {
  switch (failure.kind) {
    case 'authentication':
      return 'authentication';
    case 'denied':
      return 'denied';
    case 'unavailable':
    case 'network':
      return 'unavailable';
    default:
      return 'error';
  }
}
