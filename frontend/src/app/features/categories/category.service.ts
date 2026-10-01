import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Category, CategoryRequest } from './category.models';

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/categories`;

  list(): Observable<Category[]> {
    return this.http.get<Category[]>(this.url);
  }

  get(id: number): Observable<Category> {
    return this.http.get<Category>(`${this.url}/${id}`);
  }

  create(request: CategoryRequest): Observable<Category> {
    return this.http.post<Category>(this.url, request);
  }

  update(id: number, request: CategoryRequest): Observable<Category> {
    return this.http.put<Category>(`${this.url}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
