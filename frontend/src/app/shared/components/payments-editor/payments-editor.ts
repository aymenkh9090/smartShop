import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { PAYMENT_METHODS, PAYMENT_METHOD_LABELS, PaymentMethod } from '../../../core/models/payment';
import { fieldErrorMessage } from '../../utils/form-errors';

export type PaymentRow = FormGroup<{
  amount: FormControl<number | null>;
  method: FormControl<PaymentMethod>;
  reference: FormControl<string>;
}>;

export function createPaymentRow(amount: number | null = null, method: PaymentMethod = 'CASH'): PaymentRow {
  return new FormGroup({
    amount: new FormControl<number | null>(amount, [Validators.required, Validators.min(0.001)]),
    method: new FormControl<PaymentMethod>(method, { nonNullable: true }),
    reference: new FormControl('', { nonNullable: true, validators: Validators.maxLength(100) }),
  });
}

/**
 * Saisie des paiements d'une vente ou d'un achat (plusieurs moyens possibles).
 * Le bouton "Regler le reste" ajoute une ligne du montant restant du.
 */
@Component({
  selector: 'app-payments-editor',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @for (row of payments().controls; track row; let i = $index) {
      <div class="payment-row" [formGroup]="row">
        <mat-form-field>
          <mat-label>Montant (DT)</mat-label>
          <input matInput type="number" min="0" step="0.001" formControlName="amount" />
          <mat-error>{{ errorOf(row.controls.amount) }}</mat-error>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Moyen de paiement</mat-label>
          <mat-select formControlName="method">
            @for (method of methods; track method) {
              <mat-option [value]="method">{{ methodLabels[method] }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Référence (chèque, virement...)</mat-label>
          <input matInput formControlName="reference" />
        </mat-form-field>
        <button mat-icon-button type="button" (click)="payments().removeAt(i)" aria-label="Retirer ce paiement">
          <mat-icon>remove_circle_outline</mat-icon>
        </button>
      </div>
    }
    <div class="payment-actions">
      <button mat-stroked-button type="button" (click)="payments().push(createRow(null))">
        <mat-icon>add</mat-icon> Ajouter un paiement
      </button>
      @if (remaining() > 0) {
        <button mat-stroked-button type="button" (click)="payments().push(createRow(remaining()))">
          <mat-icon>payments</mat-icon> Régler le reste
        </button>
      }
    </div>
  `,
  styles: `
    .payment-row {
      display: flex;
      flex-wrap: wrap;
      gap: 0 1rem;
      align-items: flex-start;

      mat-form-field {
        flex: 1 1 180px;
      }
    }

    .payment-actions {
      display: flex;
      gap: 0.5rem;
      flex-wrap: wrap;
      margin-bottom: 1.25rem;
    }
  `,
})
export class PaymentsEditor {
  readonly payments = input.required<FormArray<PaymentRow>>();
  /** Reste a payer, propose par "Regler le reste". */
  readonly remaining = input(0);

  protected readonly methods = PAYMENT_METHODS;
  protected readonly methodLabels = PAYMENT_METHOD_LABELS;
  protected readonly errorOf = fieldErrorMessage;
  protected readonly createRow = createPaymentRow;
}
