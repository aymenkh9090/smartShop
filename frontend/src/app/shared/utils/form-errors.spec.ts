import { HttpErrorResponse } from '@angular/common/http';
import { FormArray, FormControl, FormGroup, Validators } from '@angular/forms';

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

  it('maps indexed backend fields (units[1].imei) to form array paths', () => {
    const form = new FormGroup({
      units: new FormArray([
        new FormGroup({ imei: new FormControl('') }),
        new FormGroup({ imei: new FormControl('') }),
      ]),
    });
    const error = new HttpErrorResponse({
      status: 400,
      error: { errors: [{ field: 'units[1].imei', message: 'IMEI invalide' }] },
    });

    applyServerErrors(form, error);

    expect(form.controls.units.at(1).controls.imei.errors?.['server']).toBe('IMEI invalide');
    expect(form.controls.units.at(0).valid).toBeTrue();
  });
});
