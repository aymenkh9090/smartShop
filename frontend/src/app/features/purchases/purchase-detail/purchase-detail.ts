import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

import { PAYMENT_METHOD_LABELS, PAYMENT_STATUS_LABELS, PaymentMethod } from '../../../core/models/payment';
import { NotificationService } from '../../../core/services/notification.service';
import { Purchase } from '../purchase.models';
import { PurchaseService } from '../purchase.service';

@Component({
  selector: 'app-purchase-detail',
  imports: [CurrencyPipe, DatePipe, RouterLink, MatButtonModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './purchase-detail.html',
  styleUrl: '../../sales/sale-detail/sale-detail.scss',
})
export class PurchaseDetail implements OnInit {
  private readonly purchaseService = inject(PurchaseService);
  private readonly notifications = inject(NotificationService);

  readonly id = input.required<string>();

  protected readonly purchase = signal<Purchase | null>(null);
  protected readonly paymentStatusLabels = PAYMENT_STATUS_LABELS;

  ngOnInit(): void {
    this.purchaseService.get(Number(this.id())).subscribe({
      next: (purchase) => this.purchase.set(purchase),
      error: (error: unknown) => this.notifications.error(error),
    });
  }

  protected methodLabel(method: PaymentMethod): string {
    return PAYMENT_METHOD_LABELS[method];
  }
}
