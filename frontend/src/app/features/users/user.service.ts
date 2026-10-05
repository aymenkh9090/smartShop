import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { PageRequest, PageResponse } from '../../core/models/page';
import { toHttpParams } from '../../shared/utils/http-params';
import { ManagedUser, UserCreateRequest, UserFilter, UserUpdateRequest } from './user.models';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/users`;

  search(filter: UserFilter, page: PageRequest): Observable<PageResponse<ManagedUser>> {
    return this.http.get<PageResponse<ManagedUser>>(this.url, { params: toHttpParams({ ...filter, ...page }) });
  }

  get(id: number): Observable<ManagedUser> {
    return this.http.get<ManagedUser>(`${this.url}/${id}`);
  }

  create(request: UserCreateRequest): Observable<ManagedUser> {
    return this.http.post<ManagedUser>(this.url, request);
  }

  update(id: number, request: UserUpdateRequest): Observable<ManagedUser> {
    return this.http.put<ManagedUser>(`${this.url}/${id}`, request);
  }

  updateStatus(id: number, active: boolean): Observable<ManagedUser> {
    return this.http.patch<ManagedUser>(`${this.url}/${id}/status`, { active });
  }

  resetPassword(id: number, password: string): Observable<ManagedUser> {
    return this.http.put<ManagedUser>(`${this.url}/${id}/password`, { password });
  }
}
