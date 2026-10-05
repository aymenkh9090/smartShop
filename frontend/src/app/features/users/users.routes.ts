import { Routes } from '@angular/router';

/** Toute la feature est reservee aux ADMIN (guard pose sur la route parente dans app.routes.ts). */
export const USER_ROUTES: Routes = [
  {
    path: '',
    title: 'Utilisateurs - SmartShop',
    loadComponent: () => import('./user-list/user-list').then((m) => m.UserList),
  },
  {
    path: 'new',
    title: 'Nouvel utilisateur - SmartShop',
    loadComponent: () => import('./user-form/user-form').then((m) => m.UserForm),
  },
  {
    path: ':id/edit',
    title: "Modifier l'utilisateur - SmartShop",
    loadComponent: () => import('./user-form/user-form').then((m) => m.UserForm),
  },
];
