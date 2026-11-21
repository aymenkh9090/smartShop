import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';

import { AuthService } from '../../../core/services/auth.service';
import { SELLER } from '../../../core/testing/auth.fixtures';
import { Login } from './login';

describe('Login', () => {
  let fixture: ComponentFixture<Login>;
  let auth: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(() => {
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['login']);
    TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    });
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);
    fixture = TestBed.createComponent(Login);
    fixture.detectChanges();
  });

  function fillAndSubmit(): void {
    const el = fixture.nativeElement as HTMLElement;
    const username = el.querySelector<HTMLInputElement>('#username')!;
    const password = el.querySelector<HTMLInputElement>('#password')!;
    username.value = 'seller';
    username.dispatchEvent(new Event('input'));
    password.value = 'Secret123!';
    password.dispatchEvent(new Event('input'));
    el.querySelector<HTMLButtonElement>('button[type=submit]')!.click();
    fixture.detectChanges();
  }

  it('does not call the API when the form is empty', () => {
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button[type=submit]')!.click();
    expect(auth.login).not.toHaveBeenCalled();
  });

  it('logs in and navigates to a safe returnUrl', () => {
    auth.login.and.returnValue(of(SELLER));
    fixture.componentRef.setInput('returnUrl', '/sales');

    fillAndSubmit();

    expect(auth.login).toHaveBeenCalledWith({ username: 'seller', password: 'Secret123!' });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/sales');
  });

  it('ignores an external returnUrl', () => {
    auth.login.and.returnValue(of(SELLER));
    fixture.componentRef.setInput('returnUrl', '//evil.example');

    fillAndSubmit();

    expect(router.navigateByUrl).toHaveBeenCalledWith('/dashboard');
  });

  it('shows an error on invalid credentials', () => {
    auth.login.and.returnValue(throwError(() => new HttpErrorResponse({ status: 401 })));

    fillAndSubmit();

    const alert = (fixture.nativeElement as HTMLElement).querySelector('[role=alert]');
    expect(alert?.textContent).toContain('Identifiants invalides');
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('tells the user when the server is slow to answer (free hosting waking up)', fakeAsync(() => {
    const response = new Subject<typeof SELLER>();
    auth.login.and.returnValue(response);
    const status = () => (fixture.nativeElement as HTMLElement).querySelector('.server-slow');

    fillAndSubmit();
    tick(4000);
    fixture.detectChanges();
    expect(status()).toBeNull();

    tick(1000);
    fixture.detectChanges();
    expect(status()?.textContent).toContain('serveur de démonstration démarre');

    response.next(SELLER);
    response.complete();
    fixture.detectChanges();
    expect(status()).toBeNull();
  }));

  it('hides the demo account box when no demo account is configured', () => {
    expect((fixture.nativeElement as HTMLElement).querySelector('.demo')).toBeNull();
  });
});
