import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Observable } from 'rxjs';

import { PAYMENT_METHODS, PAYMENT_METHOD_LABELS, PaymentMethod, PaymentRequest } from '../../../core/models/payment';
import { fieldErrorMessage } from '../../utils/form-errors';
import { fromIsoDate, toIsoDate } from '../reschedule-dialog/reschedule-dialog';

/** Jour local d'une date yyyy-MM-dd ou d'un instant ISO. */
function startOfLocalDay(value: string): Date {
  if (value.length === 10) {
    return fromIsoDate(value);
  }
  const date = new Date(value);
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

export interface PaymentDialogData {
  title: string;
  /** Reste du : montant propose et plafond du paiement. */
  amountDue: number;
  /**
   * Paiement sur un credit : demande aussi la date du versement (aujourd'hui par defaut),
   * pas avant cette date (yyyy-MM-dd ou instant ISO).
   */
  creditDate?: string;
}

/** Saisie d'un paiement ulterieur sur un credit (montant propose = reste du). */
@Component({
  selector: 'app-payment-dialog',
  imports: [
    CurrencyPipe,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
  ],
  providers: [provideNativeDateAdapter()],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <form [formGroup]="form" (ngSubmit)="confirm()">
      <mat-dialog-content>
        <p>Reste du : <strong>{{ data.amountDue | currency: 'TND' : 'symbol' : '1.3-3' }}</strong></p>
        <mat-form-field class="full">
          <mat-label>Montant (DT)</mat-label>
          <input matInput type="number" min="0.001" [max]="data.amountDue" step="0.001" formControlName="amount" />
          <mat-error>{{ errorOf(form.controls.amount) }}</mat-error>
        </mat-form-field>
        @if (withDate) {
          <mat-form-field class="full">
            <mat-label>Date du versement</mat-label>
            <input matInput [matDatepicker]="paidOnPicker" [min]="minDate" [max]="today" formControlName="paidOn" />
            <mat-datepicker-toggle matIconSuffix [for]="paidOnPicker" />
            <mat-datepicker #paidOnPicker />
            <mat-error>Date invalide</mat-error>
          </mat-form-field>
        }
        <mat-form-field class="full">
          <mat-label>Moyen de paiement</mat-label>
          <mat-select formControlName="method">
            @for (method of methods; track method) {
              <mat-option [value]="method">{{ methodLabels[method] }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field class="full">
          <mat-label>Référence (chèque, virement...)</mat-label>
          <input matInput formControlName="reference" />
          <mat-error>{{ errorOf(form.controls.reference) }}</mat-error>
        </mat-form-field>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Annuler</button>
        <button mat-flat-button type="submit">Enregistrer le paiement</button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    .full {
      width: 100%;
    }
  `,
})
export class PaymentDialog {
  protected readonly data = inject<PaymentDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<PaymentDialog, PaymentRequest>);
  protected readonly methods = PAYMENT_METHODS;
  protected readonly methodLabels = PAYMENT_METHOD_LABELS;
  protected readonly errorOf = fieldErrorMessage;
  protected readonly withDate = this.data.creditDate !== undefined;
  protected readonly today = new Date();
  protected readonly minDate = this.data.creditDate ? startOfLocalDay(this.data.creditDate) : null;

  protected readonly form = new FormGroup({
    amount: new FormControl<number | null>(this.data.amountDue, [
      Validators.required,
      Validators.min(0.001),
      Validators.max(this.data.amountDue),
    ]),
    method: new FormControl<PaymentMethod>('CASH', { nonNullable: true }),
    reference: new FormControl('', { nonNullable: true, validators: Validators.maxLength(100) }),
    paidOn: new FormControl<Date | null>(new Date(), this.withDate ? Validators.required : []),
  });

  protected confirm(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const payment: PaymentRequest = {
      amount: value.amount!,
      method: value.method,
      reference: value.reference.trim() || null,
    };
    if (this.withDate && value.paidOn) {
      payment.paidOn = toIsoDate(value.paidOn);
    }
    this.dialogRef.close(payment);
  }
}

/** Emet le paiement saisi, ou undefined si l'utilisateur renonce. */
export function askPayment(dialog: MatDialog, data: PaymentDialogData): Observable<PaymentRequest | undefined> {
  return dialog.open(PaymentDialog, { data, width: '440px' }).afterClosed();
}
