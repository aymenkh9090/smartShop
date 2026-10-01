import { Routes } from '@angular/router';

import { authGuard } from '../../core/guards/auth.guard';

export const PRODUCT_ROUTES: Routes = [
  {
    path: '',
    title: 'Produits - SmartShop',
    loadComponent: () => import('./product-list/product-list').then((m) => m.ProductList),
  },
  {
    path: 'new',
    title: 'Nouveau produit - SmartShop',
    canActivate: [authGuard],
    data: { roles: ['MANAGER'] },
    loadComponent: () => import('./product-form/product-form').then((m) => m.ProductForm),
  },
  {
    path: ':id/edit',
    title: 'Modifier le produit - SmartShop',
    canActivate: [authGuard],
    data: { roles: ['MANAGER'] },
    loadComponent: () => import('./product-form/product-form').then((m) => m.ProductForm),
  },
];
