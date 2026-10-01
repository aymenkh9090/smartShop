import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
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
import { Supplier, SupplierFilter } from '../supplier.models';
import { SupplierService } from '../supplier.service';

const DEFAULT_SORT = 'name,asc';

interface SupplierQuery extends PageRequest {
  filter: SupplierFilter;
}

@Component({
  selector: 'app-supplier-list',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatTooltipModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './supplier-list.html',
})
export class SupplierList {
  private readonly supplierService = inject(SupplierService);
  private readonly notifications = inject(NotificationService);
  private readonly dialog = inject(MatDialog);
  private readonly auth = inject(AuthService);

  protected readonly canEdit = computed(() => this.auth.hasAnyRole(['MANAGER']));
  protected readonly canChangeStatus = computed(() => this.auth.hasAnyRole(['MANAGER']));
  protected readonly columns = computed(() => {
    const columns = ['name', 'phone', 'email', 'taxId', 'status'];
    return this.canEdit() || this.canChangeStatus() ? [...columns, 'actions'] : columns;
  });

  protected readonly filters = new FormGroup({
    q: new FormControl('', { nonNullable: true }),
    /** '' = tous, 'true' = actifs, 'false' = inactifs */
    active: new FormControl<'' | 'true' | 'false'>('true', { nonNullable: true }),
  });

  protected readonly query = signal<SupplierQuery>({
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
        this.supplierService.search(query.filter, query).pipe(
          catchError((error: unknown) => {
            this.notifications.error(error);
            return of(emptyPage<Supplier>());
          }),
          finalize(() => this.loading.set(false)),
        ),
      ),
    ),
    { initialValue: emptyPage<Supplier>() },
  );

  constructor() {
    this.filters.valueChanges
      .pipe(debounceTime(300), takeUntilDestroyed())
      .subscribe(() => this.query.update((query) => ({ ...query, filter: this.toFilter(), page: 0 })));
  }

  protected onPage(event: PageEvent): void {
    this.query.update((query) => ({ ...query, page: event.pageIndex, size: event.pageSize }));
  }

  protected onSort(sort: Sort): void {
    const value = sort.direction ? `${sort.active},${sort.direction}` : DEFAULT_SORT;
    this.query.update((query) => ({ ...query, sort: value, page: 0 }));
  }

  protected toggleStatus(item: Supplier): void {
    const activate = !item.active;
    const label = item.name;
    confirmAction(this.dialog, {
      title: activate ? 'Reactiver le fournisseur' : 'Desactiver le fournisseur',
      message: activate ? `Reactiver "${label}" ?` : `Desactiver "${label}" ? L'historique est conserve.`,
      confirmLabel: activate ? 'Reactiver' : 'Desactiver',
    })
      .pipe(
        filter(Boolean),
        switchMap(() => this.supplierService.updateStatus(item.id, activate)),
      )
      .subscribe({
        next: () => {
          this.notifications.success(activate ? 'Fournisseur reactive' : 'Fournisseur desactive');
          this.query.update((query) => ({ ...query }));
        },
        error: (error: unknown) => this.notifications.error(error),
      });
  }

  private toFilter(): SupplierFilter {
    const value = this.filters.getRawValue();
    return {
      q: value.q.trim() || null,
      active: value.active === '' ? null : value.active === 'true',
    };
  }
}
