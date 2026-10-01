import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { PageRequest, PageResponse } from '../../core/models/page';
import { toHttpParams } from '../../shared/utils/http-params';
import { Customer, CustomerFilter, CustomerRequest } from './customer.models';

@Injectable({ providedIn: 'root' })
export class CustomerService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/customers`;

  search(filter: CustomerFilter, page: PageRequest): Observable<PageResponse<Customer>> {
    return this.http.get<PageResponse<Customer>>(this.url, { params: toHttpParams({ ...filter, ...page }) });
  }

  get(id: number): Observable<Customer> {
    return this.http.get<Customer>(`${this.url}/${id}`);
  }

  create(request: CustomerRequest): Observable<Customer> {
    return this.http.post<Customer>(this.url, request);
  }

  update(id: number, request: CustomerRequest): Observable<Customer> {
    return this.http.put<Customer>(`${this.url}/${id}`, request);
  }

  updateStatus(id: number, active: boolean): Observable<Customer> {
    return this.http.patch<Customer>(`${this.url}/${id}/status`, { active });
  }
}
