import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { NotificationService } from '../../../core/services/notification.service';
import { Product } from '../../products/product.models';
import { ProductService } from '../../products/product.service';
import { StockService } from '../stock.service';
import { Inventory } from './inventory';

function product(id: number, quantity: number, serialTracked = false): Product {
  return { id, sku: `SKU-${id}`, name: `Produit ${id}`, quantity, serialTracked } as Product;
}

describe('Inventory', () => {
  let fixture: ComponentFixture<Inventory>;
  let stock: jasmine.SpyObj<StockService>;
  let notifications: jasmine.SpyObj<NotificationService>;

  interface Internals {
    form: Inventory['form'];
    addProduct(product: Product): void;
    difference(line: Inventory['form']['controls']['lines']['controls'][number]): number | null;
    submit(): void;
  }

  function component(): Internals {
    return fixture.componentInstance as unknown as Internals;
  }

  beforeEach(() => {
    stock = jasmine.createSpyObj<StockService>('StockService', ['applyInventory']);
    stock.applyInventory.and.returnValue(of({ countedProducts: 2, adjustedProducts: 1, movements: [] }));
    notifications = jasmine.createSpyObj<NotificationService>('NotificationService', ['success', 'error']);
    TestBed.configureTestingModule({
      imports: [Inventory],
      providers: [
        provideRouter([]),
        { provide: StockService, useValue: stock },
        { provide: NotificationService, useValue: notifications },
        { provide: ProductService, useValue: jasmine.createSpyObj('ProductService', ['search']) },
      ],
    });
    spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(Inventory);
    fixture.detectChanges();
  });

  it('refuses serialized products and duplicates', () => {
    component().addProduct(product(1, 3, true));
    component().addProduct(product(2, 3));
    component().addProduct(product(2, 3));

    expect(component().form.controls.lines.length).toBe(1);
    expect(notifications.error).toHaveBeenCalledTimes(2);
  });

  it('computes the difference with the theoretical stock', () => {
    component().addProduct(product(2, 10));
    const line = component().form.controls.lines.at(0);

    expect(component().difference(line)).toBeNull();
    line.controls.countedQuantity.setValue(7);
    expect(component().difference(line)).toBe(-3);
  });

  it('requires every counted quantity, then submits all lines', () => {
    component().addProduct(product(2, 10));
    component().addProduct(product(3, 4));
    component().form.controls.lines.at(0).controls.countedQuantity.setValue(8);

    component().submit();
    expect(stock.applyInventory).not.toHaveBeenCalled();

    component().form.controls.lines.at(1).controls.countedQuantity.setValue(4);
    component().form.controls.reference.setValue(' INV-1 ');
    component().submit();

    expect(stock.applyInventory).toHaveBeenCalledWith({
      reference: 'INV-1',
      reason: null,
      lines: [
        { productId: 2, countedQuantity: 8 },
        { productId: 3, countedQuantity: 4 },
      ],
    });
    expect(notifications.success).toHaveBeenCalled();
  });
});
