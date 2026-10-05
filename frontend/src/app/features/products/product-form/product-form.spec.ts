import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { NotificationService } from '../../../core/services/notification.service';
import { CategoryService } from '../../categories/category.service';
import { Product } from '../product.models';
import { ProductService } from '../product.service';
import { ProductForm } from './product-form';

const PRODUCT: Product = {
  id: 7,
  sku: 'SM-A55',
  barcode: null,
  reference: 'SM-A556B',
  name: 'Galaxy A55',
  description: null,
  category: { id: 1, name: 'Smartphones' },
  type: 'SMARTPHONE',
  condition: 'NEW',
  brand: 'Samsung',
  model: 'A55',
  purchasePrice: 1100,
  salePrice: 1299.5,
  quantity: 4,
  minStock: 2,
  lowStock: false,
  serialTracked: true,
  warrantyMonths: 12,
  active: true,
  createdAt: '2026-10-01T10:00:00Z',
  updatedAt: '2026-10-01T10:00:00Z',
};

describe('ProductForm', () => {
  let fixture: ComponentFixture<ProductForm>;
  let products: jasmine.SpyObj<ProductService>;
  let notifications: jasmine.SpyObj<NotificationService>;
  let router: Router;

  beforeEach(() => {
    products = jasmine.createSpyObj<ProductService>('ProductService', ['get', 'create', 'update']);
    notifications = jasmine.createSpyObj<NotificationService>('NotificationService', ['success', 'error']);
    TestBed.configureTestingModule({
      imports: [ProductForm],
      providers: [
        provideRouter([]),
        { provide: ProductService, useValue: products },
        { provide: NotificationService, useValue: notifications },
        { provide: CategoryService, useValue: { list: () => of([{ id: 1, name: 'Smartphones' }]) } },
      ],
    });
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(ProductForm);
  });

  function form() {
    return (fixture.componentInstance as unknown as { form: ProductForm['form'] }).form;
  }

  function submit(): void {
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button[type=submit]')!.click();
    fixture.detectChanges();
  }

  it('does not submit an incomplete form', () => {
    fixture.detectChanges();
    submit();
    expect(products.create).not.toHaveBeenCalled();
  });

  it('creates a product without ever sending a quantity', () => {
    fixture.detectChanges();
    form().patchValue({
      sku: 'SM-A55',
      name: 'Galaxy A55',
      categoryId: 1,
      type: 'SMARTPHONE',
      purchasePrice: 1100,
      salePrice: 1299.5,
    });
    products.create.and.returnValue(of(PRODUCT));

    submit();

    const request = products.create.calls.mostRecent().args[0];
    expect(request.sku).toBe('SM-A55');
    expect(request.categoryId).toBe(1);
    expect('quantity' in request).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/products']);
  });

  it('loads the product in edit mode and updates it', () => {
    products.get.and.returnValue(of(PRODUCT));
    products.update.and.returnValue(of(PRODUCT));
    fixture.componentRef.setInput('id', '7');
    fixture.detectChanges();

    expect(products.get).toHaveBeenCalledWith(7);
    expect(form().controls.categoryId.value).toBe(1);
    expect(form().controls.barcode.value).toBe('');

    submit();
    expect(products.update).toHaveBeenCalledWith(7, jasmine.objectContaining({ sku: 'SM-A55' }));
  });

  it('shows backend field errors on the matching control', () => {
    fixture.detectChanges();
    form().patchValue({ sku: 'SM-A55', name: 'X', categoryId: 1, type: 'CABLE', purchasePrice: 1, salePrice: 2 });
    products.create.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 400,
            error: { message: 'Données invalides', errors: [{ field: 'sku', message: 'déjà utilise' }] },
          }),
      ),
    );

    submit();

    expect(form().controls.sku.errors?.['server']).toBe('déjà utilise');
    expect(notifications.error).toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
