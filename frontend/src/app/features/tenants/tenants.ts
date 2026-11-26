import { Component, computed, effect, inject } from '@angular/core';
import { Router } from '@angular/router';
import { SessionService } from '../../core/auth/session.service';
import { ScopeService } from '../../core/scope/scope.service';

@Component({
  selector: 'app-tenants-page',
  templateUrl: './tenants.html',
  styleUrl: './tenants.css',
})
export class TenantsPage {
  private readonly session = inject(SessionService);
  private readonly scope = inject(ScopeService);
  private readonly router = inject(Router);

  readonly profile = this.session.profile;
  readonly status = this.scope.status;
  readonly failure = this.scope.failure;
  readonly activeScope = this.scope.scope;
  readonly statusMessage = computed(() => {
    switch (this.scope.status()) {
      case 'loading':
        return 'Validation de la portée…';
      case 'denied':
        return 'Cette portée est refusée. La session reste active.';
      case 'invalid':
        return 'La portée demandée est invalide.';
      case 'unavailable':
        return 'Le service d’autorisation est indisponible.';
      case 'network':
        return 'Le service est injoignable.';
      default:
        return '';
    }
  });
  readonly busy = computed(() => this.scope.status() === 'loading');

  constructor() {
    effect(() => {
      if (this.scope.status() === 'ready' && this.scope.scope()) {
        void this.router.navigateByUrl('/app/overview');
      }
      if (this.scope.status() === 'authentication') {
        void this.router.navigateByUrl('/login');
      }
    });
  }

  choose(tenantId: string, companyId?: string): void {
    this.scope.select({ tenantId, companyId });
  }
}
