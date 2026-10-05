import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { User } from '../../../core/models/auth.models';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { SELLER } from '../../../core/testing/auth.fixtures';
import { UserService } from '../user.service';
import { UserForm } from './user-form';

const ADMIN: User = { ...SELLER, id: 1, username: 'admin', role: 'ADMIN' };

type Internals = { form: UserForm['form']; submit(): void; selectRole(role: string): void };

describe('UserForm', () => {
  let users: jasmine.SpyObj<UserService>;

  function setup(id?: string): ComponentFixture<UserForm> {
    users = jasmine.createSpyObj<UserService>('UserService', ['get', 'create', 'update']);
    users.get.and.returnValue(of(id === '1' ? ADMIN : SELLER));
    users.create.and.returnValue(of(SELLER));
    users.update.and.returnValue(of(SELLER));
    TestBed.configureTestingModule({
      imports: [UserForm],
      providers: [
        provideRouter([]),
        { provide: UserService, useValue: users },
        { provide: AuthService, useValue: { currentUser: () => ADMIN } },
        { provide: NotificationService, useValue: jasmine.createSpyObj('NotificationService', ['success', 'error']) },
      ],
    });
    const fixture = TestBed.createComponent(UserForm);
    if (id) {
      fixture.componentRef.setInput('id', id);
    }
    fixture.detectChanges();
    spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    return fixture;
  }

  it('creates a user with username, password and chosen role', () => {
    const component = setup().componentInstance as unknown as Internals;

    component.form.patchValue({
      username: 'karim',
      password: 'Secret123!',
      firstName: 'Karim',
      lastName: 'Trabelsi',
      email: 'karim@shop.tn',
    });
    component.selectRole('TECHNICIAN');
    component.submit();

    expect(users.create).toHaveBeenCalledWith({
      username: 'karim',
      password: 'Secret123!',
      firstName: 'Karim',
      lastName: 'Trabelsi',
      email: 'karim@shop.tn',
      role: 'TECHNICIAN',
    });
  });

  it('does not submit a too short password', () => {
    const component = setup().componentInstance as unknown as Internals;

    component.form.patchValue({ username: 'karim', password: 'short', firstName: 'K', lastName: 'T', email: 'k@shop.tn' });
    component.submit();

    expect(users.create).not.toHaveBeenCalled();
  });

  it('edits without username nor password', () => {
    const component = setup('2').componentInstance as unknown as Internals;

    expect(component.form.controls.username.disabled).toBeTrue();
    component.selectRole('MANAGER');
    component.submit();

    expect(users.update).toHaveBeenCalledWith(2, {
      email: SELLER.email,
      firstName: SELLER.firstName,
      lastName: SELLER.lastName,
      role: 'MANAGER',
    });
  });

  it('keeps the role of the current admin locked', () => {
    const fixture = setup('1');
    const component = fixture.componentInstance as unknown as Internals;

    component.selectRole('SELLER');

    expect(component.form.controls.role.value).toBe('ADMIN');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Vous ne pouvez pas modifier votre propre rôle');
  });
});
