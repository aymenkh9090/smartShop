import { HttpErrorResponse } from '@angular/common/http';

import { ApiError, ApiFieldError } from '../models/api-error';

/** Message lisible pour l'utilisateur a partir d'une erreur HTTP. */
export function apiErrorMessage(error: unknown, fallback = 'Une erreur est survenue.'): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'Serveur injoignable. Verifiez votre connexion.';
    }
    const message = (error.error as Partial<ApiError> | null)?.message;
    if (message) {
      return message;
    }
  }
  return fallback;
}

/** Erreurs de validation par champ renvoyees par le backend (400). */
export function apiFieldErrors(error: unknown): ApiFieldError[] {
  if (error instanceof HttpErrorResponse) {
    return (error.error as Partial<ApiError> | null)?.errors ?? [];
  }
  return [];
}
