import { Routes } from '@angular/router';

export const CUSTOMER_CREDIT_ROUTES: Routes = [
  {
    path: '',
    title: 'Crédits clients - SmartShop',
    loadComponent: () => import('./credit-list/credit-list').then((m) => m.CreditList),
  },
  {
    path: 'new',
    title: 'Nouveau crédit client - SmartShop',
    loadComponent: () => import('./credit-form/credit-form').then((m) => m.CreditForm),
  },
  {
    path: 'balances',
    title: 'Soldes clients - SmartShop',
    loadComponent: () => import('./balance-list/balance-list').then((m) => m.BalanceList),
  },
  {
    path: 'manual/:id',
    title: 'Crédit client - SmartShop',
    data: { source: 'MANUAL' },
    loadComponent: () => import('./credit-detail/credit-detail').then((m) => m.CreditDetail),
  },
  {
    path: ':id',
    title: 'Crédit client - SmartShop',
    data: { source: 'SALE' },
    loadComponent: () => import('./credit-detail/credit-detail').then((m) => m.CreditDetail),
  },
];
