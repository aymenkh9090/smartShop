import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { Observable, filter, switchMap } from 'rxjs';

import { CREDIT_STATUS_LABELS, PAYMENT_METHOD_LABELS, PaymentMethod, PaymentRequest } from '../../../core/models/payment';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { askPayment } from '../../../shared/components/payment-dialog/payment-dialog';
import { askReason } from '../../../shared/components/reason-dialog/reason-dialog';
import { RescheduleResult, askNewDueDate } from '../../../shared/components/reschedule-dialog/reschedule-dialog';
import { SupplierCredit, SupplierCreditSource } from '../supplier-credit.models';
import { SupplierCreditService } from '../supplier-credit.service';

@Component({
  selector: 'app-credit-detail',
  imports: [CurrencyPipe, DatePipe, RouterLink, MatButtonModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './credit-detail.html',
  styleUrl: '../../sales/sale-detail/sale-detail.scss',
})
export class CreditDetail implements OnInit {
  private readonly creditService = inject(SupplierCreditService);
  private readonly notifications = inject(NotificationService);
  private readonly dialog = inject(MatDialog);
  private readonly auth = inject(AuthService);

  readonly id = input.required<string>();
  /** Origine du credit, fournie par la route (data). */
  readonly source = input<SupplierCreditSource>('PURCHASE');

  protected readonly credit = signal<SupplierCredit | null>(null);
  protected readonly statusLabels = CREDIT_STATUS_LABELS;
  protected readonly isOpen = computed(() => {
    const status = this.credit()?.status;
    return !!status && status !== 'PAID' && status !== 'CANCELLED';
  });
  protected readonly isManager = computed(() => this.auth.hasAnyRole(['MANAGER']));
  protected readonly canReschedule = computed(() => this.isOpen() && this.isManager());
  /** Un credit saisi par erreur peut etre annule tant qu'aucun paiement n'est enregistre. */
  protected readonly canCancel = computed(() => {
    const credit = this.credit();
    return !!credit && credit.source === 'MANUAL' && this.isOpen() && credit.amountPaid === 0 && this.isManager();
  });

  ngOnInit(): void {
    this.creditService.get({ source: this.source(), id: Number(this.id()) }).subscribe({
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
    this.run(
      askPayment(this.dialog, {
        title: `Payer le fournisseur - ${credit.reference}`,
        amountDue: credit.amountDue,
        creditDate: credit.createdAt,
      }).pipe(
        filter((payment): payment is PaymentRequest => !!payment),
        switchMap((payment) => this.creditService.pay(credit, payment)),
      ),
      (updated) => (updated.status === 'PAID' ? 'Crédit soldé' : 'Paiement enregistré'),
    );
  }

  protected reschedule(): void {
    const credit = this.credit();
    if (!credit) {
      return;
    }
    this.run(
      askNewDueDate(this.dialog, { title: `Échéance - ${credit.reference}`, dueDate: credit.dueDate }).pipe(
        filter((result): result is RescheduleResult => !!result),
        switchMap((result) => this.creditService.reschedule(credit, result.dueDate)),
      ),
      () => 'Échéance mise à jour',
    );
  }

  protected cancel(): void {
    const credit = this.credit();
    if (!credit) {
      return;
    }
    this.run(
      askReason(this.dialog, {
        title: `Annuler le crédit ${credit.reference}`,
        message: 'Le crédit restera dans l\'historique avec le statut "Annulé".',
        confirmLabel: 'Annuler le crédit',
      }).pipe(
        filter((reason): reason is string => !!reason),
        switchMap((reason) => this.creditService.cancel(credit.id, reason)),
      ),
      () => 'Crédit annulé',
    );
  }

  private run(action: Observable<SupplierCredit>, message: (updated: SupplierCredit) => string): void {
    action.subscribe({
      next: (updated) => {
        this.credit.set(updated);
        this.notifications.success(message(updated));
      },
      error: (error: unknown) => this.notifications.error(error),
    });
  }
}
