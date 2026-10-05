import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, input, signal, viewChild } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { EMPTY, Observable, filter, switchMap } from 'rxjs';

import { PAYMENT_METHOD_LABELS, PAYMENT_STATUS_LABELS, PaymentMethod, PaymentRequest } from '../../../core/models/payment';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { confirmAction } from '../../../shared/components/confirm-dialog/confirm-dialog';
import { askPayment } from '../../../shared/components/payment-dialog/payment-dialog';
import { askReason } from '../../../shared/components/reason-dialog/reason-dialog';
import { fromIsoDate, toIsoDate } from '../../../shared/components/reschedule-dialog/reschedule-dialog';
import { applyServerErrors, fieldErrorMessage } from '../../../shared/utils/form-errors';
import { ProductPicker } from '../../products/product-picker/product-picker';
import { Product } from '../../products/product.models';
import { ProductUnit } from '../../stock/stock.models';
import { StockService } from '../../stock/stock.service';
import {
  AFTER_SALES_STATUS_LABELS,
  AfterSalesStatus,
  AfterSalesTicket,
  Technician,
  WORKSHOP_STATUSES,
  isClosed,
} from '../after-sales.models';
import { AfterSalesService } from '../after-sales.service';

/**
 * Detail d'un ticket SAV. Les actions affichees dependent du role et du statut (confort UX :
 * le backend controle les droits et le workflow).
 */
@Component({
  selector: 'app-ticket-detail',
  imports: [
    CurrencyPipe,
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCheckboxModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTooltipModule,
    ProductPicker,
  ],
  providers: [provideNativeDateAdapter()],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './ticket-detail.html',
  styleUrls: ['../../sales/sale-detail/sale-detail.scss', './ticket-detail.scss'],
})
export class TicketDetail implements OnInit {
  private readonly afterSalesService = inject(AfterSalesService);
  private readonly stockService = inject(StockService);
  private readonly notifications = inject(NotificationService);
  private readonly dialog = inject(MatDialog);
  private readonly auth = inject(AuthService);

  readonly id = input.required<string>();
  private readonly partPicker = viewChild(ProductPicker);

  protected readonly ticket = signal<AfterSalesTicket | null>(null);
  protected readonly technicians = signal<Technician[]>([]);
  protected readonly busy = signal(false);
  protected readonly statusLabels = AFTER_SALES_STATUS_LABELS;
  protected readonly paymentStatusLabels = PAYMENT_STATUS_LABELS;
  protected readonly errorOf = fieldErrorMessage;

  protected readonly isSeller = this.auth.hasAnyRole(['SELLER']);
  private readonly isTechnician = this.auth.hasAnyRole(['TECHNICIAN']);
  protected readonly isManager = this.auth.hasAnyRole(['MANAGER']);

  protected readonly open = computed(() => {
    const ticket = this.ticket();
    return !!ticket && !isClosed(ticket.status);
  });
  protected readonly canWork = computed(() => this.open() && this.isTechnician);
  protected readonly inWorkshop = computed(() => {
    const ticket = this.ticket();
    return !!ticket && this.isTechnician && WORKSHOP_STATUSES.includes(ticket.status);
  });
  protected readonly canPay = computed(() => this.open() && this.isSeller && (this.ticket()?.amountDue ?? 0) > 0);
  protected readonly canDeliver = computed(() => this.isSeller && this.ticket()?.status === 'READY');
  protected readonly canCancel = computed(() => this.open() && this.isManager);

  protected readonly workForm = new FormGroup({
    diagnosis: new FormControl('', { nonNullable: true, validators: Validators.maxLength(2000) }),
    repairDescription: new FormControl('', { nonNullable: true, validators: Validators.maxLength(2000) }),
    laborCost: new FormControl<number | null>(0, [Validators.required, Validators.min(0)]),
    estimatedCost: new FormControl<number | null>(null, Validators.min(0)),
    expectedDate: new FormControl<Date | null>(null),
    underWarranty: new FormControl(false, { nonNullable: true }),
    notes: new FormControl('', { nonNullable: true, validators: Validators.maxLength(1000) }),
  });
  protected readonly statusComment = new FormControl('', { nonNullable: true, validators: Validators.maxLength(500) });
  protected readonly technicianControl = new FormControl<number | null>(null);

