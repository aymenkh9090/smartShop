import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { Role } from '../../../core/models/auth.models';
import { AuthService } from '../../../core/services/auth.service';
import { CustomerService } from '../customer.service';
import { CustomerList } from './customer-list';

describe('CustomerList', () => {
  let customers: jasmine.SpyObj<CustomerService>;

  function setup(allowedRoles: Role[]): ComponentFixture<CustomerList> {
    customers = jasmine.createSpyObj<CustomerService>('CustomerService', ['search', 'updateStatus']);
    customers.search.and.returnValue(
      of({
        content: [
          {
            id: 1,
            firstName: 'Ali',
            lastName: 'Ben Salah',
            phone: '22333444',
            email: null,
            address: null,
            cin: null,
            notes: null,
            active: true,
            createdAt: '',
            updatedAt: '',
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      }),
    );
    TestBed.configureTestingModule({
      imports: [CustomerList],
      providers: [
        provideRouter([]),
        { provide: CustomerService, useValue: customers },
        { provide: AuthService, useValue: { hasAnyRole: (roles: Role[]) => roles.some((r) => allowedRoles.includes(r)) } },
      ],
    });
    const fixture = TestBed.createComponent(CustomerList);
    fixture.detectChanges();
    return fixture;
  }

  it('lists active customers on load', () => {
    const fixture = setup(['SELLER']);

    expect(customers.search).toHaveBeenCalledWith(
      { q: null, active: true },
      jasmine.objectContaining({ page: 0, sort: 'lastName,asc' }),
    );
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Ben Salah Ali');
  });

  it('shows the create button to sellers but not to technicians', () => {
    const seller = setup(['SELLER']);
    expect((seller.nativeElement as HTMLElement).textContent).toContain('Nouveau client');

    TestBed.resetTestingModule();
    const technician = setup([]);
    expect((technician.nativeElement as HTMLElement).textContent).not.toContain('Nouveau client');
  });

  it('searches after a debounce and resets to the first page', fakeAsync(() => {
    const fixture = setup(['SELLER']);
    const component = fixture.componentInstance as unknown as { filters: CustomerList['filters'] };

    component.filters.controls.q.setValue('  22 333 ');
    tick(299);
    expect(customers.search).toHaveBeenCalledTimes(1);
    tick(1);
    fixture.detectChanges();

    expect(customers.search).toHaveBeenCalledTimes(2);
    expect(customers.search.calls.mostRecent().args[0]).toEqual({ q: '22 333', active: true });
    expect(customers.search.calls.mostRecent().args[1].page).toBe(0);
  }));
});
