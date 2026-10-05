import { ChangeDetectionStrategy, Component, inject, signal, viewChild } from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';

import { NotificationService } from '../../../core/services/notification.service';
import { fieldErrorMessage } from '../../../shared/utils/form-errors';
import { ProductPicker } from '../../products/product-picker/product-picker';
import { Product } from '../../products/product.models';
import { StockService } from '../stock.service';

type InventoryLine = FormGroup<{
  product: FormControl<Product>;
  countedQuantity: FormControl<number | null>;
}>;

/**
 * Comptage physique : pour chaque produit compte, un ajustement corrige l'ecart avec le stock theorique.
 */
@Component({
  selector: 'app-inventory',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    ProductPicker,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './inventory.html',
  styleUrl: './inventory.scss',
})
export class Inventory {
  private readonly stockService = inject(StockService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly picker = viewChild.required(ProductPicker);

  protected readonly errorOf = fieldErrorMessage;
  protected readonly submitting = signal(false);

  protected readonly form = new FormGroup({
    reference: new FormControl('', { nonNullable: true, validators: Validators.maxLength(100) }),
    reason: new FormControl('', { nonNullable: true, validators: Validators.maxLength(255) }),
    lines: new FormArray<InventoryLine>([]),
  });

  protected addProduct(product: Product): void {
    this.picker().clear();
    if (product.serialTracked) {
      this.notifications.error(null, `"${product.name}" est suivi à l'unité : ajustez-le par entrée / sortie d'unités.`);
      return;
    }
    if (this.form.controls.lines.controls.some((line) => line.controls.product.value.id === product.id)) {
      this.notifications.error(null, `"${product.name}" est déjà dans l'inventaire.`);
      return;
    }
    this.form.controls.lines.push(
      new FormGroup({
        product: new FormControl(product, { nonNullable: true }),
        countedQuantity: new FormControl<number | null>(null, [
          Validators.required,
          Validators.min(0),
          Validators.max(1_000_000),
        ]),
      }),
    );
  }

  protected removeLine(index: number): void {
    this.form.controls.lines.removeAt(index);
  }

  protected difference(line: InventoryLine): number | null {
    const counted = line.controls.countedQuantity.value;
    return counted === null ? null : counted - line.controls.product.value.quantity;
  }

  protected submit(): void {
    const lines = this.form.controls.lines;
    if (lines.length === 0) {
      this.notifications.error(null, 'Ajoutez au moins un produit compte.');
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.submitting.set(true);
    this.stockService
      .applyInventory({
        reference: value.reference.trim() || null,
        reason: value.reason.trim() || null,
        lines: value.lines.map((line) => ({ productId: line.product.id, countedQuantity: line.countedQuantity! })),
      })
      .subscribe({
        next: (result) => {
          this.notifications.success(
            `Inventaire enregistré : ${result.adjustedProducts} produit(s) ajusté(s) sur ${result.countedProducts}`,
          );
          void this.router.navigate(['/stock']);
        },
        error: (error: unknown) => {
          this.submitting.set(false);
          this.notifications.error(error);
        },
      });
  }
}
