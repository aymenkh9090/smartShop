import { Routes } from '@angular/router';

import { authGuard } from '../../core/guards/auth.guard';

export const CUSTOMER_ROUTES: Routes = [
  {
    path: '',
    title: 'Clients - SmartShop',
    loadComponent: () => import('./customer-list/customer-list').then((m) => m.CustomerList),
  },
  {
    path: 'new',
    title: 'Nouveau client - SmartShop',
    canActivate: [authGuard],
    data: { roles: ['SELLER'] },
    loadComponent: () => import('./customer-form/customer-form').then((m) => m.CustomerForm),
  },
  {
    path: ':id/edit',
    title: 'Modifier le client - SmartShop',
    canActivate: [authGuard],
    data: { roles: ['SELLER'] },
    loadComponent: () => import('./customer-form/customer-form').then((m) => m.CustomerForm),
  },
];
