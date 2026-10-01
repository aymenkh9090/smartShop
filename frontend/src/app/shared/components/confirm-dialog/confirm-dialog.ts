import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Observable, map } from 'rxjs';

export interface ConfirmDialogData {
  title: string;
  message: string;
  confirmLabel?: string;
}

@Component({
  selector: 'app-confirm-dialog',
  imports: [MatDialogModule, MatButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>{{ data.message }}</mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Annuler</button>
      <button mat-flat-button [mat-dialog-close]="true">{{ data.confirmLabel ?? 'Confirmer' }}</button>
    </mat-dialog-actions>
  `,
})
export class ConfirmDialog {
  protected readonly data = inject<ConfirmDialogData>(MAT_DIALOG_DATA);
}

/** Ouvre une confirmation ; emet true uniquement si l'utilisateur confirme. */
export function confirmAction(dialog: MatDialog, data: ConfirmDialogData): Observable<boolean> {
  return dialog
    .open(ConfirmDialog, { data, width: '420px' })
    .afterClosed()
    .pipe(map((result) => result === true));
}
