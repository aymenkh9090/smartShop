import { Routes } from '@angular/router';

import { authGuard } from '../../core/guards/auth.guard';

export const SUPPLIER_ROUTES: Routes = [
  {
    path: '',
    title: 'Fournisseurs - SmartShop',
    loadComponent: () => import('./supplier-list/supplier-list').then((m) => m.SupplierList),
  },
  {
    path: 'new',
    title: 'Nouveau fournisseur - SmartShop',
    canActivate: [authGuard],
    data: { roles: ['MANAGER'] },
    loadComponent: () => import('./supplier-form/supplier-form').then((m) => m.SupplierForm),
  },
  {
    path: ':id/edit',
    title: 'Modifier le fournisseur - SmartShop',
    canActivate: [authGuard],
    data: { roles: ['MANAGER'] },
    loadComponent: () => import('./supplier-form/supplier-form').then((m) => m.SupplierForm),
  },
];
