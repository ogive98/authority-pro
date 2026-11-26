import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { INVENTORY_PERMISSION, InventoryService } from '../../core/inventory/inventory.service';
import { ScopeService } from '../../core/scope/scope.service';

@Component({
  selector: 'app-overview-page',
  imports: [RouterLink],
  templateUrl: './overview.html',
  styleUrl: './overview.css',
})
export class OverviewPage {
  private readonly scope = inject(ScopeService);
  private readonly inventoryStore = inject(InventoryService);

  readonly access = this.scope.scope;
  readonly inventory = this.inventoryStore.inventory;
  readonly inventoryStatus = this.inventoryStore.status;
  readonly inventoryFailure = this.inventoryStore.failure;
  readonly inventoryPermission = INVENTORY_PERMISSION;
  readonly canReadInventory = computed(
    () => this.access()?.permissions.includes(INVENTORY_PERMISSION) ?? false,
  );
  readonly totals = computed(() => {
    const inventory = this.inventory();
    if (!inventory) return null;
    const permissions = new Set<string>();
    let resources = 0;
    for (const module of inventory.modules) {
      for (const permission of module.permissions) permissions.add(permission.id);
      resources += module.dataOwnership.length;
    }
    return { modules: inventory.modules.length, permissions: permissions.size, resources };
  });

  constructor() {
    this.inventoryStore.load();
  }

  refreshInventory(): void {
    this.inventoryStore.load();
  }
}
