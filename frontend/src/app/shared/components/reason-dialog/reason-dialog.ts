import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Observable } from 'rxjs';

export interface ReasonDialogData {
  title: string;
  message: string;
  confirmLabel: string;
}

/** Confirmation qui exige un motif (annulation, etc.). */
@Component({
  selector: 'app-reason-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      <p>{{ data.message }}</p>
      <mat-form-field class="reason">
        <mat-label>Motif</mat-label>
        <textarea matInput [formControl]="reason" rows="3" maxlength="200"></textarea>
        <mat-error>Le motif est obligatoire</mat-error>
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Retour</button>
      <button mat-flat-button type="button" (click)="confirm()">{{ data.confirmLabel }}</button>
    </mat-dialog-actions>
  `,
  styles: `
    .reason {
      width: 100%;
    }
  `,
})
export class ReasonDialog {
  protected readonly data = inject<ReasonDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<ReasonDialog, string>);
  protected readonly reason = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(200)],
  });

  protected confirm(): void {
    if (this.reason.invalid) {
      this.reason.markAsTouched();
      return;
    }
    this.dialogRef.close(this.reason.value.trim());
  }
}

/** Emet le motif saisi, ou undefined si l'utilisateur renonce. */
export function askReason(dialog: MatDialog, data: ReasonDialogData): Observable<string | undefined> {
  return dialog.open(ReasonDialog, { data, width: '460px' }).afterClosed();
}
