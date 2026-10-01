import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { PageRequest, PageResponse } from '../../core/models/page';
import { toHttpParams } from '../../shared/utils/http-params';
import { Sale, SaleFilter, SaleRequest, SaleSummary } from './sale.models';

@Injectable({ providedIn: 'root' })
export class SaleService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/sales`;

  search(filter: SaleFilter, page: PageRequest): Observable<PageResponse<SaleSummary>> {
    return this.http.get<PageResponse<SaleSummary>>(this.url, { params: toHttpParams({ ...filter, ...page }) });
  }

  get(id: number): Observable<Sale> {
    return this.http.get<Sale>(`${this.url}/${id}`);
  }

  create(request: SaleRequest): Observable<Sale> {
    return this.http.post<Sale>(this.url, request);
  }

  cancel(id: number, reason: string): Observable<Sale> {
    return this.http.post<Sale>(`${this.url}/${id}/cancel`, { reason });
  }
}
