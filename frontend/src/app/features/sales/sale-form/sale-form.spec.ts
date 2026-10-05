import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { NotificationService } from '../../../core/services/notification.service';
import { createPaymentRow } from '../../../shared/components/payments-editor/payments-editor';
import { Customer } from '../../customers/customer.models';
import { CustomerService } from '../../customers/customer.service';
import { Product } from '../../products/product.models';
import { ProductService } from '../../products/product.service';
import { StockService } from '../../stock/stock.service';
import { Sale, SaleRequest } from '../sale.models';
import { SaleService } from '../sale.service';
import { SaleForm } from './sale-form';

function product(id: number, salePrice: number, quantity: number, serialTracked = false): Product {
  return { id, sku: `SKU-${id}`, name: `Produit ${id}`, salePrice, quantity, serialTracked } as Product;
}

const CUSTOMER = { id: 3, firstName: 'Ali', lastName: 'Ben Salah', phone: '22333444' } as Customer;

describe('SaleForm', () => {
  let fixture: ComponentFixture<SaleForm>;
  let sales: jasmine.SpyObj<SaleService>;
  let notifications: jasmine.SpyObj<NotificationService>;

  interface Internals {
    form: SaleForm['form'];
    addProduct(product: Product): void;
    selectCustomer(customer: Customer): void;
    subtotal(): number;
    total(): number;
    remaining(): number;
    blockingError(): string | null;
    syncQuantity(line: SaleForm['form']['controls']['lines']['controls'][number]): void;
    submit(): void;
  }

  function c(): Internals {
    return fixture.componentInstance as unknown as Internals;
  }

  function pay(amount: number): void {
    c().form.controls.payments.push(createPaymentRow(amount));
  }

  beforeEach(() => {
    sales = jasmine.createSpyObj<SaleService>('SaleService', ['create']);
    sales.create.and.returnValue(of({ id: 9, reference: 'V-2026-000009' } as Sale));
    notifications = jasmine.createSpyObj<NotificationService>('NotificationService', ['success', 'error']);
    const stock = jasmine.createSpyObj<StockService>('StockService', ['searchUnits']);
    stock.searchUnits.and.returnValue(of({ content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 }));
    TestBed.configureTestingModule({
      imports: [SaleForm],
      providers: [
        provideRouter([]),
        { provide: SaleService, useValue: sales },
        { provide: StockService, useValue: stock },
        { provide: NotificationService, useValue: notifications },
        { provide: ProductService, useValue: jasmine.createSpyObj('ProductService', ['search']) },
        { provide: CustomerService, useValue: jasmine.createSpyObj('CustomerService', ['search']) },
      ],
    });
    spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(SaleForm);
    fixture.detectChanges();
  });

  it('computes totals with the discount, exactly to the millime', () => {
    c().addProduct(product(1, 19.99, 10));
    c().form.controls.lines.at(0).controls.quantity.setValue(3);
    c().form.controls.discount.setValue(0.97);

    expect(c().subtotal()).toBe(59.97);
    expect(c().total()).toBe(59);
    expect(c().remaining()).toBe(59);
  });

  it('blocks a credit sale without customer, then allows it once a customer is chosen', () => {
    c().addProduct(product(1, 100, 5));
    pay(40);
    expect(c().blockingError()).toContain('sélectionnez un client');

    c().selectCustomer(CUSTOMER);
    expect(c().blockingError()).toBeNull();

    c().submit();
    const request: SaleRequest = sales.create.calls.mostRecent().args[0];
    expect(request.customerId).toBe(3);
    expect(request.payments).toEqual([{ amount: 40, method: 'CASH', reference: null }]);
  });

  it('blocks an overpayment and an excessive discount', () => {
    c().addProduct(product(1, 100, 5));
    pay(150);
    expect(c().blockingError()).toContain('dépasse le total');

    c().form.controls.payments.clear();
    c().form.controls.discount.setValue(120);
    expect(c().blockingError()).toContain('remise');
    expect(sales.create).not.toHaveBeenCalled();
  });

  it('refuses duplicate lines and out-of-stock products', () => {
    c().addProduct(product(1, 100, 5));
    c().addProduct(product(1, 100, 5));
    c().addProduct(product(2, 10, 0));

    expect(c().form.controls.lines.length).toBe(1);
    expect(notifications.error).toHaveBeenCalledTimes(2);
  });

  it('sells a serialized product by quantity, without IMEI', () => {
    c().addProduct(product(4, 1299.5, 3, true));
    c().form.controls.lines.at(0).controls.quantity.setValue(2);
    pay(2599);

    expect(c().subtotal()).toBe(2599);
    c().submit();

    expect(sales.create.calls.mostRecent().args[0].items).toEqual([{ productId: 4, quantity: 2, unitIds: null }]);
  });

  it('sends the selected IMEI units, raising the quantity if needed', () => {
    c().addProduct(product(4, 1299.5, 3, true));
    const line = c().form.controls.lines.at(0);
    line.controls.unitIds.setValue([11, 12]);
    c().syncQuantity(line);
    pay(2599);

    expect(line.controls.quantity.value).toBe(2);
    c().submit();

    expect(sales.create.calls.mostRecent().args[0].items).toEqual([{ productId: 4, quantity: 2, unitIds: [11, 12] }]);
  });

  it('refuses more IMEI than the quantity', () => {
    c().addProduct(product(4, 1299.5, 3, true));
    c().selectCustomer(CUSTOMER);
    const line = c().form.controls.lines.at(0);
    line.controls.unitIds.setValue([11, 12]);

    expect(c().blockingError()).toContain("plus d'IMEI");
  });

  it('sends a due date only when a balance remains', () => {
    c().addProduct(product(1, 100, 5));
    c().selectCustomer(CUSTOMER);
    c().form.controls.dueDate.setValue(new Date(2026, 10, 30));
    pay(100);

    c().submit();

    expect(sales.create.calls.mostRecent().args[0].dueDate).toBeNull();
  });
});
