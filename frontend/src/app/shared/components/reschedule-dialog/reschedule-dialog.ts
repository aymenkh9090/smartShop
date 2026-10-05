import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Observable } from 'rxjs';

export interface RescheduleDialogData {
  title: string;
  /** Echeance actuelle (yyyy-MM-dd) */
  dueDate: string | null;
}

/** Resultat : nouvelle echeance (yyyy-MM-dd) ou null pour la supprimer. */
export interface RescheduleResult {
  dueDate: string | null;
}

/** Format ISO (yyyy-MM-dd) d'une date locale, sans decalage de fuseau. */
export function toIsoDate(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

/** Date locale a partir de yyyy-MM-dd (new Date('yyyy-MM-dd') l'interpreterait en UTC). */
export function fromIsoDate(value: string): Date {
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
}

@Component({
  selector: 'app-reschedule-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatDatepickerModule],
  providers: [provideNativeDateAdapter()],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      <mat-form-field class="full">
        <mat-label>Nouvelle échéance</mat-label>
        <input matInput [matDatepicker]="picker" [min]="today" [formControl]="dueDate" />
        <mat-datepicker-toggle matIconSuffix [for]="picker" />
        <mat-datepicker #picker />
        <mat-hint>Laisser vide pour supprimer l'échéance</mat-hint>
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" mat-dialog-close>Annuler</button>
      <button mat-flat-button type="button" (click)="confirm()">Enregistrer</button>
    </mat-dialog-actions>
  `,
  styles: `
    .full {
      width: 100%;
    }
  `,
})
export class RescheduleDialog {
  protected readonly data = inject<RescheduleDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<RescheduleDialog, RescheduleResult>);
  protected readonly today = new Date();
  protected readonly dueDate = new FormControl<Date | null>(this.data.dueDate ? fromIsoDate(this.data.dueDate) : null);

  protected confirm(): void {
    const value = this.dueDate.value;
    this.dialogRef.close({ dueDate: value ? toIsoDate(value) : null });
  }
}

/** Emet la nouvelle echeance, ou undefined si l'utilisateur renonce. */
export function askNewDueDate(dialog: MatDialog, data: RescheduleDialogData): Observable<RescheduleResult | undefined> {
  return dialog.open(RescheduleDialog, { data, width: '400px' }).afterClosed();
}
