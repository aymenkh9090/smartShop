import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, booleanAttribute, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { catchError, debounceTime, filter, finalize, of, switchMap, tap } from 'rxjs';

import { PageRequest, emptyPage } from '../../../core/models/page';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { confirmAction } from '../../../shared/components/confirm-dialog/confirm-dialog';
import { CategoryService } from '../../categories/category.service';
import { PRODUCT_TYPES, PRODUCT_TYPE_LABELS, Product, ProductFilter, ProductType } from '../product.models';
import { ProductService } from '../product.service';

const DEFAULT_SORT = 'name,asc';

interface ProductQuery extends PageRequest {
  filter: ProductFilter;
}

@Component({
  selector: 'app-product-list',
  imports: [
    CurrencyPipe,
    ReactiveFormsModule,
    RouterLink,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatCheckboxModule,
    MatIconModule,
    MatProgressBarModule,
    MatTooltipModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './product-list.html',
})
export class ProductList implements OnInit {
  /** Alertes de stock (?lowStock=true), ex. depuis le tableau de bord. */
  readonly lowStock = input(false, { transform: booleanAttribute });

  private readonly productService = inject(ProductService);
  private readonly notifications = inject(NotificationService);
  private readonly dialog = inject(MatDialog);
  private readonly auth = inject(AuthService);

  protected readonly typeLabels = PRODUCT_TYPE_LABELS;
  protected readonly types = PRODUCT_TYPES;
  protected readonly canManage = computed(() => this.auth.hasAnyRole(['MANAGER']));
  protected readonly columns = computed(() => {
    const columns = ['sku', 'name', 'category', 'type', 'salePrice', 'quantity', 'status'];
    return this.canManage() ? [...columns, 'actions'] : columns;
  });

  protected readonly categories = toSignal(
    inject(CategoryService).list().pipe(catchError(() => of([]))),
    { initialValue: [] },
  );

  protected readonly filters = new FormGroup({
    q: new FormControl('', { nonNullable: true }),
    categoryId: new FormControl<number | null>(null),
    type: new FormControl<ProductType | null>(null),
    /** '' = tous, 'true' = actifs, 'false' = inactifs */
    active: new FormControl<'' | 'true' | 'false'>('true', { nonNullable: true }),
    lowStock: new FormControl(false, { nonNullable: true }),
  });

  protected readonly query = signal<ProductQuery>({
    filter: this.toFilter(),
    page: 0,
    size: 20,
    sort: DEFAULT_SORT,
  });
  protected readonly loading = signal(false);

  /** Chaque changement de query relance la recherche ; switchMap annule la requete precedente. */
  protected readonly page = toSignal(
    toObservable(this.query).pipe(
      tap(() => this.loading.set(true)),
      switchMap((query) =>
        this.productService.search(query.filter, query).pipe(
          catchError((error: unknown) => {
            this.notifications.error(error);
            return of(emptyPage<Product>());
          }),
          finalize(() => this.loading.set(false)),
        ),
      ),
    ),
    { initialValue: emptyPage<Product>() },
  );

  constructor() {
    this.filters.valueChanges
      .pipe(debounceTime(300), takeUntilDestroyed())
      .subscribe(() => this.query.update((query) => ({ ...query, filter: this.toFilter(), page: 0 })));
  }

  ngOnInit(): void {
    if (this.lowStock()) {
      this.filters.controls.lowStock.setValue(true, { emitEvent: false });
      this.query.update((query) => ({ ...query, filter: this.toFilter() }));
    }
  }

  protected typeLabel(type: ProductType): string {
    return PRODUCT_TYPE_LABELS[type];
  }

  protected onPage(event: PageEvent): void {
    this.query.update((query) => ({ ...query, page: event.pageIndex, size: event.pageSize }));
  }

  protected onSort(sort: Sort): void {
    const value = sort.direction ? `${sort.active},${sort.direction}` : DEFAULT_SORT;
    this.query.update((query) => ({ ...query, sort: value, page: 0 }));
  }

  protected toggleStatus(product: Product): void {
    const activate = !product.active;
    confirmAction(this.dialog, {
      title: activate ? 'Réactiver le produit' : 'Désactiver le produit',
      message: activate
        ? `Réactiver "${product.name}" ?`
        : `Désactiver "${product.name}" ? Il ne sera plus proposé à la vente mais reste dans l'historique.`,
      confirmLabel: activate ? 'Réactiver' : 'Désactiver',
    })
      .pipe(
        filter(Boolean),
        switchMap(() => this.productService.updateStatus(product.id, activate)),
      )
      .subscribe({
        next: () => {
          this.notifications.success(activate ? 'Produit réactivé' : 'Produit désactivé');
          this.query.update((query) => ({ ...query }));
        },
        error: (error: unknown) => this.notifications.error(error),
      });
  }

  private toFilter(): ProductFilter {
    const value = this.filters.getRawValue();
    return {
      q: value.q.trim() || null,
      categoryId: value.categoryId,
      type: value.type,
      active: value.active === '' ? null : value.active === 'true',
      lowStock: value.lowStock || null,
    };
  }
}
