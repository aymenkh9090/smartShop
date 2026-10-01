import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormArray,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { map, startWith } from 'rxjs';

import { NotificationService } from '../../../core/services/notification.service';
import { PaymentRow, PaymentsEditor } from '../../../shared/components/payments-editor/payments-editor';
import { applyServerErrors, fieldErrorMessage } from '../../../shared/utils/form-errors';
import { fromMillimes, lineAmount, sumAmounts, toMillimes } from '../../../shared/utils/money';
import { imeiValidator } from '../../../shared/validators/imei.validator';
import { ProductPicker } from '../../products/product-picker/product-picker';
import { Product } from '../../products/product.models';
import { SupplierPicker } from '../../suppliers/supplier-picker/supplier-picker';
import { Supplier } from '../../suppliers/supplier.models';
import { PurchaseRequest } from '../purchase.models';
import { PurchaseService } from '../purchase.service';

type UnitRow = FormGroup<{ imei: FormControl<string>; serialNumber: FormControl<string> }>;

type PurchaseLine = FormGroup<{
  product: FormControl<Product>;
  quantity: FormControl<number>;
  unitCost: FormControl<number | null>;
  units: FormArray<UnitRow>;
}>;

const identifierRequired = (group: AbstractControl): ValidationErrors | null => {
  const { imei, serialNumber } = group.value as { imei: string; serialNumber: string };
  return imei?.trim() || serialNumber?.trim() ? null : { identifierRequired: true };
};

function toIsoDate(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

/**
 * Reception d'un achat fournisseur. Pour un produit suivi a l'unite, une ligne IMEI par article recu.
 */
@Component({
  selector: 'app-purchase-form',
  imports: [
    CurrencyPipe,
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatButtonModule,
    MatIconModule,
    ProductPicker,
    SupplierPicker,
    PaymentsEditor,
  ],
  providers: [provideNativeDateAdapter()],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './purchase-form.html',
  styleUrl: './purchase-form.scss',
})
export class PurchaseForm {
  private readonly purchaseService = inject(PurchaseService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);

  protected readonly errorOf = fieldErrorMessage;
  protected readonly supplier = signal<Supplier | null>(null);
  protected readonly submitting = signal(false);

  protected readonly form = new FormGroup({
    supplierInvoiceRef: new FormControl('', { nonNullable: true, validators: Validators.maxLength(100) }),
    lines: new FormArray<PurchaseLine>([]),
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

  protected readonly total = computed(() =>
    sumAmounts(this.value().lines.map((line) => lineAmount(line.unitCost ?? 0, this.lineQuantity(line)))),
  );
  protected readonly paid = computed(() => sumAmounts(this.value().payments.map((payment) => payment.amount)));
  protected readonly remaining = computed(() => fromMillimes(toMillimes(this.total()) - toMillimes(this.paid())));

  protected readonly blockingError = computed(() => {
    if (!this.supplier()) {
      return 'Selectionnez un fournisseur.';
    }
    if (this.value().lines.length === 0) {
      return 'Ajoutez au moins un produit.';
    }
    if (this.remaining() < 0) {
      return 'Le montant paye depasse le total.';
    }
    return null;
  });

  protected selectSupplier(supplier: Supplier): void {
    this.supplier.set(supplier);
  }

  protected clearSupplier(): void {
    this.supplier.set(null);
  }

  protected addProduct(product: Product): void {
    if (this.form.controls.lines.controls.some((line) => line.controls.product.value.id === product.id)) {
      this.notifications.error(null, `"${product.name}" est deja dans l'achat.`);
      return;
    }
    const line: PurchaseLine = new FormGroup({
      product: new FormControl(product, { nonNullable: true }),
      quantity: new FormControl(1, {
        nonNullable: true,
        validators: [Validators.required, Validators.min(1), Validators.max(100_000)],
      }),
      unitCost: new FormControl<number | null>(product.purchasePrice, [Validators.required, Validators.min(0)]),
      units: new FormArray<UnitRow>([]),
    });
    if (product.serialTracked) {
      this.addUnitRow(line);
    }
    this.form.controls.lines.push(line);
  }

  protected removeLine(index: number): void {
    this.form.controls.lines.removeAt(index);
  }

  protected addUnitRow(line: PurchaseLine): void {
    line.controls.units.push(
      new FormGroup(
        {
          imei: new FormControl('', { nonNullable: true, validators: imeiValidator }),
          serialNumber: new FormControl('', {
            nonNullable: true,
            validators: [Validators.maxLength(50), Validators.pattern(/^\s*[A-Za-z0-9/._-]*\s*$/)],
          }),
        },
        { validators: identifierRequired },
      ),
    );
  }

  protected removeUnitRow(line: PurchaseLine, index: number): void {
    line.controls.units.removeAt(index);
  }

  protected lineTotal(line: PurchaseLine): number {
    const value = line.getRawValue();
    return lineAmount(value.unitCost ?? 0, this.lineQuantity(value));
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
    const request: PurchaseRequest = {
      supplierId: this.supplier()!.id,
      supplierInvoiceRef: value.supplierInvoiceRef.trim() || null,
      items: value.lines.map((line) => ({
        productId: line.product.id,
        quantity: this.lineQuantity(line),
        unitCost: line.unitCost!,
        units: line.product.serialTracked
          ? line.units.map((unit) => ({
              imei: unit.imei.trim() || null,
              serialNumber: unit.serialNumber.trim() || null,
            }))
          : null,
      })),
      payments: value.payments.map((payment) => ({
        amount: payment.amount!,
        method: payment.method,
        reference: payment.reference.trim() || null,
      })),
      dueDate: this.remaining() > 0 && value.dueDate ? toIsoDate(value.dueDate) : null,
      notes: value.notes.trim() || null,
    };

    this.submitting.set(true);
    this.purchaseService.create(request).subscribe({
      next: (purchase) => {
        this.notifications.success(`Achat ${purchase.reference} enregistre`);
        void this.router.navigate(['/purchases', purchase.id]);
      },
      error: (err: unknown) => {
        this.submitting.set(false);
        applyServerErrors(this.form, err);
        this.notifications.error(err);
      },
    });
  }

  /** Produit suivi a l'unite : la quantite est le nombre d'unites saisies. */
  private lineQuantity(line: { product: Product; quantity: number; units: unknown[] }): number {
    return line.product.serialTracked ? line.units.length : line.quantity;
  }
}
