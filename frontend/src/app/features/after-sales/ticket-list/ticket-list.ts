import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
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
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { optionalNumberAttribute } from '../../../shared/utils/optional-number';
import {
  AFTER_SALES_STATUSES,
  AFTER_SALES_STATUS_LABELS,
  AfterSalesFilter,
  AfterSalesStatus,
  AfterSalesSummary,
  isClosed,
} from '../after-sales.models';
import { AfterSalesService } from '../after-sales.service';

/** Valeur du filtre statut : 'OPEN' = tickets en cours, '' = tous. */
type StatusChoice = 'OPEN' | '' | AfterSalesStatus;

interface TicketQuery extends PageRequest {
  filter: AfterSalesFilter;
}

@Component({
  selector: 'app-ticket-list',
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
    MatCheckboxModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './ticket-list.html',
})
export class TicketList implements OnInit {
  private readonly afterSalesService = inject(AfterSalesService);
  private readonly notifications = inject(NotificationService);
  private readonly auth = inject(AuthService);

  /** Tickets d'un client (?customerId=), ex. depuis la liste des clients. */
  readonly customerId = input(undefined, { transform: optionalNumberAttribute });
  /** Statut initial (?status=READY), ex. depuis le tableau de bord. */
  readonly status = input<AfterSalesStatus>();

  protected readonly statuses = AFTER_SALES_STATUSES;
  protected readonly statusLabels = AFTER_SALES_STATUS_LABELS;
  protected readonly canCreate = this.auth.hasAnyRole(['SELLER']);
  protected readonly isTechnician = this.auth.hasAnyRole(['TECHNICIAN']);
  protected readonly columns = [
    'receivedAt',
    'ticketNumber',
    'customer',
    'device',
    'technician',
    'status',
    'amountDue',
    'expectedDate',
  ];

  protected readonly filters = new FormGroup({
    q: new FormControl('', { nonNullable: true }),
    status: new FormControl<StatusChoice>('OPEN', { nonNullable: true }),
    mine: new FormControl(false, { nonNullable: true }),
  });

  protected readonly query = signal<TicketQuery>({ filter: { open: true }, page: 0, size: 20, sort: 'receivedAt,desc' });
  protected readonly loading = signal(false);

  protected readonly page = toSignal(
    toObservable(this.query).pipe(
      tap(() => this.loading.set(true)),
      switchMap((query) =>
        this.afterSalesService.search(query.filter, query).pipe(
          catchError((error: unknown) => {
            this.notifications.error(error);
            return of(emptyPage<AfterSalesSummary>());
          }),
          finalize(() => this.loading.set(false)),
        ),
      ),
    ),
    { initialValue: emptyPage<AfterSalesSummary>() },
  );

  constructor() {
    this.filters.valueChanges
      .pipe(debounceTime(300), takeUntilDestroyed())
      .subscribe(() => this.query.update((query) => ({ ...query, filter: this.toFilter(), page: 0 })));
  }

  ngOnInit(): void {
    const status = this.status();
    if (status && AFTER_SALES_STATUSES.includes(status)) {
      this.filters.controls.status.setValue(status, { emitEvent: false });
    }
    this.query.update((query) => ({ ...query, filter: this.toFilter() }));
  }

  protected statusLabel(status: AfterSalesStatus): string {
    return AFTER_SALES_STATUS_LABELS[status];
  }

  protected closed(status: AfterSalesStatus): boolean {
    return isClosed(status);
  }

  protected onPage(event: PageEvent): void {
    this.query.update((query) => ({ ...query, page: event.pageIndex, size: event.pageSize }));
  }

  private toFilter(): AfterSalesFilter {
    const value = this.filters.getRawValue();
    return {
      q: value.q.trim() || null,
      customerId: this.customerId() ?? null,
      technicianId: value.mine ? (this.auth.currentUser()?.id ?? null) : null,
      open: value.status === 'OPEN' ? true : null,
      status: value.status === 'OPEN' || value.status === '' ? null : value.status,
    };
  }
}
