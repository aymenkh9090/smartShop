import { Routes } from '@angular/router';

import { authGuard, guestGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    title: 'Connexion - SmartShop',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login/login').then((m) => m.Login),
  },
  {
    path: '',
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        title: 'Tableau de bord - SmartShop',
        loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard),
      },
      {
        path: 'categories',
        loadChildren: () => import('./features/categories/categories.routes').then((m) => m.CATEGORY_ROUTES),
      },
      {
        path: 'products',
        loadChildren: () => import('./features/products/products.routes').then((m) => m.PRODUCT_ROUTES),
      },
      {
        path: 'stock',
        loadChildren: () => import('./features/stock/stock.routes').then((m) => m.STOCK_ROUTES),
      },
      {
        path: 'sales',
        canActivate: [authGuard],
        data: { roles: ['SELLER'] },
        loadChildren: () => import('./features/sales/sales.routes').then((m) => m.SALE_ROUTES),
      },
      {
        path: 'purchases',
        canActivate: [authGuard],
        data: { roles: ['MANAGER'] },
        loadChildren: () => import('./features/purchases/purchases.routes').then((m) => m.PURCHASE_ROUTES),
      },
      {
        path: 'customer-credits',
        canActivate: [authGuard],
        data: { roles: ['SELLER'] },
        loadChildren: () =>
          import('./features/customer-credits/customer-credits.routes').then((m) => m.CUSTOMER_CREDIT_ROUTES),
      },
      {
        path: 'supplier-credits',
        canActivate: [authGuard],
        data: { roles: ['MANAGER'] },
        loadChildren: () =>
          import('./features/supplier-credits/supplier-credits.routes').then((m) => m.SUPPLIER_CREDIT_ROUTES),
      },
      {
        path: 'after-sales',
        canActivate: [authGuard],
        data: { roles: ['SELLER', 'TECHNICIAN'] },
        loadChildren: () => import('./features/after-sales/after-sales.routes').then((m) => m.AFTER_SALES_ROUTES),
      },
      {
        path: 'customers',
        loadChildren: () => import('./features/customers/customers.routes').then((m) => m.CUSTOMER_ROUTES),
      },
      {
        path: 'suppliers',
        canActivate: [authGuard],
        data: { roles: ['MANAGER'] },
        loadChildren: () => import('./features/suppliers/suppliers.routes').then((m) => m.SUPPLIER_ROUTES),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
