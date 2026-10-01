import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Meme regle que le backend : 15 chiffres dont une cle de Luhn. Une valeur vide est acceptee. */
export function isValidImei(value: string): boolean {
  if (!/^\d{15}$/.test(value)) {
    return false;
  }
  let sum = 0;
  for (let i = 0; i < 15; i++) {
    let digit = Number(value[i]);
    if (i % 2 === 1) {
      digit *= 2;
      if (digit > 9) {
        digit -= 9;
      }
    }
    sum += digit;
  }
  return sum % 10 === 0;
}

export const imeiValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = ((control.value as string | null) ?? '').trim();
  return value === '' || isValidImei(value) ? null : { imei: true };
};
