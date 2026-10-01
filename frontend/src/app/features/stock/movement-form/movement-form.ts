import { ChangeDetectionStrategy, Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
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
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule, MatSelectionList } from '@angular/material/list';
import { MatSelectModule } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';

import { NotificationService } from '../../../core/services/notification.service';
import { applyServerErrors, fieldErrorMessage } from '../../../shared/utils/form-errors';
import { imeiValidator } from '../../../shared/validators/imei.validator';
import { ProductPicker } from '../../products/product-picker/product-picker';
import { Product } from '../../products/product.models';
import { ProductService } from '../../products/product.service';
import {
  MOVEMENT_TYPES,
  MOVEMENT_TYPE_LABELS,
  ProductUnit,
  StockMovementRequest,
  StockMovementType,
} from '../stock.models';
import { StockService } from '../stock.service';

/**
 * none     : produit simple, quantite saisie
 * new      : produit suivi, entree -> une ligne (IMEI / numero de serie) par unite
 * existing : produit suivi, sortie ou retour -> selection des unites existantes
 */
type UnitMode = 'none' | 'new' | 'existing';

type UnitRow = FormGroup<{ imei: FormControl<string>; serialNumber: FormControl<string> }>;

const nonZero = (control: AbstractControl): ValidationErrors | null =>
  control.value === 0 ? { nonZero: true } : null;

const identifierRequired = (group: AbstractControl): ValidationErrors | null => {
  const { imei, serialNumber } = group.value as { imei: string; serialNumber: string };
  return imei?.trim() || serialNumber?.trim() ? null : { identifierRequired: true };
};

@Component({
  selector: 'app-movement-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatListModule,
    ProductPicker,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './movement-form.html',
  styleUrl: './movement-form.scss',
})
export class MovementForm implements OnInit {
  private readonly stockService = inject(StockService);
  private readonly productService = inject(ProductService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);

  /** Produit preselectionne (?productId=), ex. depuis la liste des produits. */
  readonly productId = input<string>();

  protected readonly errorOf = fieldErrorMessage;
  protected readonly typeLabels = MOVEMENT_TYPE_LABELS;
  protected readonly product = signal<Product | null>(null);
  protected readonly selectableUnits = signal<ProductUnit[]>([]);
  protected readonly selectedUnitIds = signal<number[]>([]);
  protected readonly submitting = signal(false);

  protected readonly form = new FormGroup({
    type: new FormControl<StockMovementType>('STOCK_IN', { nonNullable: true }),
    quantity: new FormControl<number | null>(1),
    reason: new FormControl('', { nonNullable: true }),
    reference: new FormControl('', { nonNullable: true, validators: Validators.maxLength(100) }),
    units: new FormArray<UnitRow>([]),
  });

  protected readonly type = toSignal(this.form.controls.type.valueChanges, {
    initialValue: this.form.controls.type.value,
  });

  protected readonly unitMode = computed<UnitMode>(() => {
    if (!this.product()?.serialTracked) {
      return 'none';
    }
    return this.type() === 'STOCK_IN' ? 'new' : 'existing';
  });

  /** L'ajustement d'un produit suivi se fait par entree / sortie d'unites identifiees. */
  protected readonly availableTypes = computed(() =>
    this.product()?.serialTracked ? MOVEMENT_TYPES.filter((type) => type !== 'ADJUSTMENT') : MOVEMENT_TYPES,
  );

  constructor() {
    this.form.controls.type.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => this.configure());
    this.configure();
  }

  ngOnInit(): void {
    const id = this.productId();
    if (id) {
      this.productService.get(Number(id)).subscribe({
        next: (product) => this.selectProduct(product),
        error: (error: unknown) => this.notifications.error(error),
      });
    }
  }

  protected selectProduct(product: Product): void {
    this.product.set(product);
    if (product.serialTracked && this.form.controls.type.value === 'ADJUSTMENT') {
      this.form.controls.type.setValue('STOCK_IN');
    } else {
      this.configure();
    }
  }

  protected changeProduct(): void {
    this.product.set(null);
    this.configure();
  }

  protected addUnitRow(): void {
    this.form.controls.units.push(
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

  protected removeUnitRow(index: number): void {
    this.form.controls.units.removeAt(index);
  }

  protected onUnitSelection(list: MatSelectionList): void {
    this.selectedUnitIds.set(list.selectedOptions.selected.map((option) => option.value as number));
  }

  protected submit(): void {
    const product = this.product();
    if (!product) {
      this.notifications.error(null, 'Selectionnez un produit.');
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const mode = this.unitMode();
    if (mode === 'existing' && this.selectedUnitIds().length === 0) {
      this.notifications.error(null, 'Selectionnez au moins une unite.');
      return;
    }
    const value = this.form.getRawValue();
    const request: StockMovementRequest = {
      productId: product.id,
      type: value.type,
      quantity:
        mode === 'new' ? value.units.length : mode === 'existing' ? this.selectedUnitIds().length : value.quantity!,
      reason: value.reason.trim() || null,
      reference: value.reference.trim() || null,
      units:
        mode === 'new'
          ? value.units.map((unit) => ({
              imei: unit.imei.trim() || null,
              serialNumber: unit.serialNumber.trim() || null,
            }))
          : null,
      unitIds: mode === 'existing' ? this.selectedUnitIds() : null,
    };

    this.submitting.set(true);
    this.stockService.recordMovement(request).subscribe({
      next: (movement) => {
        this.notifications.success(`Mouvement enregistre (stock : ${movement.quantityAfter})`);
        void this.router.navigate(['/stock']);
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        applyServerErrors(this.form, error);
        this.notifications.error(error);
      },
    });
  }

  /** Adapte les validations et la saisie des unites au produit et au type de mouvement. */
  private configure(): void {
    const type = this.form.controls.type.value;
    const mode = this.unitMode();
    const quantity = this.form.controls.quantity;
    if (mode === 'none') {
      quantity.enable({ emitEvent: false });
      quantity.setValidators(
        type === 'ADJUSTMENT' ? [Validators.required, nonZero] : [Validators.required, Validators.min(1)],
      );
    } else {
      quantity.disable({ emitEvent: false });
    }
    quantity.updateValueAndValidity({ emitEvent: false });

    const reason = this.form.controls.reason;
    reason.setValidators(
      type === 'STOCK_IN' ? Validators.maxLength(255) : [Validators.required, Validators.maxLength(255)],
    );
    reason.updateValueAndValidity({ emitEvent: false });

    const units = this.form.controls.units;
    if (mode === 'new' && units.length === 0) {
      this.addUnitRow();
    } else if (mode !== 'new') {
      units.clear({ emitEvent: false });
    }

    this.selectedUnitIds.set([]);
    this.selectableUnits.set([]);
    if (mode === 'existing') {
      this.loadSelectableUnits(type);
    }
  }

  private loadSelectableUnits(type: StockMovementType): void {
    const product = this.product();
    if (!product) {
      return;
    }
    // Sortie : unites en stock. Retour : unites vendues ou sorties (tout sauf IN_STOCK).
    const returning = type === 'RETURN';
    this.stockService
      .searchUnits(
        { productId: product.id, status: returning ? null : 'IN_STOCK' },
        { page: 0, size: 100, sort: 'createdAt,desc' },
      )
      .subscribe({
        next: (page) =>
          this.selectableUnits.set(returning ? page.content.filter((unit) => unit.status !== 'IN_STOCK') : page.content),
        error: (error: unknown) => this.notifications.error(error),
      });
  }
}
