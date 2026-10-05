import { AbstractControl, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';

/** Memes bornes que le backend (BCrypt ignore au-dela de 72 octets). */
export const PASSWORD_VALIDATORS: ValidatorFn[] = [
  Validators.required,
  Validators.minLength(8),
  Validators.maxLength(72),
];

/** Sans caracteres ambigus (0/O, 1/l/I) : le mot de passe est souvent dicte ou recopie. */
const ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%*?';

export function generatePassword(length = 12): string {
  const values = crypto.getRandomValues(new Uint32Array(length));
  return Array.from(values, (value) => ALPHABET[value % ALPHABET.length]).join('');
}

/** Validateur de groupe : `confirm` doit etre egal a `password`. */
export function passwordsMatch(control: AbstractControl): ValidationErrors | null {
  const password = control.get('password')?.value;
  const confirm = control.get('confirm');
  if (confirm && confirm.value && password !== confirm.value) {
    confirm.setErrors({ ...(confirm.errors ?? {}), mismatch: true });
    return { mismatch: true };
  }
  if (confirm?.hasError('mismatch')) {
    const { mismatch: _mismatch, ...rest } = confirm.errors ?? {};
    confirm.setErrors(Object.keys(rest).length ? rest : null);
  }
  return null;
}
