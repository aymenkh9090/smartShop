import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';

import { environment } from '../../../../environments/environment';
import { AuthService } from '../../../core/services/auth.service';
import { apiErrorMessage } from '../../../core/utils/api-error.utils';
import { fieldErrorMessage } from '../../../shared/utils/form-errors';

/** Delai au-dela duquel on previent que le serveur (offre gratuite, en veille) demarre. */
const SLOW_SERVER_DELAY_MS = 5000;

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** Lu depuis le query param ?returnUrl= (withComponentInputBinding). */
  readonly returnUrl = input<string>();

  protected readonly errorOf = fieldErrorMessage;
  protected readonly submitting = signal(false);
  protected readonly showPassword = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly serverSlow = signal(false);
  protected readonly demoAccount = environment.demoAccount;
  private slowTimer: ReturnType<typeof setTimeout> | undefined;

  protected readonly form = inject(FormBuilder).nonNullable.group({
    username: ['', [Validators.required, Validators.maxLength(150)]],
    password: ['', [Validators.required, Validators.maxLength(72)]],
  });

  constructor() {
    // API sur un autre domaine (hebergement gratuit qui se met en veille) : on la reveille
    // des l'ouverture de la page, pendant que l'utilisateur saisit ses identifiants.
    if (/^https?:\/\//.test(environment.apiUrl)) {
      fetch(`${environment.apiUrl}/auth/me`).catch(() => undefined);
    }
    inject(DestroyRef).onDestroy(() => this.watchSlowServer(false));
  }

  /** Affiche un message si la requete depasse SLOW_SERVER_DELAY_MS (false : arrete et masque). */
  private watchSlowServer(active: boolean): void {
    clearTimeout(this.slowTimer);
    this.serverSlow.set(false);
    if (active) {
      this.slowTimer = setTimeout(() => this.serverSlow.set(true), SLOW_SERVER_DELAY_MS);
    }
  }

  protected fillDemo(): void {
    if (this.demoAccount) {
      this.form.setValue({ ...this.demoAccount });
      this.errorMessage.set(null);
    }
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);
    this.watchSlowServer(true);
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
        this.watchSlowServer(false);
        void this.router.navigateByUrl(this.safeReturnUrl());
      },
      error: (error: unknown) => {
        this.watchSlowServer(false);
        this.submitting.set(false);
        this.errorMessage.set(this.toMessage(error));
      },
    });
  }

  /** N'accepte qu'un chemin interne, pour eviter les redirections ouvertes. */
  private safeReturnUrl(): string {
    const url = this.returnUrl();
    return url && url.startsWith('/') && !url.startsWith('//') ? url : '/dashboard';
  }

  private toMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 401) {
      return 'Identifiants invalides.';
    }
    return apiErrorMessage(error);
  }
}
