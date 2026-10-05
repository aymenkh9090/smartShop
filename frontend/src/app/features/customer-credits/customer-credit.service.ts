import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { PageRequest, PageResponse } from '../../core/models/page';
import { PaymentRequest } from '../../core/models/payment';
import { toHttpParams } from '../../shared/utils/http-params';
import {
  CustomerBalance,
  CustomerCredit,
  CustomerCreditFilter,
  CustomerCreditRequest,
  CustomerCreditSource,
  CustomerCreditSummary,
} from './customer-credit.models';

/** Reference d'un credit : origine + id (vente ou credit saisi). */
export interface CustomerCreditRef {
  source: CustomerCreditSource;
  id: number;
}

@Injectable({ providedIn: 'root' })
export class CustomerCreditService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/customer-credits`;

  search(filter: CustomerCreditFilter, page: PageRequest): Observable<PageResponse<CustomerCreditSummary>> {
    return this.http.get<PageResponse<CustomerCreditSummary>>(this.url, {
      params: toHttpParams({ ...filter, ...page }),
    });
  }

  balances(customerId: number | null, page: Omit<PageRequest, 'sort'>): Observable<PageResponse<CustomerBalance>> {
    return this.http.get<PageResponse<CustomerBalance>>(`${this.url}/balances`, {
      params: toHttpParams({ customerId, ...page }),
    });
  }

  create(request: CustomerCreditRequest): Observable<CustomerCredit> {
    return this.http.post<CustomerCredit>(this.url, request);
  }

  get(credit: CustomerCreditRef): Observable<CustomerCredit> {
    return this.http.get<CustomerCredit>(this.creditUrl(credit));
  }

  pay(credit: CustomerCreditRef, payment: PaymentRequest): Observable<CustomerCredit> {
    return this.http.post<CustomerCredit>(`${this.creditUrl(credit)}/payments`, payment);
  }

  reschedule(credit: CustomerCreditRef, dueDate: string | null): Observable<CustomerCredit> {
    return this.http.patch<CustomerCredit>(`${this.creditUrl(credit)}/due-date`, { dueDate });
  }

  /** Annule un credit saisi par erreur (sans paiement). */
  cancel(id: number, reason: string): Observable<CustomerCredit> {
    return this.http.post<CustomerCredit>(`${this.url}/manual/${id}/cancel`, { reason });
  }

  private creditUrl(credit: CustomerCreditRef): string {
    return credit.source === 'MANUAL' ? `${this.url}/manual/${credit.id}` : `${this.url}/${credit.id}`;
  }
}
