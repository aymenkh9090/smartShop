import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { ProductService } from './product.service';

describe('ProductService', () => {
  let service: ProductService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ProductService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends filters, pagination and sort as query params, skipping empty filters', () => {
    service
      .search({ q: 'galaxy', categoryId: null, type: 'SMARTPHONE', active: true }, { page: 2, size: 20, sort: 'name,asc' })
      .subscribe();

    const req = http.expectOne((r) => r.url === `${environment.apiUrl}/products`);
    expect(req.request.params.get('q')).toBe('galaxy');
    expect(req.request.params.has('categoryId')).toBeFalse();
    expect(req.request.params.get('type')).toBe('SMARTPHONE');
    expect(req.request.params.get('active')).toBe('true');
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('sort')).toBe('name,asc');
    req.flush({ content: [], page: 2, size: 20, totalElements: 0, totalPages: 0 });
  });

  it('changes the status with PATCH', () => {
    service.updateStatus(7, false).subscribe();

    const req = http.expectOne(`${environment.apiUrl}/products/7/status`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ active: false });
    req.flush({});
  });
});
