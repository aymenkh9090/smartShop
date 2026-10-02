import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { PageRequest, PageResponse } from '../../core/models/page';
import { PaymentRequest } from '../../core/models/payment';
import { toHttpParams } from '../../shared/utils/http-params';
import {
  AfterSalesCreateRequest,
  AfterSalesFilter,
  AfterSalesPartRequest,
  AfterSalesStatus,
  AfterSalesSummary,
  AfterSalesTicket,
  AfterSalesWorkRequest,
  DeliverRequest,
  Technician,
} from './after-sales.models';

@Injectable({ providedIn: 'root' })
export class AfterSalesService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/after-sales`;

  search(filter: AfterSalesFilter, page: PageRequest): Observable<PageResponse<AfterSalesSummary>> {
    return this.http.get<PageResponse<AfterSalesSummary>>(this.url, { params: toHttpParams({ ...filter, ...page }) });
  }

  get(id: number): Observable<AfterSalesTicket> {
    return this.http.get<AfterSalesTicket>(`${this.url}/${id}`);
  }

  technicians(): Observable<Technician[]> {
    return this.http.get<Technician[]>(`${this.url}/technicians`);
  }

  create(request: AfterSalesCreateRequest): Observable<AfterSalesTicket> {
    return this.http.post<AfterSalesTicket>(this.url, request);
  }

  updateWork(id: number, request: AfterSalesWorkRequest): Observable<AfterSalesTicket> {
    return this.http.put<AfterSalesTicket>(`${this.url}/${id}`, request);
  }

  assignTechnician(id: number, technicianId: number): Observable<AfterSalesTicket> {
    return this.http.patch<AfterSalesTicket>(`${this.url}/${id}/technician`, { technicianId });
  }

  changeStatus(id: number, status: AfterSalesStatus, comment: string | null): Observable<AfterSalesTicket> {
    return this.http.post<AfterSalesTicket>(`${this.url}/${id}/status`, { status, comment });
  }

  addPart(id: number, request: AfterSalesPartRequest): Observable<AfterSalesTicket> {
    return this.http.post<AfterSalesTicket>(`${this.url}/${id}/parts`, request);
  }

  removePart(id: number, partId: number): Observable<AfterSalesTicket> {
    return this.http.delete<AfterSalesTicket>(`${this.url}/${id}/parts/${partId}`);
  }

  pay(id: number, payment: PaymentRequest): Observable<AfterSalesTicket> {
    return this.http.post<AfterSalesTicket>(`${this.url}/${id}/payments`, payment);
  }

  deliver(id: number, request: DeliverRequest): Observable<AfterSalesTicket> {
    return this.http.post<AfterSalesTicket>(`${this.url}/${id}/deliver`, request);
  }

  cancel(id: number, reason: string): Observable<AfterSalesTicket> {
    return this.http.post<AfterSalesTicket>(`${this.url}/${id}/cancel`, { reason });
  }
}
