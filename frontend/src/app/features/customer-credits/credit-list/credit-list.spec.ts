import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { NotificationService } from '../../../core/services/notification.service';
import { CustomerCreditSummary } from '../customer-credit.models';
import { CustomerCreditService } from '../customer-credit.service';
import { CreditList } from './credit-list';

const CREDIT = {
  id: 5,
  source: 'SALE',
  reference: 'V-2026-000005',
  description: null,
  customer: { id: 1, firstName: 'Ali', lastName: 'Ben Salah', phone: '22333444' },
  createdAt: '2026-10-01T10:00:00Z',
  createdBy: 'seller',
  total: 800,
  amountPaid: 300,
  amountDue: 500,
  dueDate: '2026-09-28',
  status: 'OVERDUE',
} as CustomerCreditSummary;

describe('CreditList', () => {
  let fixture: ComponentFixture<CreditList>;
  let credits: jasmine.SpyObj<CustomerCreditService>;
  let dialog: jasmine.SpyObj<MatDialog>;

  beforeEach(() => {
    credits = jasmine.createSpyObj<CustomerCreditService>('CustomerCreditService', ['search', 'pay']);
    credits.search.and.returnValue(of({ content: [CREDIT], page: 0, size: 20, totalElements: 1, totalPages: 1 }));
    credits.pay.and.returnValue(of({ ...CREDIT, amountDue: 0, status: 'PAID', cancelReason: null, payments: [] }));
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    TestBed.configureTestingModule({
      imports: [CreditList],
      providers: [
        provideRouter([]),
        { provide: CustomerCreditService, useValue: credits },
        { provide: MatDialog, useValue: dialog },
        { provide: NotificationService, useValue: jasmine.createSpyObj('NotificationService', ['success', 'error']) },
      ],
    });
  });

  /** A appeler dans la zone de test (fakeAsync si besoin) : le debounce des filtres en depend. */
  function create(): void {
    fixture = TestBed.createComponent(CreditList);
    fixture.detectChanges();
  }

  it('shows open credits by default, with the overdue status', () => {
    create();
    expect(credits.search).toHaveBeenCalledWith(
      { q: null, customerId: null, open: true, status: null },
      jasmine.objectContaining({ page: 0 }),
    );
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('En retard');
  });

  it('maps the status choice to the API filter', fakeAsync(() => {
    create();
    const filters = (fixture.componentInstance as unknown as { filters: CreditList['filters'] }).filters;

    filters.controls.status.setValue('PAID');
    tick(300);
    fixture.detectChanges();
    expect(credits.search.calls.mostRecent().args[0]).toEqual(
      jasmine.objectContaining({ open: null, status: 'PAID' }),
    );

    filters.controls.status.setValue('');
    tick(300);
    fixture.detectChanges();
    expect(credits.search.calls.mostRecent().args[0]).toEqual(jasmine.objectContaining({ open: null, status: null }));
  }));

  it('records a payment from the dialog and reloads the list', () => {
    create();
    dialog.open.and.returnValue({ afterClosed: () => of({ amount: 500, method: 'CASH', reference: null }) } as never);

    (fixture.componentInstance as unknown as { pay(credit: CustomerCreditSummary): void }).pay(CREDIT);

    fixture.detectChanges();

    expect(credits.pay).toHaveBeenCalledWith(CREDIT, { amount: 500, method: 'CASH', reference: null });
    expect(credits.search).toHaveBeenCalledTimes(2);
  });

  it('does nothing when the payment dialog is dismissed', () => {
    create();
    dialog.open.and.returnValue({ afterClosed: () => of(undefined) } as never);

    (fixture.componentInstance as unknown as { pay(credit: CustomerCreditSummary): void }).pay(CREDIT);

    expect(credits.pay).not.toHaveBeenCalled();
  });
});
