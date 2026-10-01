import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { Role } from '../../../core/models/auth.models';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { CustomerCredit } from '../customer-credit.models';
import { CustomerCreditService } from '../customer-credit.service';
import { CreditDetail } from './credit-detail';

const CREDIT = {
  saleId: 5,
  reference: 'V-2026-000005',
  customer: { id: 1, firstName: 'Ali', lastName: 'Ben Salah', phone: '22333444' },
  createdAt: '2026-10-01T10:00:00Z',
  createdBy: 'seller',
  total: 800,
  amountPaid: 300,
  amountDue: 500,
  dueDate: '2026-11-30',
  status: 'PARTIALLY_PAID',
  payments: [
    { id: 1, amount: 300, method: 'CASH', reference: null, paidAt: '2026-10-01T10:00:00Z', createdBy: 'seller' },
  ],
} as CustomerCredit;

describe('CreditDetail', () => {
  function render(roles: Role[], credit: CustomerCredit = CREDIT): string {
    TestBed.configureTestingModule({
      imports: [CreditDetail],
      providers: [
        provideRouter([]),
        { provide: CustomerCreditService, useValue: { get: () => of(credit) } },
        { provide: NotificationService, useValue: jasmine.createSpyObj('NotificationService', ['success', 'error']) },
        { provide: AuthService, useValue: { hasAnyRole: (r: Role[]) => r.some((x) => roles.includes(x)) } },
      ],
    });
    const fixture: ComponentFixture<CreditDetail> = TestBed.createComponent(CreditDetail);
    fixture.componentRef.setInput('id', '5');
    fixture.detectChanges();
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  it('shows the payment history and lets a seller collect, but not reschedule', () => {
    const text = render(['SELLER']);
    expect(text).toContain('Historique des paiements');
    expect(text).toContain('Especes');
    expect(text).toContain('Encaisser');
    expect(text).not.toContain("Reporter l'echeance");
  });

  it('lets a manager reschedule an open credit', () => {
    expect(render(['MANAGER'])).toContain("Reporter l'echeance");
  });

  it('offers no action on a settled credit', () => {
    const text = render(['MANAGER'], { ...CREDIT, amountDue: 0, status: 'PAID' });
    expect(text).not.toContain('Encaisser');
    expect(text).not.toContain("Reporter l'echeance");
  });
});
