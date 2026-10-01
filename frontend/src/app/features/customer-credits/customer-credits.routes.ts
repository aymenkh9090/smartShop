import { Routes } from '@angular/router';

export const CUSTOMER_CREDIT_ROUTES: Routes = [
  {
    path: '',
    title: 'Credits clients - SmartShop',
    loadComponent: () => import('./credit-list/credit-list').then((m) => m.CreditList),
  },
  {
    path: 'balances',
    title: 'Soldes clients - SmartShop',
    loadComponent: () => import('./balance-list/balance-list').then((m) => m.BalanceList),
  },
  {
    path: ':id',
    title: 'Credit client - SmartShop',
    loadComponent: () => import('./credit-detail/credit-detail').then((m) => m.CreditDetail),
  },
];