  /** Piece en cours d'ajout. */
  protected readonly partProduct = signal<Product | null>(null);
  protected readonly partUnits = signal<ProductUnit[]>([]);
  protected readonly partForm = new FormGroup({
    quantity: new FormControl(1, { nonNullable: true, validators: [Validators.required, Validators.min(1), Validators.max(100)] }),
    unitIds: new FormControl<number[]>([], { nonNullable: true }),
  });

  ngOnInit(): void {
    this.afterSalesService.get(Number(this.id())).subscribe({
      next: (ticket) => this.setTicket(ticket),
      error: (error: unknown) => this.notifications.error(error),
    });
    if (this.isManager) {
      this.afterSalesService.technicians().subscribe({
        next: (technicians) => this.technicians.set(technicians),
        error: (error: unknown) => this.notifications.error(error),
      });
    }
  }

  protected methodLabel(method: PaymentMethod): string {
    return PAYMENT_METHOD_LABELS[method];
  }

  protected statusLabel(status: AfterSalesStatus | null): string {
    return status ? AFTER_SALES_STATUS_LABELS[status] : '-';
  }

  protected unitLabel(unit: { imei: string | null; serialNumber: string | null }): string {
    return unit.imei ?? `SN ${unit.serialNumber}`;
  }

  // ------------------------------------------------------------------ travail technique

  protected changeStatus(status: AfterSalesStatus): void {
    const comment = this.statusComment.value.trim() || null;
    this.run((ticket) => this.afterSalesService.changeStatus(ticket.id, status, comment), `Statut : ${this.statusLabel(status)}`, () =>
      this.statusComment.reset(),
    );
  }

  protected saveWork(): void {
    if (this.workForm.invalid) {
      this.workForm.markAllAsTouched();
      return;
    }
    const value = this.workForm.getRawValue();
    this.run(
      (ticket) =>
        this.afterSalesService.updateWork(ticket.id, {
          diagnosis: value.diagnosis.trim() || null,
          repairDescription: value.repairDescription.trim() || null,
          laborCost: value.laborCost ?? 0,
          estimatedCost: value.estimatedCost,
          expectedDate: value.expectedDate ? toIsoDate(value.expectedDate) : null,
          underWarranty: value.underWarranty,
          notes: value.notes.trim() || null,
        }),
      'Ticket mis à jour',
      undefined,
      this.workForm,
    );
  }

  protected assignTechnician(): void {
    const technicianId = this.technicianControl.value;
    if (technicianId) {
      this.run((ticket) => this.afterSalesService.assignTechnician(ticket.id, technicianId), 'Technicien assigné');
    }
  }

  protected selectPart(product: Product): void {
    this.partProduct.set(product);
    this.partForm.reset({ quantity: 1, unitIds: [] });
    this.partUnits.set([]);
    if (product.serialTracked) {
      this.stockService
        .searchUnits({ productId: product.id, status: 'IN_STOCK' }, { page: 0, size: 100, sort: 'createdAt,asc' })
        .subscribe({
          next: (page) => this.partUnits.set(page.content),
          error: (error: unknown) => this.notifications.error(error),
        });
    }
  }

  protected addPart(): void {
    const product = this.partProduct();
    if (!product || this.partForm.invalid) {
      return;
    }
    const value = this.partForm.getRawValue();
    if (product.serialTracked && value.unitIds.length === 0) {
      this.notifications.error(null, 'Choisissez les unités (IMEI / numéro de série) utilisées.');
      return;
    }
    this.run(
      (ticket) =>
        this.afterSalesService.addPart(ticket.id, {
          productId: product.id,
          quantity: product.serialTracked ? value.unitIds.length : value.quantity,
          unitIds: product.serialTracked ? value.unitIds : null,
        }),
      'Pièce ajoutée (sortie de stock)',
      () => {
        this.partProduct.set(null);
        this.partPicker()?.clear();
      },
    );
  }

