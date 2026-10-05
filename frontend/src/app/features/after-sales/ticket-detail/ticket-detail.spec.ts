import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { Role } from '../../../core/models/auth.models';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ProductService } from '../../products/product.service';
import { StockService } from '../../stock/stock.service';
import { AfterSalesTicket } from '../after-sales.models';
import { AfterSalesService } from '../after-sales.service';
import { TicketDetail } from './ticket-detail';

const TICKET = {
  id: 3,
  ticketNumber: 'SAV-2026-000003',
  customer: { id: 1, firstName: 'Ali', lastName: 'Ben Salah', phone: '22333444' },
  deviceDescription: 'Samsung A55 noir',
  product: null,
  unitId: null,
  saleId: null,
  saleReference: null,
  imei: '490154203237518',
  serialNumber: null,
  reportedIssue: 'Écran fissure',
  diagnosis: 'Dalle à remplacer',
  repairDescription: null,
  technician: null,
  status: 'IN_REPAIR',
  nextStatuses: ['WAITING_FOR_PART', 'READY'],
  warrantyUntil: null,
  underWarranty: false,
  estimatedCost: 150,
  laborCost: 30,
  partsTotal: 120,
  totalCost: 150,
  amountPaid: 50,
  amountDue: 100,
  paymentStatus: 'PARTIALLY_PAID',
  receivedAt: '2026-10-01T10:00:00Z',
  expectedDate: null,
  deliveredAt: null,
  notes: null,
  createdBy: 'seller',
  statusHistory: [
    { id: 1, fromStatus: null, toStatus: 'RECEIVED', comment: null, changedBy: 'seller', changedAt: '2026-10-01T10:00:00Z' },
    { id: 2, fromStatus: 'RECEIVED', toStatus: 'DIAGNOSING', comment: 'Pris en charge', changedBy: 'tech', changedAt: '2026-10-01T11:00:00Z' },
  ],
  parts: [
    {
      id: 7, productId: 4, productSku: 'ECR-A55', productName: 'Écran A55', quantity: 1, unitPrice: 120,
      lineTotal: 120, units: [], addedBy: 'tech', addedAt: '2026-10-01T11:00:00Z',
    },
  ],
  payments: [],
} as AfterSalesTicket;

describe('TicketDetail', () => {
  function render(roles: Role[], ticket: AfterSalesTicket = TICKET): string {
    TestBed.configureTestingModule({
      imports: [TicketDetail],
      providers: [
        provideRouter([]),
        { provide: AfterSalesService, useValue: { get: () => of(ticket), technicians: () => of([]) } },
        { provide: StockService, useValue: {} },
        { provide: ProductService, useValue: {} },
        { provide: NotificationService, useValue: jasmine.createSpyObj('NotificationService', ['success', 'error']) },
        {
          provide: AuthService,
          useValue: { hasAnyRole: (r: Role[]) => r.some((x) => roles.includes(x)), currentUser: () => null },
        },
      ],
    });
    const fixture: ComponentFixture<TicketDetail> = TestBed.createComponent(TicketDetail);
    fixture.componentRef.setInput('id', '3');
    fixture.detectChanges();
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  it('shows parts, amounts and the status history', () => {
    const text = render(['SELLER']);
    expect(text).toContain('Écran A55');
    expect(text).toContain('Pris en charge');
    expect(text).toContain('Dalle à remplacer');
  });

  it('lets a technician move the workflow and manage parts, but not collect payments', () => {
    const text = render(['TECHNICIAN']);
    expect(text).toContain('Avancement');
    expect(text).toContain('Prêt');
    expect(text).toContain('Ajouter une pièce');
    expect(text).not.toContain('Acompte');
    expect(text).not.toContain('Annuler');
  });

  it('lets a seller collect a payment, but not change the workflow', () => {
    const text = render(['SELLER']);
    expect(text).toContain('Acompte');
    expect(text).not.toContain('Avancement');
    expect(text).not.toContain('Livrer');
  });

  it('offers delivery to a seller once the ticket is ready', () => {
    expect(render(['SELLER'], { ...TICKET, status: 'READY', nextStatuses: ['IN_REPAIR'] })).toContain('Livrer');
  });

  it('offers no action on a delivered ticket', () => {
    const text = render(['MANAGER'], { ...TICKET, status: 'DELIVERED', nextStatuses: [], amountDue: 0 });
    expect(text).not.toContain('Avancement');
    expect(text).not.toContain('Acompte');
    expect(text).not.toContain('Annuler');
    expect(text).not.toContain('Ajouter une pièce');
  });
});
