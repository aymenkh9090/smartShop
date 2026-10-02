import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { NotificationService } from '../../../core/services/notification.service';
import { CustomerService } from '../../customers/customer.service';
import { Customer } from '../../customers/customer.models';
import { ProductService } from '../../products/product.service';
import { AfterSalesTicket } from '../after-sales.models';
import { AfterSalesService } from '../after-sales.service';
import { TicketForm } from './ticket-form';

describe('TicketForm', () => {
  let fixture: ComponentFixture<TicketForm>;
  let afterSalesService: jasmine.SpyObj<AfterSalesService>;
  let notifications: jasmine.SpyObj<NotificationService>;

  beforeEach(() => {
    afterSalesService = jasmine.createSpyObj('AfterSalesService', ['create']);
    afterSalesService.create.and.returnValue(of({ id: 9, ticketNumber: 'SAV-2026-000009', underWarranty: true } as AfterSalesTicket));
    notifications = jasmine.createSpyObj('NotificationService', ['success', 'error']);
    TestBed.configureTestingModule({
      imports: [TicketForm],
      providers: [
        provideRouter([]),
        { provide: AfterSalesService, useValue: afterSalesService },
        { provide: CustomerService, useValue: {} },
        { provide: ProductService, useValue: {} },
        { provide: NotificationService, useValue: notifications },
      ],
    });
    fixture = TestBed.createComponent(TicketForm);
    fixture.detectChanges();
    spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
  });

  function component(): any {
    return fixture.componentInstance;
  }

  it('requires a customer before sending', () => {
    component().form.patchValue({ deviceDescription: 'iPhone 12', reportedIssue: 'Ne charge plus' });
    component().submit();

    expect(afterSalesService.create).not.toHaveBeenCalled();
    expect(notifications.error).toHaveBeenCalled();
  });

  it('rejects an invalid IMEI', () => {
    component().form.controls.imei.setValue('123456789012345');
    expect(component().form.controls.imei.hasError('imei')).toBeTrue();
  });

  it('sends a trimmed request and opens the created ticket', () => {
    component().selectCustomer({ id: 4, firstName: 'Ali', lastName: 'Ben Salah', phone: '22333444' } as Customer);
    component().form.patchValue({
      deviceDescription: ' iPhone 12 ',
      imei: '490154203237518',
      reportedIssue: ' Ne charge plus ',
      expectedDate: new Date(2026, 9, 5),
    });
    component().submit();

    expect(afterSalesService.create).toHaveBeenCalledWith({
      customerId: 4,
      deviceDescription: 'iPhone 12',
      productId: null,
      imei: '490154203237518',
      serialNumber: null,
      reportedIssue: 'Ne charge plus',
      estimatedCost: null,
      expectedDate: '2026-10-05',
      notes: null,
    });
    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/after-sales', 9]);
  });
});