  protected removePart(partId: number, label: string): void {
    confirmAction(this.dialog, {
      title: 'Retirer la pièce',
      message: `"${label}" sera remise en stock.`,
      confirmLabel: 'Retirer',
    })
      .pipe(filter(Boolean))
      .subscribe(() =>
        this.run((ticket) => this.afterSalesService.removePart(ticket.id, partId), 'Pièce retirée et remise en stock'),
      );
  }

  // ------------------------------------------------------------------ paiement, livraison, annulation

  protected pay(): void {
    const ticket = this.ticket();
    if (!ticket) {
      return;
    }
    askPayment(this.dialog, { title: `Encaisser - ${ticket.ticketNumber}`, amountDue: ticket.amountDue })
      .pipe(filter((payment): payment is PaymentRequest => !!payment))
      .subscribe((payment) => this.run((t) => this.afterSalesService.pay(t.id, payment), 'Paiement enregistré'));
  }

  /** Livraison : encaisse d'abord le reste du (le ticket doit etre solde), sinon simple confirmation. */
  protected deliver(): void {
    const ticket = this.ticket();
    if (!ticket) {
      return;
    }
    const payments$: Observable<PaymentRequest[]> =
      ticket.amountDue > 0
        ? askPayment(this.dialog, { title: `Règlement - ${ticket.ticketNumber}`, amountDue: ticket.amountDue }).pipe(
            switchMap((payment) => (payment ? [[payment]] : EMPTY)),
          )
        : confirmAction(this.dialog, {
            title: 'Livrer l\'appareil',
            message: `Confirmer la remise de l'appareil au client (${ticket.ticketNumber}) ?`,
            confirmLabel: 'Livrer',
          }).pipe(switchMap((confirmed) => (confirmed ? [[]] : EMPTY)));
    payments$.subscribe((payments) =>
      this.run((t) => this.afterSalesService.deliver(t.id, { payments, comment: null }), 'Appareil livre'),
    );
  }

  protected cancel(): void {
    const ticket = this.ticket();
    if (!ticket) {
      return;
    }
    askReason(this.dialog, {
      title: `Annuler le ticket ${ticket.ticketNumber}`,
      message: 'Les pièces utilisées seront remises en stock. Les paiements restent dans l\'historique.',
      confirmLabel: 'Annuler le ticket',
    })
      .pipe(filter((reason): reason is string => !!reason))
      .subscribe((reason) => this.run((t) => this.afterSalesService.cancel(t.id, reason), 'Ticket annulé'));
  }

  // ------------------------------------------------------------------ outils

  private run(
    action: (ticket: AfterSalesTicket) => Observable<AfterSalesTicket>,
    successMessage: string,
    onSuccess?: () => void,
    form?: FormGroup,
  ): void {
    const ticket = this.ticket();
    if (!ticket || this.busy()) {
      return;
    }
    this.busy.set(true);
    action(ticket).subscribe({
      next: (updated) => {
        this.busy.set(false);
        this.setTicket(updated);
        this.notifications.success(successMessage);
        onSuccess?.();
      },
      error: (error: unknown) => {
        this.busy.set(false);
        if (form) {
          applyServerErrors(form, error);
        }
        this.notifications.error(error);
      },
    });
  }

  private setTicket(ticket: AfterSalesTicket): void {
    this.ticket.set(ticket);
    this.workForm.reset({
      diagnosis: ticket.diagnosis ?? '',
      repairDescription: ticket.repairDescription ?? '',
      laborCost: ticket.laborCost,
      estimatedCost: ticket.estimatedCost,
      expectedDate: ticket.expectedDate ? fromIsoDate(ticket.expectedDate) : null,
      underWarranty: ticket.underWarranty,
      notes: ticket.notes ?? '',
    });
    if (ticket.warrantyUntil) {
      this.workForm.controls.underWarranty.enable();
    } else {
      this.workForm.controls.underWarranty.disable();
    }
    this.technicianControl.setValue(ticket.technician?.id ?? null);
  }
}
