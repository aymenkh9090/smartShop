import { ChangeDetectionStrategy, Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterLink } from '@angular/router';

import { ROLES, ROLE_DESCRIPTIONS, ROLE_LABELS, Role } from '../../../core/models/auth.models';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { applyServerErrors, fieldErrorMessage } from '../../../shared/utils/form-errors';
import { PASSWORD_VALIDATORS, generatePassword } from '../password';
import { ROLE_ICONS } from '../role-icons';
import { UserService } from '../user.service';

/** Meme regle que le backend (UserCreateRequest). */
const USERNAME_PATTERN = /^\s*[a-zA-Z0-9._-]+\s*$/;

@Component({
  selector: 'app-user-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './user-form.html',
  styleUrl: './user-form.scss',
})
export class UserForm implements OnInit {
  private readonly userService = inject(UserService);
  private readonly notifications = inject(NotificationService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** Parametre de route :id (absent en creation). */
  readonly id = input<string>();
  protected readonly isEdit = computed(() => !!this.id());
  /** Un administrateur ne change pas son propre role (regle backend). */
  protected readonly isSelf = computed(() => Number(this.id()) === this.auth.currentUser()?.id);
  protected readonly submitting = signal(false);
  protected readonly passwordVisible = signal(false);
  protected readonly errorOf = fieldErrorMessage;

  protected readonly roles = ROLES;
  protected readonly roleLabels = ROLE_LABELS;
  protected readonly roleDescriptions = ROLE_DESCRIPTIONS;
  protected readonly roleIcons = ROLE_ICONS;

  protected readonly form = inject(FormBuilder).nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50),
      Validators.pattern(USERNAME_PATTERN)]],
    password: ['', PASSWORD_VALIDATORS],
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    role: ['SELLER' as Role, Validators.required],
  });

  ngOnInit(): void {
    const id = this.id();
    if (!id) {
      return;
    }
    // Identifiant non modifiable, mot de passe change via la liste (reinitialisation)
    this.form.controls.username.disable();
    this.form.controls.password.disable();
    this.userService.get(Number(id)).subscribe({
      next: (user) =>
        this.form.patchValue({
          username: user.username,
          firstName: user.firstName,
          lastName: user.lastName,
          email: user.email,
          role: user.role,
        }),
      error: (error: unknown) => this.notifications.error(error),
    });
  }

  protected selectRole(role: Role): void {
    if (!this.isSelf()) {
      this.form.controls.role.setValue(role);
    }
  }

  protected generate(): void {
    this.form.controls.password.setValue(generatePassword());
    this.passwordVisible.set(true);
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    const value = this.form.getRawValue();
    const id = this.id();
    const save$ = id
      ? this.userService.update(Number(id), {
          email: value.email,
          firstName: value.firstName,
          lastName: value.lastName,
          role: value.role,
        })
      : this.userService.create(value);
    save$.subscribe({
      next: () => {
        this.notifications.success(id ? 'Utilisateur modifié' : 'Utilisateur créé');
        void this.router.navigate(['/users']);
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        applyServerErrors(this.form, error);
        this.notifications.error(error);
      },
    });
  }
}
