import { Routes } from '@angular/router';

export const SUPPLIER_CREDIT_ROUTES: Routes = [
  {
    path: '',
    title: 'Credits fournisseurs - SmartShop',
    loadComponent: () => import('./credit-list/credit-list').then((m) => m.CreditList),
  },
  {
    path: 'balances',
    title: 'Soldes fournisseurs - SmartShop',
    loadComponent: () => import('./balance-list/balance-list').then((m) => m.BalanceList),
  },
  {
    path: ':id',
    title: 'Credit fournisseur - SmartShop',
    loadComponent: () => import('./credit-detail/credit-detail').then((m) => m.CreditDetail),
  },
];
