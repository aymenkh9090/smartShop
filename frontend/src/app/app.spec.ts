import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { App } from './app';
import { ROLE_HIERARCHY, Role, User } from './core/models/auth.models';
import { AuthService } from './core/services/auth.service';
import { SELLER } from './core/testing/auth.fixtures';

describe('App', () => {
  function render(user: User | null): HTMLElement {
    const currentUser = signal(user);
    TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            currentUser,
            hasAnyRole: (roles: Role[]) => !!user && roles.some((r) => ROLE_HIERARCHY[user.role].includes(r)),
            logout: jasmine.createSpy('logout'),
          },
        },
      ],
    });
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('shows no shell when logged out', () => {
    const el = render(null);
    expect(el.querySelector('mat-toolbar')).toBeNull();
    expect(el.querySelector('mat-nav-list')).toBeNull();
  });

  it('shows the shell with the user name when logged in', () => {
    const el = render(SELLER);
    expect(el.querySelector('mat-toolbar')?.textContent).toContain('SmartShop');
    expect(el.querySelector('mat-toolbar')?.textContent).toContain('Sami Ben Ali');
  });

  it('hides the suppliers menu from sellers', () => {
    const el = render(SELLER);
    const menu = el.querySelector('mat-nav-list')?.textContent ?? '';
    expect(menu).toContain('Produits');
    expect(menu).toContain('Clients');
    expect(menu).not.toContain('Fournisseurs');
  });

  it('shows the suppliers menu to managers', () => {
    const el = render({ ...SELLER, role: 'MANAGER' });
    expect(el.querySelector('mat-nav-list')?.textContent).toContain('Fournisseurs');
  });
});
