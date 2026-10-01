import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { environment } from '../../../environments/environment';
import { SELLER, authResponse } from '../testing/auth.fixtures';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  const authUrl = `${environment.apiUrl}/auth`;
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('login stores the tokens and exposes the current user', () => {
    service.login({ username: 'seller', password: 'Secret123!' }).subscribe();
    http.expectOne(`${authUrl}/login`).flush(authResponse('1'));

    expect(service.isAuthenticated()).toBeTrue();
    expect(service.currentUser()?.username).toBe('seller');
    expect(service.accessToken()).toBe('access-1');
    expect(service.hasRefreshToken()).toBeTrue();
  });

  it('logout revokes the refresh token, clears the session and goes to /login', () => {
    const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    service.login({ username: 'seller', password: 'x' }).subscribe();
    http.expectOne(`${authUrl}/login`).flush(authResponse('1'));

    service.logout();

    const req = http.expectOne(`${authUrl}/logout`);
    expect(req.request.body).toEqual({ refreshToken: 'refresh-1' });
    req.flush(null);
    expect(service.isAuthenticated()).toBeFalse();
    expect(service.accessToken()).toBeNull();
    expect(navigate).toHaveBeenCalledWith(['/login']);
  });

  it('concurrent refresh calls share a single request', () => {
    localStorage.setItem('smartshop.refreshToken', 'refresh-0');
    const tokens: string[] = [];

    service.refreshAccessToken().subscribe((t) => tokens.push(t));
    service.refreshAccessToken().subscribe((t) => tokens.push(t));
    http.expectOne(`${authUrl}/refresh`).flush(authResponse('2'));

    expect(tokens).toEqual(['access-2', 'access-2']);
    expect(service.accessToken()).toBe('access-2');
  });

  it('restoreSession without stored tokens does not call the API', () => {
    service.restoreSession().subscribe();
    http.expectNone(`${authUrl}/me`);
    expect(service.isAuthenticated()).toBeFalse();
  });

  it('restoreSession loads the user, and clears the session on failure', () => {
    localStorage.setItem('smartshop.accessToken', 'access-0');
    service.restoreSession().subscribe();
    http.expectOne(`${authUrl}/me`).flush(SELLER);
    expect(service.currentUser()?.username).toBe('seller');

    service.restoreSession().subscribe();
    http.expectOne(`${authUrl}/me`).flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(service.isAuthenticated()).toBeFalse();
    expect(service.accessToken()).toBeNull();
  });

  it('hasAnyRole follows the role hierarchy', () => {
    service.login({ username: 'admin', password: 'x' }).subscribe();
    http.expectOne(`${authUrl}/login`).flush(authResponse('1', { ...SELLER, role: 'MANAGER' }));

    expect(service.hasAnyRole(['SELLER'])).toBeTrue();
    expect(service.hasAnyRole(['TECHNICIAN'])).toBeTrue();
    expect(service.hasAnyRole(['ADMIN'])).toBeFalse();
  });
});
