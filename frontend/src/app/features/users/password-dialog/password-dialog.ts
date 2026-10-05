import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Observable } from 'rxjs';

import { fieldErrorMessage } from '../../../shared/utils/form-errors';
import { PASSWORD_VALIDATORS, generatePassword, passwordsMatch } from '../password';

export interface PasswordDialogData {
  /** Nom affiche de l'utilisateur concerne. */
  name: string;
}

/** Saisie d'un nouveau mot de passe par un ADMIN ; emet le mot de passe, ou undefined si abandon. */
@Component({
  selector: 'app-password-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatTooltipModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>Réinitialiser le mot de passe</h2>
    <form [formGroup]="form" (ngSubmit)="confirm()" novalidate>
      <mat-dialog-content>
        <p class="intro">
          Nouveau mot de passe pour <strong>{{ data.name }}</strong>. Ses sessions ouvertes seront fermées.
        </p>
        <mat-form-field class="full">
          <mat-label>Nouveau mot de passe</mat-label>
          <input matInput formControlName="password" [type]="visible() ? 'text' : 'password'"
                 autocomplete="new-password" />
          <button mat-icon-button matSuffix type="button" (click)="visible.set(!visible())"
                  [attr.aria-label]="visible() ? 'Masquer' : 'Afficher'">
            <mat-icon>{{ visible() ? 'visibility_off' : 'visibility' }}</mat-icon>
          </button>
          <mat-hint>8 caractères minimum</mat-hint>
          <mat-error>{{ errorOf(form.controls.password) }}</mat-error>
        </mat-form-field>
        <mat-form-field class="full">
          <mat-label>Confirmer le mot de passe</mat-label>
          <input matInput formControlName="confirm" [type]="visible() ? 'text' : 'password'"
                 autocomplete="new-password" />
          <mat-error>{{ form.controls.confirm.hasError('mismatch') ? 'Les mots de passe ne correspondent pas' : errorOf(form.controls.confirm) }}</mat-error>
        </mat-form-field>
        <button mat-stroked-button type="button" (click)="generate()">
          <mat-icon>auto_awesome</mat-icon> Générer un mot de passe
        </button>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Annuler</button>
        <button mat-flat-button type="submit">Enregistrer</button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    .intro {
      margin-top: 0;
      color: var(--app-text-muted);
    }
    .full {
      width: 100%;
    }
  `,
})
export class PasswordDialog {
  protected readonly data = inject<PasswordDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<PasswordDialog, string>);
  protected readonly visible = signal(false);
  protected readonly errorOf = fieldErrorMessage;

  protected readonly form = inject(FormBuilder).nonNullable.group(
    {
      password: ['', PASSWORD_VALIDATORS],
      confirm: ['', Validators.required],
    },
    { validators: passwordsMatch },
  );

  protected generate(): void {
    const password = generatePassword();
    this.form.setValue({ password, confirm: password });
    this.visible.set(true);
  }

  protected confirm(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.dialogRef.close(this.form.controls.password.value);
  }
}

export function askNewPassword(dialog: MatDialog, data: PasswordDialogData): Observable<string | undefined> {
  return dialog.open(PasswordDialog, { data, width: '460px' }).afterClosed();
}
