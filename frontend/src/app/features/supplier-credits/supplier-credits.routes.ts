import { Routes } from '@angular/router';

export const SUPPLIER_CREDIT_ROUTES: Routes = [
  {
    path: '',
    title: 'Crédits fournisseurs - SmartShop',
    loadComponent: () => import('./credit-list/credit-list').then((m) => m.CreditList),
  },
  {
    path: 'new',
    title: 'Nouveau crédit fournisseur - SmartShop',
    loadComponent: () => import('./credit-form/credit-form').then((m) => m.CreditForm),
  },
  {
    path: 'balances',
    title: 'Soldes fournisseurs - SmartShop',
    loadComponent: () => import('./balance-list/balance-list').then((m) => m.BalanceList),
  },
  {
    path: 'manual/:id',
    title: 'Crédit fournisseur - SmartShop',
    data: { source: 'MANUAL' },
    loadComponent: () => import('./credit-detail/credit-detail').then((m) => m.CreditDetail),
  },
  {
    path: ':id',
    title: 'Crédit fournisseur - SmartShop',
    data: { source: 'PURCHASE' },
    loadComponent: () => import('./credit-detail/credit-detail').then((m) => m.CreditDetail),
  },
];
