import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { ScopeService } from './scope.service';

export const scopeGuard: CanActivateFn = () => {
  const scope = inject(ScopeService);
  const router = inject(Router);
  return scope.scope() ? true : router.parseUrl('/app/tenants');
};
