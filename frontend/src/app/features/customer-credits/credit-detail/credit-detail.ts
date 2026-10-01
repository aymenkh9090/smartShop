import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { filter, switchMap } from 'rxjs';

import { CREDIT_STATUS_LABELS, PAYMENT_METHOD_LABELS, PaymentMethod, PaymentRequest } from '../../../core/models/payment';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { askPayment } from '../../../shared/components/payment-dialog/payment-dialog';
import { RescheduleResult, askNewDueDate } from '../../../shared/components/reschedule-dialog/reschedule-dialog';
import { CustomerCredit } from '../customer-credit.models';
import { CustomerCreditService } from '../customer-credit.service';

@Component({
  selector: 'app-credit-detail',
  imports: [CurrencyPipe, DatePipe, RouterLink, MatButtonModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './credit-detail.html',
  styleUrl: '../../sales/sale-detail/sale-detail.scss',
})
export class CreditDetail implements OnInit {
  private readonly creditService = inject(CustomerCreditService);
  private readonly notifications = inject(NotificationService);
  private readonly dialog = inject(MatDialog);
  private readonly auth = inject(AuthService);

  readonly id = input.required<string>();

  protected readonly credit = signal<CustomerCredit | null>(null);
  protected readonly statusLabels = CREDIT_STATUS_LABELS;
  protected readonly isOpen = computed(() => {
    const status = this.credit()?.status;
    return !!status && status !== 'PAID' && status !== 'CANCELLED';
  });
  protected readonly canReschedule = computed(() => this.isOpen() && this.auth.hasAnyRole(['MANAGER']));

  ngOnInit(): void {
    this.creditService.get(Number(this.id())).subscribe({
      next: (credit) => this.credit.set(credit),
      error: (error: unknown) => this.notifications.error(error),
    });
  }

  protected methodLabel(method: PaymentMethod): string {
    return PAYMENT_METHOD_LABELS[method];
  }

  protected pay(): void {
    const credit = this.credit();
    if (!credit) {
      return;
    }
    askPayment(this.dialog, { title: `Encaisser - ${credit.reference}`, amountDue: credit.amountDue })
      .pipe(
        filter((payment): payment is PaymentRequest => !!payment),
        switchMap((payment) => this.creditService.pay(credit.saleId, payment)),
      )
      .subscribe({
        next: (updated) => {
          this.credit.set(updated);
          this.notifications.success(updated.status === 'PAID' ? 'Credit solde' : 'Paiement enregistre');
        },
        error: (error: unknown) => this.notifications.error(error),
      });
  }

  protected reschedule(): void {
    const credit = this.credit();
    if (!credit) {
      return;
    }
    askNewDueDate(this.dialog, { title: `Echeance - ${credit.reference}`, dueDate: credit.dueDate })
      .pipe(
        filter((result): result is RescheduleResult => !!result),
        switchMap((result) => this.creditService.reschedule(credit.saleId, result.dueDate)),
      )
      .subscribe({
        next: (updated) => {
          this.credit.set(updated);
          this.notifications.success('Echeance mise a jour');
        },
        error: (error: unknown) => this.notifications.error(error),
      });
  }
}
