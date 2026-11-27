import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { scopeGuard } from './core/scope/scope.guard';

export const routes: Routes = [
  {
    path: 'login',
    data: { titleKey: 'titles.login' },
    loadComponent: () => import('./features/login/login').then((module) => module.LoginPage),
  },
  {
    path: 'app',
    canActivate: [authGuard],
    loadComponent: () => import('./features/shell/shell').then((module) => module.Shell),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'tenants' },
      {
        path: 'tenants',
        data: { titleKey: 'titles.tenants' },
        loadComponent: () => import('./features/tenants/tenants').then((module) => module.TenantsPage),
      },
      {
        path: 'overview',
        canActivate: [scopeGuard],
        data: { titleKey: 'titles.overview' },
        loadComponent: () => import('./features/overview/overview').then((module) => module.OverviewPage),
      },
      {
        path: 'inventory',
        canActivate: [scopeGuard],
        data: { titleKey: 'titles.inventory' },
        loadComponent: () => import('./features/inventory/inventory').then((module) => module.InventoryPage),
      },
      {
        path: 'audit',
        canActivate: [scopeGuard],
        data: { titleKey: 'titles.audit' },
        loadComponent: () => import('./features/audit/audit').then((module) => module.AuditPage),
      },
      {
        path: 'preferences',
        data: { titleKey: 'titles.preferences' },
        loadComponent: () =>
          import('./features/preferences/preferences').then((module) => module.PreferencesPage),
      },
    ],
  },
  { path: '', pathMatch: 'full', redirectTo: 'app' },
  {
    path: '**',
    data: { titleKey: 'titles.notFound' },
    loadComponent: () => import('./features/not-found/not-found').then((module) => module.NotFoundPage),
  },
];
