import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { PageRequest, PageResponse } from '../../core/models/page';
import { PaymentRequest } from '../../core/models/payment';
import { toHttpParams } from '../../shared/utils/http-params';
import { SupplierBalance, SupplierCredit, SupplierCreditFilter } from './supplier-credit.models';

@Injectable({ providedIn: 'root' })
export class SupplierCreditService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/supplier-credits`;

  search(filter: SupplierCreditFilter, page: PageRequest): Observable<PageResponse<SupplierCredit>> {
    return this.http.get<PageResponse<SupplierCredit>>(this.url, { params: toHttpParams({ ...filter, ...page }) });
  }

  balances(supplierId: number | null, page: Omit<PageRequest, 'sort'>): Observable<PageResponse<SupplierBalance>> {
    return this.http.get<PageResponse<SupplierBalance>>(`${this.url}/balances`, {
      params: toHttpParams({ supplierId, ...page }),
    });
  }

  get(purchaseId: number): Observable<SupplierCredit> {
    return this.http.get<SupplierCredit>(`${this.url}/${purchaseId}`);
  }

  pay(purchaseId: number, payment: PaymentRequest): Observable<SupplierCredit> {
    return this.http.post<SupplierCredit>(`${this.url}/${purchaseId}/payments`, payment);
  }

  reschedule(purchaseId: number, dueDate: string | null): Observable<SupplierCredit> {
    return this.http.patch<SupplierCredit>(`${this.url}/${purchaseId}/due-date`, { dueDate });
  }
}
