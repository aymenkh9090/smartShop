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
import { imeiValidator } from '../../../shared/validators/imei.validator';
import { CustomerPicker } from '../../customers/customer-picker/customer-picker';
import { Customer } from '../../customers/customer.models';
import { ProductPicker } from '../../products/product-picker/product-picker';
import { Product } from '../../products/product.models';
import { AfterSalesCreateRequest } from '../after-sales.models';
import { AfterSalesService } from '../after-sales.service';

/**
 * Reception d'un appareil. Si l'IMEI correspond a un appareil vendu par la boutique,
 * le backend retrouve la vente d'origine et la garantie.
 */
@Component({
  selector: 'app-ticket-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatButtonModule,
    MatIconModule,
    CustomerPicker,
    ProductPicker,
  ],
  providers: [provideNativeDateAdapter()],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './ticket-form.html',
  styles: `
    .selected {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 1rem;
    }
  `,
})
export class TicketForm {
  private readonly afterSalesService = inject(AfterSalesService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);

  protected readonly errorOf = fieldErrorMessage;
  protected readonly customer = signal<Customer | null>(null);
  protected readonly product = signal<Product | null>(null);
  protected readonly submitting = signal(false);
  protected readonly minDate = new Date();

  protected readonly form = new FormGroup({
    deviceDescription: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(200)],
    }),
    imei: new FormControl('', { nonNullable: true, validators: imeiValidator }),
    serialNumber: new FormControl('', {
      nonNullable: true,
      validators: [Validators.maxLength(50), Validators.pattern(/^\s*[A-Za-z0-9/._-]*\s*$/)],
    }),
    reportedIssue: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(1000)],
    }),
    estimatedCost: new FormControl<number | null>(null, Validators.min(0)),
    expectedDate: new FormControl<Date | null>(null),
    notes: new FormControl('', { nonNullable: true, validators: Validators.maxLength(1000) }),
  });

  protected selectCustomer(customer: Customer): void {
    this.customer.set(customer);
  }

  protected selectProduct(product: Product): void {
    this.product.set(product);
    if (!this.form.controls.deviceDescription.value.trim()) {
      this.form.controls.deviceDescription.setValue(product.name);
    }
  }

  protected submit(): void {
    const customer = this.customer();
    if (this.form.invalid || !customer) {
      this.form.markAllAsTouched();
      if (!customer) {
        this.notifications.error(null, 'Sélectionnez le client.');
      }
      return;
    }
    const value = this.form.getRawValue();
    const request: AfterSalesCreateRequest = {
      customerId: customer.id,
      deviceDescription: value.deviceDescription.trim(),
      productId: this.product()?.id ?? null,
      imei: value.imei.trim() || null,
      serialNumber: value.serialNumber.trim() || null,
      reportedIssue: value.reportedIssue.trim(),
      estimatedCost: value.estimatedCost,
      expectedDate: value.expectedDate ? toIsoDate(value.expectedDate) : null,
      notes: value.notes.trim() || null,
    };
    this.submitting.set(true);
    this.afterSalesService.create(request).subscribe({
      next: (ticket) => {
        this.notifications.success(
          ticket.underWarranty ? `Ticket ${ticket.ticketNumber} créé (sous garantie)` : `Ticket ${ticket.ticketNumber} créé`,
        );
        void this.router.navigate(['/after-sales', ticket.id]);
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        applyServerErrors(this.form, error);
        this.notifications.error(error);
      },
    });
  }
}
