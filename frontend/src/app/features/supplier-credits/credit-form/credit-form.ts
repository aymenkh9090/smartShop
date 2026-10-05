import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';

import { NotificationService } from '../../../core/services/notification.service';
import { toIsoDate } from '../../../shared/components/reschedule-dialog/reschedule-dialog';
import { applyServerErrors, fieldErrorMessage } from '../../../shared/utils/form-errors';
import { SupplierPicker } from '../../suppliers/supplier-picker/supplier-picker';
import { Supplier } from '../../suppliers/supplier.models';
import { SupplierCreditService } from '../supplier-credit.service';

/**
 * Saisie simple d'un credit fournisseur : fournisseur, montant du, date. Les versements se font ensuite
 * depuis le detail du credit ; le systeme calcule le reste.
 */
@Component({
  selector: 'app-supplier-credit-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatButtonModule,
    MatIconModule,
    SupplierPicker,
  ],
  providers: [provideNativeDateAdapter()],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './credit-form.html',
  styleUrl: './credit-form.scss',
})
export class CreditForm {
  private readonly creditService = inject(SupplierCreditService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);

  protected readonly errorOf = fieldErrorMessage;
  protected readonly today = new Date();
  protected readonly supplier = signal<Supplier | null>(null);
  protected readonly supplierMissing = signal(false);
  protected readonly submitting = signal(false);

  protected readonly form = new FormGroup({
    amount: new FormControl<number | null>(null, [Validators.required, Validators.min(0.001)]),
    creditDate: new FormControl<Date | null>(new Date(), Validators.required),
    dueDate: new FormControl<Date | null>(null),
    description: new FormControl('', { nonNullable: true, validators: Validators.maxLength(255) }),
  });

  protected selectSupplier(supplier: Supplier): void {
    this.supplier.set(supplier);
    this.supplierMissing.set(false);
  }

  protected clearSupplier(): void {
    this.supplier.set(null);
  }

  protected submit(): void {
    const supplier = this.supplier();
    this.supplierMissing.set(!supplier);
    if (this.form.invalid || !supplier) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.submitting.set(true);
    this.creditService
      .create({
        supplierId: supplier.id,
        amount: value.amount!,
        creditDate: value.creditDate ? toIsoDate(value.creditDate) : null,
        dueDate: value.dueDate ? toIsoDate(value.dueDate) : null,
        description: value.description.trim() || null,
      })
      .subscribe({
        next: (credit) => {
          this.notifications.success(`Crédit ${credit.reference} enregistré`);
          void this.router.navigate(['/supplier-credits', 'manual', credit.id]);
        },
        error: (error: unknown) => {
          this.submitting.set(false);
          applyServerErrors(this.form, error);
          this.notifications.error(error);
        },
      });
  }
}
