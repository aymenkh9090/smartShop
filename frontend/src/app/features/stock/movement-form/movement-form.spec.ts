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
    component().form.controls.reason.setValue('Ecart');

    component().form.controls.quantity.setValue(0);
    component().submit();
    expect(stock.recordMovement).not.toHaveBeenCalled();

    component().form.controls.quantity.setValue(-1);
    component().submit();
    expect(lastRequest().quantity).toBe(-1);
  });

  it('builds one unit per row for a serialized stock in, quantity derived from the rows', () => {
    component().selectProduct(product(true));
    expect(component().unitMode()).toBe('new');

    component().addUnitRow();
    const rows = component().form.controls.units;
    rows.at(0).setValue({ imei: VALID_IMEI, serialNumber: '' });
    rows.at(1).setValue({ imei: '', serialNumber: 'sn-2' });

    component().submit();

    expect(lastRequest()).toEqual(
      jasmine.objectContaining({
        type: 'STOCK_IN',
        quantity: 2,
        units: [
          { imei: VALID_IMEI, serialNumber: null },
          { imei: null, serialNumber: 'sn-2' },
        ],
        unitIds: null,
      }),
    );
  });

  it('rejects a unit row without any identifier or with an invalid IMEI', () => {
    component().selectProduct(product(true));
    const row = component().form.controls.units.at(0);

    row.setValue({ imei: '', serialNumber: '' });
    component().submit();
    row.setValue({ imei: '490154203237519', serialNumber: '' });
    component().submit();

    expect(stock.recordMovement).not.toHaveBeenCalled();
  });

  it('selects existing units for a serialized stock out', () => {
    component().selectProduct(product(true));
    component().form.controls.type.setValue('STOCK_OUT');
    component().form.controls.reason.setValue('Ecran casse');

    expect(component().unitMode()).toBe('existing');
    expect(stock.searchUnits).toHaveBeenCalledWith(
      { productId: 5, status: 'IN_STOCK' },
      jasmine.objectContaining({ size: 100 }),
    );

    component().submit();
    expect(stock.recordMovement).not.toHaveBeenCalled();

    component().selectedUnitIds.set([21]);
    component().submit();
    expect(lastRequest()).toEqual(
      jasmine.objectContaining({ type: 'STOCK_OUT', quantity: 1, unitIds: [21], units: null }),
    );
  });

  it('does not offer adjustments for serialized products', () => {
    component().form.controls.type.setValue('ADJUSTMENT');
    component().selectProduct(product(true));

    expect(component().form.controls.type.value).toBe('STOCK_IN');
  });
});
