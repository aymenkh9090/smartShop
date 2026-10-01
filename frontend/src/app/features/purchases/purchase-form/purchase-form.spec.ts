import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { NotificationService } from '../../../core/services/notification.service';
import { createPaymentRow } from '../../../shared/components/payments-editor/payments-editor';
import { Product } from '../../products/product.models';
import { ProductService } from '../../products/product.service';
import { Supplier } from '../../suppliers/supplier.models';
import { SupplierService } from '../../suppliers/supplier.service';
import { Purchase, PurchaseRequest } from '../purchase.models';
import { PurchaseService } from '../purchase.service';
import { PurchaseForm } from './purchase-form';

const VALID_IMEI = '490154203237518';

function product(id: number, purchasePrice: number, serialTracked = false): Product {
  return { id, sku: `SKU-${id}`, name: `Produit ${id}`, purchasePrice, quantity: 0, serialTracked } as Product;
}

describe('PurchaseForm', () => {
  let fixture: ComponentFixture<PurchaseForm>;
  let purchases: jasmine.SpyObj<PurchaseService>;

  interface Internals {
    form: PurchaseForm['form'];
    selectSupplier(supplier: Supplier): void;
    addProduct(product: Product): void;
    addUnitRow(line: PurchaseForm['form']['controls']['lines']['controls'][number]): void;
    total(): number;
    remaining(): number;
    blockingError(): string | null;
    submit(): void;
  }

  function c(): Internals {
    return fixture.componentInstance as unknown as Internals;
  }

  beforeEach(() => {
    purchases = jasmine.createSpyObj<PurchaseService>('PurchaseService', ['create']);
    purchases.create.and.returnValue(of({ id: 4, reference: 'A-2026-000004' } as Purchase));
    TestBed.configureTestingModule({
      imports: [PurchaseForm],
      providers: [
        provideRouter([]),
        { provide: PurchaseService, useValue: purchases },
        { provide: NotificationService, useValue: jasmine.createSpyObj('NotificationService', ['success', 'error']) },
        { provide: ProductService, useValue: jasmine.createSpyObj('ProductService', ['search']) },
        { provide: SupplierService, useValue: jasmine.createSpyObj('SupplierService', ['search']) },
      ],
    });
    spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(PurchaseForm);
    fixture.detectChanges();
  });

  it('requires a supplier and at least one line', () => {
    expect(c().blockingError()).toContain('fournisseur');
    c().selectSupplier({ id: 1, name: 'TechDistrib' } as Supplier);
    expect(c().blockingError()).toContain('produit');
  });

  it('prefills the unit cost with the last purchase price and computes the supplier credit', () => {
    c().selectSupplier({ id: 1, name: 'TechDistrib' } as Supplier);
    c().addProduct(product(2, 8.5));
    c().form.controls.lines.at(0).controls.quantity.setValue(10);
    c().form.controls.payments.push(createPaymentRow(50));

    expect(c().form.controls.lines.at(0).controls.unitCost.value).toBe(8.5);
    expect(c().total()).toBe(85);
    expect(c().remaining()).toBe(35);
  });

  it('sends one unit per IMEI row for a serialized product', () => {
    c().selectSupplier({ id: 1, name: 'TechDistrib' } as Supplier);
    c().addProduct(product(5, 1100, true));
    const line = c().form.controls.lines.at(0);
    c().addUnitRow(line);
    line.controls.units.at(0).setValue({ imei: VALID_IMEI, serialNumber: '' });
    line.controls.units.at(1).setValue({ imei: '', serialNumber: 'SN-2' });

    expect(c().total()).toBe(2200);
    c().submit();

    const request: PurchaseRequest = purchases.create.calls.mostRecent().args[0];
    expect(request.items).toEqual([
      {
        productId: 5,
        quantity: 2,
        unitCost: 1100,
        units: [
          { imei: VALID_IMEI, serialNumber: null },
          { imei: null, serialNumber: 'SN-2' },
        ],
      },
    ]);
  });

  it('rejects an invalid IMEI before calling the API', () => {
    c().selectSupplier({ id: 1, name: 'TechDistrib' } as Supplier);
    c().addProduct(product(5, 1100, true));
    c().form.controls.lines.at(0).controls.units.at(0).setValue({ imei: '490154203237519', serialNumber: '' });

    c().submit();

    expect(purchases.create).not.toHaveBeenCalled();
  });
});
