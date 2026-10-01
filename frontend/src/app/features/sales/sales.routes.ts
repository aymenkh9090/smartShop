import { Routes } from '@angular/router';

export const SALE_ROUTES: Routes = [
  {
    path: '',
    title: 'Ventes - SmartShop',
    loadComponent: () => import('./sale-list/sale-list').then((m) => m.SaleList),
  },
  {
    path: 'new',
    title: 'Nouvelle vente - SmartShop',
    loadComponent: () => import('./sale-form/sale-form').then((m) => m.SaleForm),
  },
  {
    path: ':id',
    title: 'Vente - SmartShop',
    loadComponent: () => import('./sale-detail/sale-detail').then((m) => m.SaleDetail),
  },
];
