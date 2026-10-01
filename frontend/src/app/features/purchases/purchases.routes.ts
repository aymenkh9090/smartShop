import { Routes } from '@angular/router';

export const PURCHASE_ROUTES: Routes = [
  {
    path: '',
    title: 'Achats - SmartShop',
    loadComponent: () => import('./purchase-list/purchase-list').then((m) => m.PurchaseList),
  },
  {
    path: 'new',
    title: 'Nouvel achat - SmartShop',
    loadComponent: () => import('./purchase-form/purchase-form').then((m) => m.PurchaseForm),
  },
  {
    path: ':id',
    title: 'Achat - SmartShop',
    loadComponent: () => import('./purchase-detail/purchase-detail').then((m) => m.PurchaseDetail),
  },
];
