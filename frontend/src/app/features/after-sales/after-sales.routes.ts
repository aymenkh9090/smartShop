import { Routes } from '@angular/router';

import { authGuard } from '../../core/guards/auth.guard';

export const AFTER_SALES_ROUTES: Routes = [
  {
    path: '',
    title: 'SAV - SmartShop',
    loadComponent: () => import('./ticket-list/ticket-list').then((m) => m.TicketList),
  },
  {
    path: 'new',
    title: 'Nouveau ticket SAV - SmartShop',
    canActivate: [authGuard],
    data: { roles: ['SELLER'] },
    loadComponent: () => import('./ticket-form/ticket-form').then((m) => m.TicketForm),
  },
  {
    path: ':id',
    title: 'Ticket SAV - SmartShop',
    loadComponent: () => import('./ticket-detail/ticket-detail').then((m) => m.TicketDetail),
  },
];
