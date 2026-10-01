import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { PageRequest, PageResponse } from '../../core/models/page';
import { toHttpParams } from '../../shared/utils/http-params';
import {
  InventoryRequest,
  InventoryResponse,
  ProductUnit,
  ProductUnitFilter,
  StockMovement,
  StockMovementFilter,
  StockMovementRequest,
} from './stock.models';

@Injectable({ providedIn: 'root' })
export class StockService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/stock`;

  searchMovements(filter: StockMovementFilter, page: PageRequest): Observable<PageResponse<StockMovement>> {
    return this.http.get<PageResponse<StockMovement>>(`${this.url}/movements`, {
      params: toHttpParams({ ...filter, ...page }),
    });
  }

  recordMovement(request: StockMovementRequest): Observable<StockMovement> {
    return this.http.post<StockMovement>(`${this.url}/movements`, request);
  }

  applyInventory(request: InventoryRequest): Observable<InventoryResponse> {
    return this.http.post<InventoryResponse>(`${this.url}/inventory`, request);
  }

  searchUnits(filter: ProductUnitFilter, page: PageRequest): Observable<PageResponse<ProductUnit>> {
    return this.http.get<PageResponse<ProductUnit>>(`${this.url}/units`, {
      params: toHttpParams({ ...filter, ...page }),
    });
  }
}
