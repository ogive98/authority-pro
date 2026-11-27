import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { INVENTORY_PERMISSION, InventoryService } from '../../core/inventory/inventory.service';
import { ModuleView } from '../../core/models';
import { ScopeService } from '../../core/scope/scope.service';

type SortKey = 'id' | 'version' | 'lifecycle';
type CopyState = 'idle' | 'copied' | 'failed';

const PAGE_SIZE = 10;

@Component({
  selector: 'app-inventory-page',
  imports: [RouterLink],
  templateUrl: './inventory.html',
  styleUrl: './inventory.css',
})
export class InventoryPage {
  private readonly scope = inject(ScopeService);
  private readonly inventoryStore = inject(InventoryService);

  readonly access = this.scope.scope;
  readonly inventory = this.inventoryStore.inventory;
  readonly status = this.inventoryStore.status;
  readonly failure = this.inventoryStore.failure;
  readonly permission = INVENTORY_PERMISSION;
  readonly pageSize = PAGE_SIZE;

  readonly filter = signal('');
  readonly lifecycle = signal('all');
  readonly sortKey = signal<SortKey>('id');
  readonly ascending = signal(true);
  readonly page = signal(0);
  readonly expandedId = signal<string | null>(null);
  readonly copyState = signal<CopyState>('idle');

  readonly canRead = computed(
    () => this.access()?.permissions.includes(INVENTORY_PERMISSION) ?? false,
  );
  readonly modules = computed<ModuleView[]>(() => this.inventory()?.modules ?? []);
  readonly lifecycles = computed(() =>
    [...new Set(this.modules().map((module) => module.lifecycle))].sort((left, right) =>
      left.localeCompare(right, 'fr', { numeric: true, sensitivity: 'base' }),
    ),
  );
  readonly filtered = computed<ModuleView[]>(() => {
    const query = this.filter().trim().toLocaleLowerCase('fr');
    const lifecycle = this.lifecycle();
    const key = this.sortKey();
    const direction = this.ascending() ? 1 : -1;
    return this.modules()
      .map((module, index) => ({ module, index }))
      .filter(({ module }) => {
        if (lifecycle !== 'all' && module.lifecycle !== lifecycle) return false;
        if (!query) return true;
        return [module.id, module.version, module.lifecycle].some((value) =>
          value.toLocaleLowerCase('fr').includes(query),
        );
      })
      .sort((left, right) => {
        const compared = left.module[key].localeCompare(right.module[key], 'fr', {
          numeric: true,
          sensitivity: 'base',
        });
        return compared !== 0 ? compared * direction : left.index - right.index;
      })
      .map(({ module }) => module);
  });
  readonly pageCount = computed(() => Math.max(1, Math.ceil(this.filtered().length / PAGE_SIZE)));
  readonly currentPage = computed(() => Math.min(this.page(), this.pageCount() - 1));
  readonly visible = computed<ModuleView[]>(() => {
    const start = this.currentPage() * PAGE_SIZE;
    return this.filtered().slice(start, start + PAGE_SIZE);
  });
  readonly summary = computed(() => {
    const total = this.filtered().length;
    const label = total === 1 ? '1 module' : `${total} modules`;
    return `${label} — page ${this.currentPage() + 1} sur ${this.pageCount()}`;
  });

  constructor() {
    this.inventoryStore.load();
  }

  setFilter(event: Event): void {
    this.filter.set((event.target as HTMLInputElement).value);
    this.restart();
  }

  setLifecycle(event: Event): void {
    this.lifecycle.set((event.target as HTMLSelectElement).value);
    this.restart();
  }

  sortBy(key: SortKey): void {
    if (this.sortKey() === key) {
      this.ascending.update((ascending) => !ascending);
    } else {
      this.sortKey.set(key);
      this.ascending.set(true);
    }
    this.restart();
  }

  ariaSort(key: SortKey): 'ascending' | 'descending' | null {
    if (this.sortKey() !== key) return null;
    return this.ascending() ? 'ascending' : 'descending';
  }

  toggle(module: ModuleView): void {
    this.copyState.set('idle');
    this.expandedId.update((current) => (current === module.id ? null : module.id));
  }

  closeDetail(): void {
    this.expandedId.set(null);
  }

  async copyDigest(module: ModuleView): Promise<void> {
    try {
      await navigator.clipboard.writeText(module.digest);
      this.copyState.set('copied');
    } catch {
      this.copyState.set('failed');
    }
  }

  resetFilters(): void {
    this.filter.set('');
    this.lifecycle.set('all');
    this.restart();
  }

  previousPage(): void {
    if (this.currentPage() === 0) return;
    this.page.set(this.currentPage() - 1);
    this.expandedId.set(null);
  }

  nextPage(): void {
    if (this.currentPage() >= this.pageCount() - 1) return;
    this.page.set(this.currentPage() + 1);
    this.expandedId.set(null);
  }

  refresh(): void {
    this.expandedId.set(null);
    this.copyState.set('idle');
    this.inventoryStore.load();
  }

  private restart(): void {
    this.page.set(0);
    this.expandedId.set(null);
    this.copyState.set('idle');
  }
}
