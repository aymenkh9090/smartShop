import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, map, of, shareReplay, tap, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthResponse, LoginRequest, ROLE_HIERARCHY, Role, User } from '../models/auth.models';

const ACCESS_TOKEN_KEY = 'smartshop.accessToken';
const REFRESH_TOKEN_KEY = 'smartshop.refreshToken';

/**
 * Session utilisateur : tokens (localStorage) et utilisateur courant (signal).
 * L'autorisation reelle reste controlee par le backend.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly authUrl = `${environment.apiUrl}/auth`;

  private readonly user = signal<User | null>(null);
  private refreshInFlight$: Observable<string> | null = null;

  readonly currentUser = this.user.asReadonly();
  readonly isAuthenticated = computed(() => this.user() !== null);

  login(credentials: LoginRequest): Observable<User> {
    return this.http.post<AuthResponse>(`${this.authUrl}/login`, credentials).pipe(
      tap((response) => this.storeSession(response)),
      map((response) => response.user),
    );
  }

  /** Revoque le refresh token cote serveur (best effort), vide la session et renvoie vers /login. */
  logout(): void {
    const refreshToken = this.refreshToken();
    this.clearSession();
    if (refreshToken) {
      this.http.post<void>(`${this.authUrl}/logout`, { refreshToken }).subscribe({ error: () => {} });
    }
    void this.router.navigate(['/login']);
  }

  /** Restaure la session au demarrage de l'application a partir des tokens stockes. */
  restoreSession(): Observable<void> {
    if (!this.accessToken() && !this.refreshToken()) {
      return of(undefined);
    }
    return this.http.get<User>(`${this.authUrl}/me`).pipe(
      tap((user) => this.user.set(user)),
      map(() => undefined),
      catchError(() => {
        this.clearSession();
        return of(undefined);
      }),
    );
  }

  /**
   * Obtient un nouvel access token. Les appels simultanes partagent la meme requete :
   * le refresh token etant a usage unique, deux requetes paralleles revoqueraient la session.
   */
  refreshAccessToken(): Observable<string> {
    const refreshToken = this.refreshToken();
    if (!refreshToken) {
      return throwError(() => new Error('Aucun refresh token'));
    }
    if (!this.refreshInFlight$) {
      this.refreshInFlight$ = this.http
        .post<AuthResponse>(`${this.authUrl}/refresh`, { refreshToken })
        .pipe(
          tap((response) => this.storeSession(response)),
          map((response) => response.accessToken),
          finalize(() => (this.refreshInFlight$ = null)),
          shareReplay(1),
        );
    }
    return this.refreshInFlight$;
  }

  hasAnyRole(roles: readonly Role[]): boolean {
    const user = this.user();
    return !!user && roles.some((role) => ROLE_HIERARCHY[user.role].includes(role));
  }

  accessToken(): string | null {
    return localStorage.getItem(ACCESS_TOKEN_KEY);
  }

  hasRefreshToken(): boolean {
    return this.refreshToken() !== null;
  }

  private refreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  }

  private storeSession(response: AuthResponse): void {
    localStorage.setItem(ACCESS_TOKEN_KEY, response.accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken);
    this.user.set(response.user);
  }

  private clearSession(): void {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    this.user.set(null);
  }
}
