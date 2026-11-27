import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { scopeGuard } from './core/scope/scope.guard';

export const routes: Routes = [
  {
    path: 'login',
    title: 'Connexion — AUTHORITY',
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
        title: 'Portée — AUTHORITY',
        loadComponent: () => import('./features/tenants/tenants').then((module) => module.TenantsPage),
      },
      {
        path: 'overview',
        canActivate: [scopeGuard],
        title: 'Vue d’ensemble — AUTHORITY',
        loadComponent: () => import('./features/overview/overview').then((module) => module.OverviewPage),
      },
      {
        path: 'inventory',
        canActivate: [scopeGuard],
        title: 'Inventaire — AUTHORITY',
        loadComponent: () => import('./features/inventory/inventory').then((module) => module.InventoryPage),
      },
    ],
  },
  { path: '', pathMatch: 'full', redirectTo: 'app' },
  {
    path: '**',
    title: 'Page introuvable — AUTHORITY',
    loadComponent: () => import('./features/not-found/not-found').then((module) => module.NotFoundPage),
  },
];
