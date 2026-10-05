import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { PageRequest, PageResponse } from '../../core/models/page';
import { PaymentRequest } from '../../core/models/payment';
import { toHttpParams } from '../../shared/utils/http-params';
import {
  SupplierBalance,
  SupplierCredit,
  SupplierCreditFilter,
  SupplierCreditRequest,
  SupplierCreditSource,
  SupplierCreditSummary,
} from './supplier-credit.models';

/** Reference d'un credit : origine + id (achat ou credit saisi). */
export interface SupplierCreditRef {
  source: SupplierCreditSource;
  id: number;
}

@Injectable({ providedIn: 'root' })
export class SupplierCreditService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/supplier-credits`;

  search(filter: SupplierCreditFilter, page: PageRequest): Observable<PageResponse<SupplierCreditSummary>> {
    return this.http.get<PageResponse<SupplierCreditSummary>>(this.url, {
      params: toHttpParams({ ...filter, ...page }),
    });
  }

  balances(supplierId: number | null, page: Omit<PageRequest, 'sort'>): Observable<PageResponse<SupplierBalance>> {
    return this.http.get<PageResponse<SupplierBalance>>(`${this.url}/balances`, {
      params: toHttpParams({ supplierId, ...page }),
    });
  }

  create(request: SupplierCreditRequest): Observable<SupplierCredit> {
    return this.http.post<SupplierCredit>(this.url, request);
  }

  get(credit: SupplierCreditRef): Observable<SupplierCredit> {
    return this.http.get<SupplierCredit>(this.creditUrl(credit));
  }

  pay(credit: SupplierCreditRef, payment: PaymentRequest): Observable<SupplierCredit> {
    return this.http.post<SupplierCredit>(`${this.creditUrl(credit)}/payments`, payment);
  }

  reschedule(credit: SupplierCreditRef, dueDate: string | null): Observable<SupplierCredit> {
    return this.http.patch<SupplierCredit>(`${this.creditUrl(credit)}/due-date`, { dueDate });
  }

  /** Annule un credit saisi par erreur (sans paiement). */
  cancel(id: number, reason: string): Observable<SupplierCredit> {
    return this.http.post<SupplierCredit>(`${this.url}/manual/${id}/cancel`, { reason });
  }

  private creditUrl(credit: SupplierCreditRef): string {
    return credit.source === 'MANUAL' ? `${this.url}/manual/${credit.id}` : `${this.url}/${credit.id}`;
  }
}
