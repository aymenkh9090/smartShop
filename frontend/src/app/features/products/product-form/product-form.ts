import { ChangeDetectionStrategy, Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';

import { NotificationService } from '../../../core/services/notification.service';
import { applyServerErrors, fieldErrorMessage } from '../../../shared/utils/form-errors';
import { CategoryService } from '../../categories/category.service';
import {
  PRODUCT_CONDITIONS,
  PRODUCT_CONDITION_LABELS,
  PRODUCT_TYPES,
  PRODUCT_TYPE_LABELS,
  ProductCondition,
  ProductRequest,
  ProductType,
} from '../product.models';
import { ProductService } from '../product.service';

const CODE_PATTERN = /^\s*[A-Za-z0-9._-]+\s*$/;
const BARCODE_PATTERN = /^\s*[A-Za-z0-9-]*\s*$/;

@Component({
  selector: 'app-product-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatButtonModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './product-form.html',
})
export class ProductForm implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);

  /** Parametre de route :id (absent en creation). */
  readonly id = input<string>();
  protected readonly isEdit = computed(() => !!this.id());
  protected readonly submitting = signal(false);
  protected readonly errorOf = fieldErrorMessage;
  protected readonly types = PRODUCT_TYPES;
  protected readonly typeLabels = PRODUCT_TYPE_LABELS;
  protected readonly conditions = PRODUCT_CONDITIONS;
  protected readonly conditionLabels = PRODUCT_CONDITION_LABELS;
  protected readonly categories = toSignal(
    inject(CategoryService).list().pipe(catchError(() => of([]))),
    { initialValue: [] },
  );

  protected readonly form = new FormGroup({
    sku: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(50), Validators.pattern(CODE_PATTERN)],
    }),
    barcode: new FormControl('', {
      nonNullable: true,
      validators: [Validators.maxLength(50), Validators.pattern(BARCODE_PATTERN)],
    }),
    reference: new FormControl('', { nonNullable: true, validators: Validators.maxLength(100) }),
    name: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(150)] }),
    description: new FormControl('', { nonNullable: true, validators: Validators.maxLength(2000) }),
    categoryId: new FormControl<number | null>(null, Validators.required),
    type: new FormControl<ProductType | null>(null, Validators.required),
    condition: new FormControl<ProductCondition>('NEW', { nonNullable: true, validators: Validators.required }),
    brand: new FormControl('', { nonNullable: true, validators: Validators.maxLength(100) }),
    model: new FormControl('', { nonNullable: true, validators: Validators.maxLength(100) }),
    purchasePrice: new FormControl<number | null>(null, [Validators.required, Validators.min(0)]),
    salePrice: new FormControl<number | null>(null, [Validators.required, Validators.min(0)]),
    minStock: new FormControl(0, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(0), Validators.max(1_000_000)],
    }),
    serialTracked: new FormControl(false, { nonNullable: true }),
    warrantyMonths: new FormControl<number | null>(null, [Validators.min(0), Validators.max(120)]),
  });

  ngOnInit(): void {
    const id = this.id();
    if (id) {
      this.productService.get(Number(id)).subscribe({
        next: (product) =>
          this.form.patchValue({
            ...product,
            barcode: product.barcode ?? '',
            reference: product.reference ?? '',
            description: product.description ?? '',
            brand: product.brand ?? '',
            model: product.model ?? '',
            categoryId: product.category.id,
          }),
        error: (error: unknown) => this.notifications.error(error),
      });
    }
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    // Les validateurs garantissent la presence des champs obligatoires
    const request = this.form.getRawValue() as ProductRequest;
    const id = this.id();
    const save$ = id ? this.productService.update(Number(id), request) : this.productService.create(request);
    save$.subscribe({
      next: () => {
        this.notifications.success(id ? 'Produit modifie' : 'Produit cree');
        void this.router.navigate(['/products']);
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        applyServerErrors(this.form, error);
        this.notifications.error(error);
      },
    });
  }
}
