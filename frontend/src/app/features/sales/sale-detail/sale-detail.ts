import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { filter, switchMap } from 'rxjs';

import { PAYMENT_METHOD_LABELS, PAYMENT_STATUS_LABELS, PaymentMethod, PaymentRequest } from '../../../core/models/payment';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { askPayment } from '../../../shared/components/payment-dialog/payment-dialog';
import { askReason } from '../../../shared/components/reason-dialog/reason-dialog';
import { CustomerCreditService } from '../../customer-credits/customer-credit.service';
import { SALE_STATUS_LABELS, Sale } from '../sale.models';
import { SaleService } from '../sale.service';

@Component({
  selector: 'app-sale-detail',
  imports: [CurrencyPipe, DatePipe, RouterLink, MatButtonModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './sale-detail.html',
  styleUrl: './sale-detail.scss',
})
export class SaleDetail implements OnInit {
  private readonly saleService = inject(SaleService);
  private readonly notifications = inject(NotificationService);
  private readonly dialog = inject(MatDialog);
  private readonly auth = inject(AuthService);
  private readonly creditService = inject(CustomerCreditService);

  readonly id = input.required<string>();

  protected readonly sale = signal<Sale | null>(null);
  protected readonly paymentStatusLabels = PAYMENT_STATUS_LABELS;
  protected readonly saleStatusLabels = SALE_STATUS_LABELS;
  /** Reglement d'une vente a credit (paiement ulterieur). */
  protected readonly canReceivePayment = computed(() => {
    const sale = this.sale();
    return !!sale && sale.status === 'COMPLETED' && sale.amountDue > 0;
  });
  protected readonly canCancel = computed(
    () => this.auth.hasAnyRole(['MANAGER']) && this.sale()?.status === 'COMPLETED',
  );

  ngOnInit(): void {
    this.saleService.get(Number(this.id())).subscribe({
      next: (sale) => this.sale.set(sale),
      error: (error: unknown) => this.notifications.error(error),
    });
  }

  protected methodLabel(method: PaymentMethod): string {
    return PAYMENT_METHOD_LABELS[method];
  }

  protected receivePayment(): void {
    const sale = this.sale();
    if (!sale) {
      return;
    }
    askPayment(this.dialog, { title: `Encaisser - ${sale.reference}`, amountDue: sale.amountDue, creditDate: sale.createdAt })
      .pipe(
        filter((payment): payment is PaymentRequest => !!payment),
        switchMap((payment) => this.creditService.pay({ source: 'SALE', id: sale.id }, payment)),
        switchMap(() => this.saleService.get(sale.id)),
      )
      .subscribe({
        next: (updated) => {
          this.sale.set(updated);
          this.notifications.success('Paiement enregistré');
        },
        error: (error: unknown) => this.notifications.error(error),
      });
  }

  protected cancel(): void {
    const sale = this.sale();
    if (!sale) {
      return;
    }
    askReason(this.dialog, {
      title: `Annuler la vente ${sale.reference}`,
      message: 'Le stock et les IMEI seront restitués. Les remboursements éventuels sont à traiter à part.',
      confirmLabel: 'Annuler la vente',
    })
      .pipe(
        filter((reason): reason is string => !!reason),
        switchMap((reason) => this.saleService.cancel(sale.id, reason)),
      )
      .subscribe({
        next: (cancelled) => {
          this.sale.set(cancelled);
          this.notifications.success('Vente annulée');
        },
        error: (error: unknown) => this.notifications.error(error),
      });
  }
}
