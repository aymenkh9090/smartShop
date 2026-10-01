import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { PageRequest, PageResponse } from '../../core/models/page';
import { toHttpParams } from '../../shared/utils/http-params';
import { Product, ProductFilter, ProductRequest } from './product.models';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/products`;

  search(filter: ProductFilter, page: PageRequest): Observable<PageResponse<Product>> {
    return this.http.get<PageResponse<Product>>(this.url, { params: toHttpParams({ ...filter, ...page }) });
  }

  get(id: number): Observable<Product> {
    return this.http.get<Product>(`${this.url}/${id}`);
  }

  create(request: ProductRequest): Observable<Product> {
    return this.http.post<Product>(this.url, request);
  }

  update(id: number, request: ProductRequest): Observable<Product> {
    return this.http.put<Product>(`${this.url}/${id}`, request);
  }

  updateStatus(id: number, active: boolean): Observable<Product> {
    return this.http.patch<Product>(`${this.url}/${id}/status`, { active });
  }
}
