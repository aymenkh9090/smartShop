import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { NotificationService } from '../../../core/services/notification.service';
import { Product } from '../../products/product.models';
import { ProductService } from '../../products/product.service';
import { StockMovement, StockMovementRequest } from '../stock.models';
import { StockService } from '../stock.service';
import { MovementForm } from './movement-form';

const VALID_IMEI = '490154203237518';

function product(serialTracked: boolean): Product {
  return {
    id: 5,
    sku: 'SM-A55',
    barcode: null,
    reference: null,
    name: 'Galaxy A55',
    description: null,
    category: { id: 1, name: 'Smartphones' },
    type: 'SMARTPHONE',
    condition: 'NEW',
    brand: null,
    model: null,
    purchasePrice: 1000,
    salePrice: 1200,
    quantity: 3,
    minStock: 1,
    lowStock: false,
    serialTracked,
    warrantyMonths: 12,
    active: true,
    createdAt: '',
    updatedAt: '',
  };
}

describe('MovementForm', () => {
  let fixture: ComponentFixture<MovementForm>;
  let stock: jasmine.SpyObj<StockService>;
  let notifications: jasmine.SpyObj<NotificationService>;

  interface Internals {
    form: MovementForm['form'];
    selectProduct(product: Product): void;
    addUnitRow(): void;
    selectedUnitIds: { set(ids: number[]): void };
    unitMode(): string;
    submit(): void;
  }

  function component(): Internals {
    return fixture.componentInstance as unknown as Internals;
  }

  beforeEach(() => {
    stock = jasmine.createSpyObj<StockService>('StockService', ['recordMovement', 'searchUnits']);
    stock.recordMovement.and.returnValue(of({ quantityAfter: 4 } as StockMovement));
    stock.searchUnits.and.returnValue(
      of({
        content: [
          {
            id: 21,
            productId: 5,
            productSku: 'SM-A55',
            productName: 'Galaxy A55',
            imei: VALID_IMEI,
            serialNumber: null,
            status: 'IN_STOCK',
            createdAt: '',
            updatedAt: '',
          },
        ],
        page: 0,
        size: 100,
        totalElements: 1,
        totalPages: 1,
      }),
    );
    notifications = jasmine.createSpyObj<NotificationService>('NotificationService', ['success', 'error']);
    TestBed.configureTestingModule({
      imports: [MovementForm],
      providers: [
        provideRouter([]),
        { provide: StockService, useValue: stock },
        { provide: NotificationService, useValue: notifications },
        { provide: ProductService, useValue: jasmine.createSpyObj('ProductService', ['get', 'search']) },
      ],
    });
    spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(MovementForm);
    fixture.detectChanges();
  });

  function lastRequest(): StockMovementRequest {
    return stock.recordMovement.calls.mostRecent().args[0];
  }

  it('requires a reason for a stock out of a simple product', () => {
    component().selectProduct(product(false));
    component().form.controls.type.setValue('STOCK_OUT');
    component().form.controls.quantity.setValue(2);

    component().submit();
    expect(stock.recordMovement).not.toHaveBeenCalled();

    component().form.controls.reason.setValue('Casse');
    component().submit();
    expect(lastRequest()).toEqual(
      jasmine.objectContaining({ productId: 5, type: 'STOCK_OUT', quantity: 2, reason: 'Casse', units: null }),
    );
  });

  it('accepts a negative adjustment but not a zero one', () => {
    component().selectProduct(product(false));
    component().form.controls.type.setValue('ADJUSTMENT');
    component().form.controls.reason.setValue('Écart');

    component().form.controls.quantity.setValue(0);
    component().submit();
    expect(stock.recordMovement).not.toHaveBeenCalled();

    component().form.controls.quantity.setValue(-1);
    component().submit();
    expect(lastRequest().quantity).toBe(-1);
  });

  it('accepts a serialized stock in without any IMEI', () => {
    component().selectProduct(product(true));
    expect(component().unitMode()).toBe('new');
    expect(component().form.controls.units.length).toBe(0);

    component().form.controls.quantity.setValue(3);
    component().submit();

    expect(lastRequest()).toEqual(
      jasmine.objectContaining({ type: 'STOCK_IN', quantity: 3, units: [], unitIds: null }),
    );
  });

  it('sends only the filled IMEI rows, at most one per item', () => {
    component().selectProduct(product(true));
    component().addUnitRow();
    component().addUnitRow();
    component().addUnitRow();
    const rows = component().form.controls.units;
    rows.at(0).setValue({ imei: VALID_IMEI, serialNumber: '' });
    rows.at(1).setValue({ imei: '', serialNumber: 'sn-2' });
    rows.at(2).setValue({ imei: '', serialNumber: '' });

    component().form.controls.quantity.setValue(1);
    component().submit();
    expect(stock.recordMovement).not.toHaveBeenCalled();

    component().form.controls.quantity.setValue(5);
    component().submit();
    expect(lastRequest()).toEqual(
      jasmine.objectContaining({
        type: 'STOCK_IN',
        quantity: 5,
        units: [
          { imei: VALID_IMEI, serialNumber: null },
          { imei: null, serialNumber: 'sn-2' },
        ],
      }),
    );
  });

  it('rejects an invalid IMEI', () => {
    component().selectProduct(product(true));
    component().addUnitRow();
    component().form.controls.units.at(0).setValue({ imei: '490154203237519', serialNumber: '' });

    component().submit();

    expect(stock.recordMovement).not.toHaveBeenCalled();
  });

  it('lets a serialized stock out designate units, or none', () => {
    component().selectProduct(product(true));
    component().form.controls.type.setValue('STOCK_OUT');
    component().form.controls.reason.setValue('Écran casse');

    expect(component().unitMode()).toBe('existing');
    expect(stock.searchUnits).toHaveBeenCalledWith(
      { productId: 5, status: 'IN_STOCK' },
      jasmine.objectContaining({ size: 100 }),
    );

    component().submit();
    expect(lastRequest()).toEqual(
      jasmine.objectContaining({ type: 'STOCK_OUT', quantity: 1, unitIds: [], units: null }),
    );

    component().selectedUnitIds.set([21]);
    component().submit();
    expect(lastRequest()).toEqual(
      jasmine.objectContaining({ type: 'STOCK_OUT', quantity: 1, unitIds: [21], units: null }),
    );
  });

  it('offers adjustments for serialized products too', () => {
    component().form.controls.type.setValue('ADJUSTMENT');
    component().selectProduct(product(true));

    expect(component().form.controls.type.value).toBe('ADJUSTMENT');
    expect(component().unitMode()).toBe('none');
  });
});
