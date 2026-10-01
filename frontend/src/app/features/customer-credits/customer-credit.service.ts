import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { PageRequest, PageResponse } from '../../core/models/page';
import { PaymentRequest } from '../../core/models/payment';
import { toHttpParams } from '../../shared/utils/http-params';
import { CustomerBalance, CustomerCredit, CustomerCreditFilter } from './customer-credit.models';

@Injectable({ providedIn: 'root' })
export class CustomerCreditService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/customer-credits`;

  search(filter: CustomerCreditFilter, page: PageRequest): Observable<PageResponse<CustomerCredit>> {
    return this.http.get<PageResponse<CustomerCredit>>(this.url, { params: toHttpParams({ ...filter, ...page }) });
  }

  balances(customerId: number | null, page: Omit<PageRequest, 'sort'>): Observable<PageResponse<CustomerBalance>> {
    return this.http.get<PageResponse<CustomerBalance>>(`${this.url}/balances`, {
      params: toHttpParams({ customerId, ...page }),
    });
  }

  get(saleId: number): Observable<CustomerCredit> {
    return this.http.get<CustomerCredit>(`${this.url}/${saleId}`);
  }

  pay(saleId: number, payment: PaymentRequest): Observable<CustomerCredit> {
    return this.http.post<CustomerCredit>(`${this.url}/${saleId}/payments`, payment);
  }

  reschedule(saleId: number, dueDate: string | null): Observable<CustomerCredit> {
    return this.http.patch<CustomerCredit>(`${this.url}/${saleId}/due-date`, { dueDate });
  }
}
