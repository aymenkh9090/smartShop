import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { Role } from '../models/auth.models';
import { AuthService } from '../services/auth.service';

/**
 * Exige une session ; si la route declare `data: { roles: [...] }`, exige aussi l'un de ces roles.
 * Confort UX uniquement : le backend reste la reference pour les autorisations.
 */
export const authGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isAuthenticated()) {
    return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  }
  const roles = route.data['roles'] as Role[] | undefined;
  if (roles?.length && !auth.hasAnyRole(roles)) {
    return router.createUrlTree(['/dashboard']);
  }
  return true;
};

/** Empeche un utilisateur deja connecte d'afficher la page de connexion. */
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isAuthenticated() ? inject(Router).createUrlTree(['/dashboard']) : true;
};
