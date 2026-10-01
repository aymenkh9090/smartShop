import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';
import { map, startWith } from 'rxjs';

import { NotificationService } from '../../../core/services/notification.service';
import { PaymentRow, PaymentsEditor } from '../../../shared/components/payments-editor/payments-editor';
import { fieldErrorMessage } from '../../../shared/utils/form-errors';
import { fromMillimes, lineAmount, sumAmounts, toMillimes } from '../../../shared/utils/money';
import { CustomerPicker } from '../../customers/customer-picker/customer-picker';
import { Customer } from '../../customers/customer.models';
import { ProductPicker } from '../../products/product-picker/product-picker';
import { Product } from '../../products/product.models';
import { ProductUnit } from '../../stock/stock.models';
import { StockService } from '../../stock/stock.service';
import { SaleRequest } from '../sale.models';
import { SaleService } from '../sale.service';

type SaleLine = FormGroup<{
  product: FormControl<Product>;
  quantity: FormControl<number>;
  unitIds: FormControl<number[]>;
}>;

/** Format ISO (yyyy-MM-dd) d'une date locale, sans decalage de fuseau. */
function toIsoDate(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

/**
 * Saisie d'une vente (caisse). Les montants affiches sont un apercu ; le backend recalcule et valide tout.
 */
@Component({
  selector: 'app-sale-form',
  imports: [
    CurrencyPipe,
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatButtonModule,
    MatIconModule,
    ProductPicker,
    CustomerPicker,
    PaymentsEditor,
  ],
  providers: [provideNativeDateAdapter()],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './sale-form.html',
  styleUrl: './sale-form.scss',
})
export class SaleForm {
  private readonly saleService = inject(SaleService);
  private readonly stockService = inject(StockService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);

  protected readonly errorOf = fieldErrorMessage;
  protected readonly customer = signal<Customer | null>(null);
  /** Unites en stock disponibles par produit suivi a l'unite. */
  protected readonly availableUnits = signal<Partial<Record<number, ProductUnit[]>>>({});
  protected readonly submitting = signal(false);
  protected readonly minDueDate = new Date();

  protected readonly form = new FormGroup({
    lines: new FormArray<SaleLine>([]),
    discount: new FormControl<number | null>(null, Validators.min(0)),
    payments: new FormArray<PaymentRow>([]),
    dueDate: new FormControl<Date | null>(null),
    notes: new FormControl('', { nonNullable: true, validators: Validators.maxLength(1000) }),
  });

  private readonly value = toSignal(
    this.form.valueChanges.pipe(
      startWith(null),
      map(() => this.form.getRawValue()),
    ),
    { requireSync: true },
  );

  protected readonly subtotal = computed(() =>
    sumAmounts(this.value().lines.map((line) => lineAmount(line.product.salePrice, this.lineQuantity(line)))),
  );
  protected readonly total = computed(() =>
    fromMillimes(Math.max(0, toMillimes(this.subtotal()) - toMillimes(this.value().discount))),
  );
  protected readonly paid = computed(() => sumAmounts(this.value().payments.map((payment) => payment.amount)));
  protected readonly remaining = computed(() => fromMillimes(toMillimes(this.total()) - toMillimes(this.paid())));

  /** Memes regles que le backend, affichees avant l'envoi. */
  protected readonly blockingError = computed(() => {
    if (this.value().lines.length === 0) {
      return 'Ajoutez au moins un produit.';
    }
    if (toMillimes(this.value().discount) > toMillimes(this.subtotal())) {
      return 'La remise depasse le sous-total.';
    }
    if (this.remaining() < 0) {
      return 'Le montant paye depasse le total.';
    }
    if (this.remaining() > 0 && !this.customer()) {
      return 'Vente a credit : selectionnez un client.';
    }
    return null;
  });

  protected addProduct(product: Product): void {
    if (this.form.controls.lines.controls.some((line) => line.controls.product.value.id === product.id)) {
      this.notifications.error(null, `"${product.name}" est deja dans la vente : modifiez la quantite.`);
      return;
    }
    if (product.quantity <= 0) {
      this.notifications.error(null, `"${product.name}" est en rupture de stock.`);
      return;
    }
    this.form.controls.lines.push(
      new FormGroup({
        product: new FormControl(product, { nonNullable: true }),
        quantity: new FormControl(1, {
          nonNullable: true,
          validators: [Validators.required, Validators.min(1), Validators.max(product.quantity)],
        }),
        unitIds: new FormControl<number[]>([], {
          nonNullable: true,
          validators: product.serialTracked ? Validators.required : [],
        }),
      }),
    );
    if (product.serialTracked) {
      this.loadUnits(product.id);
    }
  }

  protected removeLine(index: number): void {
    this.form.controls.lines.removeAt(index);
  }

  protected selectCustomer(customer: Customer): void {
    this.customer.set(customer);
  }

  protected clearCustomer(): void {
    this.customer.set(null);
  }

  protected lineTotal(line: SaleLine): number {
    return lineAmount(line.controls.product.value.salePrice, this.lineQuantity(line.getRawValue()));
  }

  protected unitLabel(unit: ProductUnit): string {
    return unit.imei ?? `SN ${unit.serialNumber}`;
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const error = this.blockingError();
    if (error) {
      this.notifications.error(null, error);
      return;
    }
    const value = this.form.getRawValue();
    const hasBalance = this.remaining() > 0;
    const request: SaleRequest = {
      customerId: this.customer()?.id ?? null,
      items: value.lines.map((line) => ({
        productId: line.product.id,
        quantity: this.lineQuantity(line),
        unitIds: line.product.serialTracked ? line.unitIds : null,
      })),
      discount: value.discount,
      payments: value.payments.map((payment) => ({
        amount: payment.amount!,
        method: payment.method,
        reference: payment.reference.trim() || null,
      })),
      dueDate: hasBalance && value.dueDate ? toIsoDate(value.dueDate) : null,
      notes: value.notes.trim() || null,
    };

    this.submitting.set(true);
    this.saleService.create(request).subscribe({
      next: (sale) => {
        this.notifications.success(`Vente ${sale.reference} enregistree`);
        void this.router.navigate(['/sales', sale.id]);
      },
      error: (err: unknown) => {
        this.submitting.set(false);
        this.notifications.error(err);
      },
    });
  }

  /** Produit suivi a l'unite : la quantite est le nombre d'unites choisies. */
  private lineQuantity(line: { product: Product; quantity: number; unitIds: number[] }): number {
    return line.product.serialTracked ? line.unitIds.length : line.quantity;
  }

  private loadUnits(productId: number): void {
    this.stockService
      .searchUnits({ productId, status: 'IN_STOCK' }, { page: 0, size: 100, sort: 'createdAt,asc' })
      .subscribe({
        next: (page) => this.availableUnits.update((units) => ({ ...units, [productId]: page.content })),
        error: (err: unknown) => this.notifications.error(err),
      });
  }
}
