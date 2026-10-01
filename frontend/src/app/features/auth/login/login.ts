import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { apiErrorMessage } from '../../../core/utils/api-error.utils';
import { fieldErrorMessage } from '../../../shared/utils/form-errors';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule],
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
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    username: ['', [Validators.required, Validators.maxLength(150)]],
    password: ['', [Validators.required, Validators.maxLength(72)]],
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => void this.router.navigateByUrl(this.safeReturnUrl()),
      error: (error: unknown) => {
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
