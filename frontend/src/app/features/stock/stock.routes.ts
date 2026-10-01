import { Routes } from '@angular/router';

import { authGuard } from '../../core/guards/auth.guard';

export const STOCK_ROUTES: Routes = [
  {
    path: '',
    title: 'Stock - SmartShop',
    loadComponent: () => import('./movement-list/movement-list').then((m) => m.MovementList),
  },
  {
    path: 'new',
    title: 'Nouveau mouvement - SmartShop',
    canActivate: [authGuard],
    data: { roles: ['MANAGER'] },
    loadComponent: () => import('./movement-form/movement-form').then((m) => m.MovementForm),
  },
  {
    path: 'inventory',
    title: 'Inventaire - SmartShop',
    canActivate: [authGuard],
    data: { roles: ['MANAGER'] },
    loadComponent: () => import('./inventory/inventory').then((m) => m.Inventory),
  },
  {
    path: 'units',
    title: 'Recherche IMEI - SmartShop',
    loadComponent: () => import('./unit-list/unit-list').then((m) => m.UnitList),
  },
];
