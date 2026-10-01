import { AbstractControl, FormGroup } from '@angular/forms';

import { apiFieldErrors } from '../../core/utils/api-error.utils';

/** Reporte les erreurs de validation du backend sur les champs du formulaire correspondants. */
export function applyServerErrors(form: FormGroup, error: unknown): void {
  for (const fieldError of apiFieldErrors(error)) {
    const control = form.get(fieldError.field);
    if (control) {
      control.setErrors({ ...(control.errors ?? {}), server: fieldError.message });
      control.markAsTouched();
    }
  }
}

/** Message a afficher dans <mat-error> pour un champ invalide. */
export function fieldErrorMessage(control: AbstractControl): string {
  const errors = control.errors;
  if (!errors) {
    return '';
  }
  if (errors['required']) {
    return 'Champ obligatoire';
  }
  if (errors['maxlength']) {
    return `${errors['maxlength'].requiredLength} caracteres maximum`;
  }
  if (errors['minlength']) {
    return `${errors['minlength'].requiredLength} caracteres minimum`;
  }
  if (errors['email']) {
    return 'Email invalide';
  }
  if (errors['min']) {
    return `Valeur minimale : ${errors['min'].min}`;
  }
  if (errors['max']) {
    return `Valeur maximale : ${errors['max'].max}`;
  }
  if (errors['pattern']) {
    return 'Format invalide';
  }
  if (errors['server']) {
    return errors['server'];
  }
  return 'Valeur invalide';
}
