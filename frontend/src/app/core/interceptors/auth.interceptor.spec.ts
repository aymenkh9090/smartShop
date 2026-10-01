import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { environment } from '../../../environments/environment';
import { authResponse } from '../testing/auth.fixtures';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  const api = environment.apiUrl;
  let client: HttpClient;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('smartshop.accessToken', 'access-old');
    localStorage.setItem('smartshop.refreshToken', 'refresh-old');
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    client = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('adds the bearer token to API calls', () => {
    client.get(`${api}/products`).subscribe();
    expect(http.expectOne(`${api}/products`).request.headers.get('Authorization')).toBe('Bearer access-old');
  });

  it('does not add the token to login or external calls', () => {
    client.post(`${api}/auth/login`, {}).subscribe();
    client.get('https://example.com/data').subscribe();

    expect(http.expectOne(`${api}/auth/login`).request.headers.has('Authorization')).toBeFalse();
    expect(http.expectOne('https://example.com/data').request.headers.has('Authorization')).toBeFalse();
  });

  it('refreshes the token on 401 and replays the request', () => {
    let body: unknown;
    client.get(`${api}/products`).subscribe((b) => (body = b));

    http.expectOne(`${api}/products`).flush(null, { status: 401, statusText: 'Unauthorized' });
    http.expectOne(`${api}/auth/refresh`).flush(authResponse('new'));
    const retry = http.expectOne(`${api}/products`);
    expect(retry.request.headers.get('Authorization')).toBe('Bearer access-new');
    retry.flush(['ok']);

    expect(body).toEqual(['ok']);
  });

  it('ends the session when the refresh fails', () => {
    let status: number | undefined;
    client.get(`${api}/products`).subscribe({ error: (e) => (status = e.status) });

    http.expectOne(`${api}/products`).flush(null, { status: 401, statusText: 'Unauthorized' });
    http.expectOne(`${api}/auth/refresh`).flush(null, { status: 401, statusText: 'Unauthorized' });
    http.expectOne(`${api}/auth/logout`).flush(null);

    expect(status).toBe(401);
    expect(localStorage.getItem('smartshop.accessToken')).toBeNull();
    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/login']);
  });

  it('does not try to refresh on 403', () => {
    let status: number | undefined;
    client.get(`${api}/products`).subscribe({ error: (e) => (status = e.status) });

    http.expectOne(`${api}/products`).flush(null, { status: 403, statusText: 'Forbidden' });

    expect(status).toBe(403);
    http.expectNone(`${api}/auth/refresh`);
  });
});
