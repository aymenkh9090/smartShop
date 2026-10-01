import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import { applyServerErrors, fieldErrorMessage } from './form-errors';

describe('form errors', () => {
  it('maps validator errors to French messages', () => {
    expect(fieldErrorMessage(new FormControl('', Validators.required))).toBe('Champ obligatoire');
    expect(fieldErrorMessage(new FormControl('abcdef', Validators.maxLength(3)))).toBe('3 caracteres maximum');
    expect(fieldErrorMessage(new FormControl(-1, Validators.min(0)))).toBe('Valeur minimale : 0');
    expect(fieldErrorMessage(new FormControl('ok'))).toBe('');
  });

  it('applies backend field errors to matching controls only', () => {
    const form = new FormGroup({ sku: new FormControl('X'), name: new FormControl('Y') });
    const error = new HttpErrorResponse({
      status: 400,
      error: { errors: [{ field: 'sku', message: 'deja utilise' }, { field: 'unknown', message: 'ignore' }] },
    });

    applyServerErrors(form, error);

    expect(fieldErrorMessage(form.controls.sku)).toBe('deja utilise');
    expect(form.controls.sku.touched).toBeTrue();
    expect(form.controls.name.valid).toBeTrue();
  });
});
