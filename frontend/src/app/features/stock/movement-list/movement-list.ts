import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { catchError, debounceTime, finalize, of, switchMap, tap } from 'rxjs';

import { PageRequest, emptyPage } from '../../../core/models/page';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import {
  MOVEMENT_TYPES,
  MOVEMENT_TYPE_LABELS,
  StockMovement,
  StockMovementFilter,
  StockMovementType,
  UnitSummary,
} from '../stock.models';
import { StockService } from '../stock.service';

interface MovementQuery extends PageRequest {
  filter: StockMovementFilter;
}

@Component({
  selector: 'app-movement-list',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    MatTableModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatTooltipModule,
  ],
  providers: [provideNativeDateAdapter()],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './movement-list.html',
})
export class MovementList {
  private readonly stockService = inject(StockService);
  private readonly notifications = inject(NotificationService);
  private readonly auth = inject(AuthService);

  protected readonly types = MOVEMENT_TYPES;
  protected readonly typeLabels = MOVEMENT_TYPE_LABELS;
  protected readonly canManage = computed(() => this.auth.hasAnyRole(['MANAGER']));
  protected readonly columns = [
    'createdAt',
    'product',
    'type',
    'quantityChange',
    'quantityAfter',
    'reason',
    'reference',
    'createdBy',
  ];

  protected readonly filters = new FormGroup({
    q: new FormControl('', { nonNullable: true }),
    type: new FormControl<StockMovementType | null>(null),
    from: new FormControl<Date | null>(null),
    to: new FormControl<Date | null>(null),
  });

  protected readonly query = signal<MovementQuery>({
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
        this.stockService.searchMovements(query.filter, query).pipe(
          catchError((error: unknown) => {
            this.notifications.error(error);
            return of(emptyPage<StockMovement>());
          }),
          finalize(() => this.loading.set(false)),
        ),
      ),
    ),
    { initialValue: emptyPage<StockMovement>() },
  );

  constructor() {
    this.filters.valueChanges
      .pipe(debounceTime(300), takeUntilDestroyed())
      .subscribe(() => this.query.update((query) => ({ ...query, filter: this.toFilter(), page: 0 })));
  }

  protected typeLabel(type: StockMovementType): string {
    return MOVEMENT_TYPE_LABELS[type];
  }

  protected unitsLabel(units: UnitSummary[]): string {
    return units.map((unit) => unit.imei ?? unit.serialNumber).join(', ');
  }

  protected onPage(event: PageEvent): void {
    this.query.update((query) => ({ ...query, page: event.pageIndex, size: event.pageSize }));
  }

  /** Dates choisies en heure locale : du debut du jour "du" au debut du lendemain du jour "au". */
  private toFilter(): StockMovementFilter {
    const value = this.filters.getRawValue();
    const to = value.to ? new Date(value.to.getFullYear(), value.to.getMonth(), value.to.getDate() + 1) : null;
    return {
      q: value.q.trim() || null,
      type: value.type,
      from: value.from?.toISOString() ?? null,
      to: to?.toISOString() ?? null,
    };
  }
}
