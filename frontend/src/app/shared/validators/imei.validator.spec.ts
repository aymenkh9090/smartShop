import { FormControl } from '@angular/forms';

import { imeiValidator, isValidImei } from './imei.validator';

describe('imeiValidator', () => {
  it('checks the Luhn key like the backend', () => {
    expect(isValidImei('490154203237518')).toBeTrue();
    expect(isValidImei('490154203237519')).toBeFalse();
    expect(isValidImei('49015420323751')).toBeFalse();
  });

  it('accepts empty values and trims input', () => {
    expect(imeiValidator(new FormControl(''))).toBeNull();
    expect(imeiValidator(new FormControl(' 490154203237518 '))).toBeNull();
    expect(imeiValidator(new FormControl('123'))).toEqual({ imei: true });
  });
});
