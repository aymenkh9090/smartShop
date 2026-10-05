import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { User } from '../../../core/models/auth.models';
import { AuthService } from '../../../core/services/auth.service';
import { SELLER } from '../../../core/testing/auth.fixtures';
import { UserService } from '../user.service';
import { UserList } from './user-list';

const ADMIN: User = { ...SELLER, id: 1, username: 'admin', firstName: 'Admin', lastName: 'Shop', role: 'ADMIN' };

describe('UserList', () => {
  let users: jasmine.SpyObj<UserService>;

  function setup(): ComponentFixture<UserList> {
    users = jasmine.createSpyObj<UserService>('UserService', ['search', 'updateStatus', 'resetPassword']);
    users.search.and.returnValue(
      of({ content: [ADMIN, { ...SELLER, enabled: false }], page: 0, size: 20, totalElements: 2, totalPages: 1 }),
    );
    TestBed.configureTestingModule({
      imports: [UserList],
      providers: [
        provideRouter([]),
        { provide: UserService, useValue: users },
        { provide: AuthService, useValue: { currentUser: () => ADMIN } },
      ],
    });
    const fixture = TestBed.createComponent(UserList);
    fixture.detectChanges();
    return fixture;
  }

  it('lists all users sorted by last name on load', () => {
    const fixture = setup();
    const text = (fixture.nativeElement as HTMLElement).textContent;

    expect(users.search).toHaveBeenCalledWith(
      { q: null, role: null, enabled: null },
      jasmine.objectContaining({ page: 0, sort: 'lastName,asc' }),
    );
    expect(text).toContain('Sami Ben Ali');
    expect(text).toContain('@seller');
    expect(text).toContain('Vendeur');
    expect(text).toContain('Désactivé');
  });

  it('marks the current user', () => {
    const fixture = setup();
    const rows = (fixture.nativeElement as HTMLElement).querySelectorAll('tr.mat-mdc-row');

    expect(rows[0].textContent).toContain('Vous');
    expect(rows[1].textContent).not.toContain('Vous');
  });
});
