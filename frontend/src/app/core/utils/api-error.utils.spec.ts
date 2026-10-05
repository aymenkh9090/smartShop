import { HttpErrorResponse } from '@angular/common/http';

import { apiErrorMessage, apiFieldErrors } from './api-error.utils';

describe('api error utils', () => {
  it('uses the backend message when present', () => {
    const error = new HttpErrorResponse({ status: 409, error: { message: 'SKU déjà utilise', errors: [] } });
    expect(apiErrorMessage(error)).toBe('SKU déjà utilise');
  });

  it('reports an unreachable server', () => {
    expect(apiErrorMessage(new HttpErrorResponse({ status: 0 }))).toContain('Serveur injoignable');
  });

  it('falls back on a generic message', () => {
    expect(apiErrorMessage(new Error('boom'), 'Échec')).toBe('Échec');
  });

  it('extracts field errors', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: { message: 'Données invalides', errors: [{ field: 'sku', message: 'invalide' }] },
    });
    expect(apiFieldErrors(error)).toEqual([{ field: 'sku', message: 'invalide' }]);
    expect(apiFieldErrors(new Error('x'))).toEqual([]);
  });
});
