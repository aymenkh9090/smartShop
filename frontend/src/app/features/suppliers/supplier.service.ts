import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { PageRequest, PageResponse } from '../../core/models/page';
import { toHttpParams } from '../../shared/utils/http-params';
import { Supplier, SupplierFilter, SupplierRequest } from './supplier.models';

@Injectable({ providedIn: 'root' })
export class SupplierService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/suppliers`;

  search(filter: SupplierFilter, page: PageRequest): Observable<PageResponse<Supplier>> {
    return this.http.get<PageResponse<Supplier>>(this.url, { params: toHttpParams({ ...filter, ...page }) });
  }

  get(id: number): Observable<Supplier> {
    return this.http.get<Supplier>(`${this.url}/${id}`);
  }

  create(request: SupplierRequest): Observable<Supplier> {
    return this.http.post<Supplier>(this.url, request);
  }

  update(id: number, request: SupplierRequest): Observable<Supplier> {
    return this.http.put<Supplier>(`${this.url}/${id}`, request);
  }

  updateStatus(id: number, active: boolean): Observable<Supplier> {
    return this.http.patch<Supplier>(`${this.url}/${id}/status`, { active });
  }
}
