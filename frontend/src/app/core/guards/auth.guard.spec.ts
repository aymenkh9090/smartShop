import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';

import { Role } from '../models/auth.models';
import { AuthService } from '../services/auth.service';
import { authGuard, guestGuard } from './auth.guard';

describe('auth guards', () => {
  let authenticated: boolean;
  let roleAllowed: boolean;

  beforeEach(() => {
    authenticated = false;
    roleAllowed = true;
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: { isAuthenticated: () => authenticated, hasAnyRole: () => roleAllowed },
        },
      ],
    });
  });

  function runAuthGuard(roles?: Role[]) {
    const route = { data: roles ? { roles } : {} } as unknown as ActivatedRouteSnapshot;
    const state = { url: '/sales' } as RouterStateSnapshot;
    return TestBed.runInInjectionContext(() => authGuard(route, state));
  }

  function serialize(result: unknown): string {
    return TestBed.inject(Router).serializeUrl(result as UrlTree);
  }

  it('redirects anonymous users to /login with returnUrl', () => {
    expect(serialize(runAuthGuard())).toBe('/login?returnUrl=%2Fsales');
  });

  it('lets authenticated users through', () => {
    authenticated = true;
    expect(runAuthGuard()).toBeTrue();
  });

  it('redirects users without the required role to the dashboard', () => {
    authenticated = true;
    roleAllowed = false;
    expect(serialize(runAuthGuard(['ADMIN']))).toBe('/dashboard');
  });

  it('guestGuard sends authenticated users to the dashboard', () => {
    authenticated = true;
    const result = TestBed.runInInjectionContext(() =>
      guestGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );
    expect(serialize(result)).toBe('/dashboard');
  });
});
