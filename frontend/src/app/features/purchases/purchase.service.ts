import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { PageRequest, PageResponse } from '../../core/models/page';
import { toHttpParams } from '../../shared/utils/http-params';
import { Purchase, PurchaseFilter, PurchaseRequest, PurchaseSummary } from './purchase.models';

@Injectable({ providedIn: 'root' })
export class PurchaseService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/purchases`;

  search(filter: PurchaseFilter, page: PageRequest): Observable<PageResponse<PurchaseSummary>> {
    return this.http.get<PageResponse<PurchaseSummary>>(this.url, { params: toHttpParams({ ...filter, ...page }) });
  }

  get(id: number): Observable<Purchase> {
    return this.http.get<Purchase>(`${this.url}/${id}`);
  }

  create(request: PurchaseRequest): Observable<Purchase> {
    return this.http.post<Purchase>(this.url, request);
  }
}
