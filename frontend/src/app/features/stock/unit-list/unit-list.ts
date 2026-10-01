import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { catchError, debounceTime, finalize, of, switchMap, tap } from 'rxjs';

import { PageRequest, emptyPage } from '../../../core/models/page';
import { NotificationService } from '../../../core/services/notification.service';
import {
  ProductUnit,
  ProductUnitFilter,
  ProductUnitStatus,
  UNIT_STATUSES,
  UNIT_STATUS_LABELS,
} from '../stock.models';
import { StockService } from '../stock.service';

interface UnitQuery extends PageRequest {
  filter: ProductUnitFilter;
}

/** Recherche d'unites par IMEI ou numero de serie (comptoir, SAV). */
@Component({
  selector: 'app-unit-list',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule,
    MatProgressBarModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './unit-list.html',
})
export class UnitList {
  private readonly stockService = inject(StockService);
  private readonly notifications = inject(NotificationService);

  protected readonly statuses = UNIT_STATUSES;
  protected readonly statusLabels = UNIT_STATUS_LABELS;
  protected readonly columns = ['imei', 'serialNumber', 'product', 'status', 'createdAt'];

  protected readonly filters = new FormGroup({
    q: new FormControl('', { nonNullable: true }),
    status: new FormControl<ProductUnitStatus | null>(null),
  });

  protected readonly query = signal<UnitQuery>({
    filter: this.toFilter(),
    page: 0,
    size: 20,
    sort: 'createdAt,desc',
  });
  protected readonly loading = signal(false);

  protected readonly page = toSignal(
    toObservable(this.query).pipe(
      tap(() => this.loading.set(true)),
      switchMap((query) =>
        this.stockService.searchUnits(query.filter, query).pipe(
          catchError((error: unknown) => {
            this.notifications.error(error);
            return of(emptyPage<ProductUnit>());
          }),
          finalize(() => this.loading.set(false)),
        ),
      ),
    ),
    { initialValue: emptyPage<ProductUnit>() },
  );

  constructor() {
    this.filters.valueChanges
      .pipe(debounceTime(300), takeUntilDestroyed())
      .subscribe(() => this.query.update((query) => ({ ...query, filter: this.toFilter(), page: 0 })));
  }

  protected statusLabel(status: ProductUnitStatus): string {
    return UNIT_STATUS_LABELS[status];
  }

  protected onPage(event: PageEvent): void {
    this.query.update((query) => ({ ...query, page: event.pageIndex, size: event.pageSize }));
  }

  private toFilter(): ProductUnitFilter {
    const value = this.filters.getRawValue();
    return { q: value.q.trim() || null, status: value.status };
  }
}
