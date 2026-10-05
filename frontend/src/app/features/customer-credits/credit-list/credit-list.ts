import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, input, numberAttribute, signal } from '@angular/core';
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
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { catchError, debounceTime, filter, finalize, of, switchMap, tap } from 'rxjs';

import { PageRequest, emptyPage } from '../../../core/models/page';
import { CREDIT_STATUSES, CREDIT_STATUS_LABELS, CreditStatus, PaymentRequest } from '../../../core/models/payment';
import { NotificationService } from '../../../core/services/notification.service';
import { askPayment } from '../../../shared/components/payment-dialog/payment-dialog';
import { CustomerCreditFilter, CustomerCreditSummary, customerCreditLink } from '../customer-credit.models';
import { CustomerCreditService } from '../customer-credit.service';

/** Valeur du filtre statut : 'OPEN' = credits non soldes, '' = tous. */
type StatusChoice = 'OPEN' | '' | CreditStatus;

interface CreditQuery extends PageRequest {
  filter: CustomerCreditFilter;
}

@Component({
  selector: 'app-credit-list',
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
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatTooltipModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './credit-list.html',
})
export class CreditList implements OnInit {
  private readonly creditService = inject(CustomerCreditService);
  private readonly notifications = inject(NotificationService);
  private readonly dialog = inject(MatDialog);

  /** Credits d'un client (?customerId=), ex. depuis la liste des soldes. */
  readonly customerId = input(undefined, { transform: numberAttribute });

  protected readonly statuses = CREDIT_STATUSES;
  protected readonly statusLabels = CREDIT_STATUS_LABELS;
  protected readonly columns = ['createdAt', 'reference', 'party', 'total', 'amountDue', 'dueDate', 'status', 'actions'];

  protected readonly filters = new FormGroup({
    q: new FormControl('', { nonNullable: true }),
    status: new FormControl<StatusChoice>('OPEN', { nonNullable: true }),
  });

  protected readonly query = signal<CreditQuery>({ filter: { open: true }, page: 0, size: 20, sort: 'createdAt,desc' });
  protected readonly loading = signal(false);

  protected readonly page = toSignal(
    toObservable(this.query).pipe(
      tap(() => this.loading.set(true)),
      switchMap((query) =>
        this.creditService.search(query.filter, query).pipe(
          catchError((error: unknown) => {
            this.notifications.error(error);
            return of(emptyPage<CustomerCreditSummary>());
          }),
          finalize(() => this.loading.set(false)),
        ),
      ),
    ),
    { initialValue: emptyPage<CustomerCreditSummary>() },
  );

  constructor() {
    this.filters.valueChanges
      .pipe(debounceTime(300), takeUntilDestroyed())
      .subscribe(() => this.query.update((query) => ({ ...query, filter: this.toFilter(), page: 0 })));
  }

  ngOnInit(): void {
    this.query.update((query) => ({ ...query, filter: this.toFilter() }));
  }

  protected link(credit: CustomerCreditSummary): (string | number)[] {
    return customerCreditLink(credit);
  }

  protected statusLabel(status: CreditStatus): string {
    return CREDIT_STATUS_LABELS[status];
  }

  protected isOpen(credit: CustomerCreditSummary): boolean {
    return credit.status !== 'PAID' && credit.status !== 'CANCELLED';
  }

  protected pay(credit: CustomerCreditSummary): void {
    askPayment(this.dialog, {
      title: `Encaisser - ${credit.reference}`,
      amountDue: credit.amountDue,
      creditDate: credit.createdAt,
    })
      .pipe(
        filter((payment): payment is PaymentRequest => !!payment),
        switchMap((payment) => this.creditService.pay(credit, payment)),
      )
      .subscribe({
        next: (updated) => {
          this.notifications.success(
            updated.status === 'PAID' ? `Crédit ${updated.reference} soldé` : 'Paiement enregistré',
          );
          this.query.update((query) => ({ ...query }));
        },
        error: (error: unknown) => this.notifications.error(error),
      });
  }

  protected onPage(event: PageEvent): void {
    this.query.update((query) => ({ ...query, page: event.pageIndex, size: event.pageSize }));
  }

  private toFilter(): CustomerCreditFilter {
    const value = this.filters.getRawValue();
    return {
      q: value.q.trim() || null,
      customerId: this.customerId() ?? null,
      open: value.status === 'OPEN' ? true : null,
      status: value.status === 'OPEN' || value.status === '' ? null : value.status,
    };
  }
}
