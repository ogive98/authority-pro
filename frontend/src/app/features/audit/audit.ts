import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { AUDIT_PERMISSION, AuditService } from '../../core/audit/audit.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { AuditRecord } from '../../core/models';
import { ScopeService } from '../../core/scope/scope.service';

@Component({
  selector: 'app-audit-page',
  imports: [RouterLink, TranslatePipe],
  templateUrl: './audit.html',
  styleUrl: './audit.css',
})
export class AuditPage {
  private readonly scope = inject(ScopeService);
  private readonly auditStore = inject(AuditService);
  private readonly i18n = inject(I18nService);

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
  readonly families = computed(() => {
    const locale = this.i18n.locale();
    return [...new Set(this.records().map((record) => record.actionCode.split('.')[0]))].sort(
      (left, right) => left.localeCompare(right, locale, { sensitivity: 'base' }),
    );
  });
  readonly filtered = computed<AuditRecord[]>(() => {
    const family = this.family();
    if (family === 'all') return this.records();
    return this.records().filter((record) => record.actionCode.startsWith(`${family}.`));
  });
  readonly summary = computed(() => {
    this.i18n.language();
    return this.i18n.instant('audit.summary', {
      loaded: this.records().length,
      visible: this.filtered().length,
    });
  });

  private readonly dateFormats = new Map<string, Intl.DateTimeFormat>();

  constructor() {
    this.auditStore.load();
  }

  setFamily(event: Event): void {
    this.family.set((event.target as HTMLSelectElement).value);
  }

  formatDate(value: string): string {
    const locale = this.i18n.locale();
    let format = this.dateFormats.get(locale);
    if (!format) {
      format = new Intl.DateTimeFormat(locale, { dateStyle: 'short', timeStyle: 'medium' });
      this.dateFormats.set(locale, format);
    }
    return format.format(new Date(value));
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
