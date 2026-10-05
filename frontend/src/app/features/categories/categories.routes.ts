import { Routes } from '@angular/router';

import { authGuard } from '../../core/guards/auth.guard';

export const CATEGORY_ROUTES: Routes = [
  {
    path: '',
    title: 'Catégories - SmartShop',
    loadComponent: () => import('./category-list/category-list').then((m) => m.CategoryList),
  },
  {
    path: 'new',
    title: 'Nouvelle catégorie - SmartShop',
    canActivate: [authGuard],
    data: { roles: ['MANAGER'] },
    loadComponent: () => import('./category-form/category-form').then((m) => m.CategoryForm),
  },
  {
    path: ':id/edit',
    title: 'Modifier la catégorie - SmartShop',
    canActivate: [authGuard],
    data: { roles: ['MANAGER'] },
    loadComponent: () => import('./category-form/category-form').then((m) => m.CategoryForm),
  },
];
