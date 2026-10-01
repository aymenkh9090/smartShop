import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { Role } from '../../../core/models/auth.models';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Sale } from '../sale.models';
import { CustomerCreditService } from '../../customer-credits/customer-credit.service';
import { SaleService } from '../sale.service';
import { SaleDetail } from './sale-detail';

const SALE = {
  id: 9,
  reference: 'V-2026-000009',
  customer: null,
  status: 'COMPLETED',
  items: [
    {
      id: 1,
      productId: 4,
      productSku: 'SM-A55',
      productName: 'Galaxy A55',
      quantity: 1,
      unitPrice: 1299.5,
      lineTotal: 1299.5,
      warrantyMonths: 12,
      units: [{ id: 7, imei: '490154203237518', serialNumber: null }],
    },
  ],
  subtotal: 1299.5,
  discount: 0,
  total: 1299.5,
  amountPaid: 1299.5,
  amountDue: 0,
  paymentStatus: 'PAID',
  dueDate: null,
  payments: [],
  notes: null,
  createdBy: 'seller',
  createdAt: '2026-10-01T10:00:00Z',
  cancelledBy: null,
  cancelledAt: null,
  cancelReason: null,
} as Sale;

describe('SaleDetail', () => {
  function render(roles: Role[], sale: Sale = SALE): ComponentFixture<SaleDetail> {
    TestBed.configureTestingModule({
      imports: [SaleDetail],
      providers: [
        provideRouter([]),
        { provide: SaleService, useValue: { get: () => of(sale) } },
        { provide: CustomerCreditService, useValue: jasmine.createSpyObj('CustomerCreditService', ['pay']) },
        { provide: NotificationService, useValue: jasmine.createSpyObj('NotificationService', ['success', 'error']) },
        { provide: AuthService, useValue: { hasAnyRole: (r: Role[]) => r.some((x) => roles.includes(x)) } },
      ],
    });
    const fixture = TestBed.createComponent(SaleDetail);
    fixture.componentRef.setInput('id', '9');
    fixture.detectChanges();
    return fixture;
  }

  it('shows the IMEI and the warranty of each sold item', () => {
    const text = (render(['SELLER']).nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('490154203237518');
    expect(text).toContain('12 mois');
  });

  it('offers cancellation to managers only, and never on a cancelled sale', () => {
    expect((render(['SELLER']).nativeElement as HTMLElement).textContent).not.toContain('Annuler la vente');

    TestBed.resetTestingModule();
    expect((render(['MANAGER']).nativeElement as HTMLElement).textContent).toContain('Annuler la vente');

    TestBed.resetTestingModule();
    const cancelled = render(['MANAGER'], { ...SALE, status: 'CANCELLED', cancelReason: 'Erreur' });
    const buttons = (cancelled.nativeElement as HTMLElement).querySelectorAll('button');
    expect(Array.from(buttons).some((b) => b.textContent?.includes('Annuler la vente'))).toBeFalse();
  });

  it('offers to collect a payment only on a completed sale with a balance', () => {
    const text = (fixture: ComponentFixture<SaleDetail>) => (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text(render(['SELLER']))).not.toContain('Encaisser');

    TestBed.resetTestingModule();
    expect(text(render(['SELLER'], { ...SALE, amountDue: 500, paymentStatus: 'PARTIALLY_PAID' }))).toContain('Encaisser');

    TestBed.resetTestingModule();
    expect(text(render(['SELLER'], { ...SALE, amountDue: 500, status: 'CANCELLED' }))).not.toContain('Encaisser');
  });
});
