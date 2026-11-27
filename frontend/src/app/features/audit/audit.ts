import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AUDIT_PERMISSION, AuditService } from '../../core/audit/audit.service';
import { AuditRecord } from '../../core/models';
import { ScopeService } from '../../core/scope/scope.service';

@Component({
  selector: 'app-audit-page',
  imports: [RouterLink],
  templateUrl: './audit.html',
  styleUrl: './audit.css',
})
export class AuditPage {
  private readonly scope = inject(ScopeService);
  private readonly auditStore = inject(AuditService);

  readonly access = this.scope.scope;
  readonly records = this.auditStore.records;
  readonly status = this.auditStore.status;
  readonly failure = this.auditStore.failure;
  readonly next = this.auditStore.next;
  readonly loadingMore = this.auditStore.loadingMore;
  readonly moreFailure = this.auditStore.moreFailure;
  readonly permission = AUDIT_PERMISSION;

  readonly family = signal('all');
  readonly canRead = computed(
    () => this.access()?.permissions.includes(AUDIT_PERMISSION) ?? false,
  );
  readonly families = computed(() =>
    [...new Set(this.records().map((record) => record.actionCode.split('.')[0]))].sort((left, right) =>
      left.localeCompare(right, 'fr', { sensitivity: 'base' }),
    ),
  );
  readonly filtered = computed<AuditRecord[]>(() => {
    const family = this.family();
    if (family === 'all') return this.records();
    return this.records().filter((record) => record.actionCode.startsWith(`${family}.`));
  });
  readonly summary = computed(() => {
    const loaded = this.records().length;
    const visible = this.filtered().length;
    const label = loaded === 1 ? '1 enregistrement chargé' : `${loaded} enregistrements chargés`;
    return visible === loaded ? label : `${visible} sur ${label}`;
  });

  private readonly dateFormat = new Intl.DateTimeFormat('fr-FR', {
    dateStyle: 'short',
    timeStyle: 'medium',
  });

  constructor() {
    this.auditStore.load();
  }

  setFamily(event: Event): void {
    this.family.set((event.target as HTMLSelectElement).value);
  }

  formatDate(value: string): string {
    return this.dateFormat.format(new Date(value));
  }

  shortId(value: string | null): string {
    if (!value) return '—';
    return value.length > 12 ? `${value.slice(0, 8)}…` : value;
  }

  refresh(): void {
    this.family.set('all');
    this.auditStore.load();
  }

  loadMore(): void {
    this.auditStore.loadMore();
  }
}
