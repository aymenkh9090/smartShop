import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, input, signal } from '@angular/core';
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
import { RouterLink } from '@angular/router';
import { catchError, debounceTime, finalize, of, switchMap, tap } from 'rxjs';

import { PageRequest, emptyPage } from '../../../core/models/page';
import { PAYMENT_STATUSES, PAYMENT_STATUS_LABELS, PaymentStatus } from '../../../core/models/payment';
import { NotificationService } from '../../../core/services/notification.service';
import { optionalNumberAttribute } from '../../../shared/utils/optional-number';
import { SALE_STATUS_LABELS, SaleFilter, SaleStatus, SaleSummary } from '../sale.models';
import { SaleService } from '../sale.service';

interface SaleQuery extends PageRequest {
  filter: SaleFilter;
}

@Component({
  selector: 'app-sale-list',
  imports: [
    CurrencyPipe,
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
  ],
  providers: [provideNativeDateAdapter()],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './sale-list.html',
})
export class SaleList implements OnInit {
  private readonly saleService = inject(SaleService);
  private readonly notifications = inject(NotificationService);

  /** Historique d'un client (?customerId=), ex. depuis la liste des clients. */
  readonly customerId = input(undefined, { transform: optionalNumberAttribute });

  protected readonly paymentStatuses = PAYMENT_STATUSES;
  protected readonly paymentStatusLabels = PAYMENT_STATUS_LABELS;
  protected readonly saleStatusLabels = SALE_STATUS_LABELS;
  protected readonly columns = ['createdAt', 'reference', 'customer', 'total', 'amountDue', 'paymentStatus', 'status'];

  protected readonly filters = new FormGroup({
    q: new FormControl('', { nonNullable: true }),
    status: new FormControl<SaleStatus | null>(null),
    paymentStatus: new FormControl<PaymentStatus | null>(null),
    from: new FormControl<Date | null>(null),
    to: new FormControl<Date | null>(null),
  });

  protected readonly query = signal<SaleQuery>({ filter: {}, page: 0, size: 20, sort: 'createdAt,desc' });
  protected readonly loading = signal(false);

  protected readonly page = toSignal(
    toObservable(this.query).pipe(
      tap(() => this.loading.set(true)),
      switchMap((query) =>
        this.saleService.search(query.filter, query).pipe(
          catchError((error: unknown) => {
            this.notifications.error(error);
            return of(emptyPage<SaleSummary>());
          }),
          finalize(() => this.loading.set(false)),
        ),
      ),
    ),
    { initialValue: emptyPage<SaleSummary>() },
  );

  constructor() {
    this.filters.valueChanges
      .pipe(debounceTime(300), takeUntilDestroyed())
      .subscribe(() => this.query.update((query) => ({ ...query, filter: this.toFilter(), page: 0 })));
  }

  ngOnInit(): void {
    this.query.update((query) => ({ ...query, filter: this.toFilter() }));
  }

  protected paymentStatusLabel(status: PaymentStatus): string {
    return PAYMENT_STATUS_LABELS[status];
  }

  protected saleStatusLabel(status: SaleStatus): string {
    return SALE_STATUS_LABELS[status];
  }

  protected onPage(event: PageEvent): void {
    this.query.update((query) => ({ ...query, page: event.pageIndex, size: event.pageSize }));
  }

  private toFilter(): SaleFilter {
    const value = this.filters.getRawValue();
    const to = value.to ? new Date(value.to.getFullYear(), value.to.getMonth(), value.to.getDate() + 1) : null;
    return {
      q: value.q.trim() || null,
      customerId: this.customerId() ?? null,
      status: value.status,
      paymentStatus: value.paymentStatus,
      from: value.from?.toISOString() ?? null,
      to: to?.toISOString() ?? null,
    };
  }
}
