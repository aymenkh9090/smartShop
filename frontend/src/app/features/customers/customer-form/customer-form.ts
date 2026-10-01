import { ChangeDetectionStrategy, Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';

import { NotificationService } from '../../../core/services/notification.service';
import { applyServerErrors, fieldErrorMessage } from '../../../shared/utils/form-errors';
import { CustomerService } from '../customer.service';

/** Meme regle que le backend : chiffres et espaces, "+" initial facultatif. */
const PHONE_PATTERN = /^\s*\+?[0-9 ]{8,20}$/;

@Component({
  selector: 'app-customer-form',
  imports: [ReactiveFormsModule, RouterLink, MatFormFieldModule, MatInputModule, MatButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './customer-form.html',
})
export class CustomerForm implements OnInit {
  private readonly customerService = inject(CustomerService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);

  /** Parametre de route :id (absent en creation). */
  readonly id = input<string>();
  protected readonly isEdit = computed(() => !!this.id());
  protected readonly submitting = signal(false);
  protected readonly errorOf = fieldErrorMessage;

  protected readonly form = inject(FormBuilder).nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    phone: ['', [Validators.required, Validators.pattern(PHONE_PATTERN)]],
    email: ['', [Validators.email, Validators.maxLength(150)]],
    address: ['', Validators.maxLength(255)],
    cin: ['', Validators.pattern(/^\s*[A-Za-z0-9]{6,20}\s*$/)],
    notes: ['', Validators.maxLength(1000)],
  });

  ngOnInit(): void {
    const id = this.id();
    if (id) {
      this.customerService.get(Number(id)).subscribe({
        next: (customer) =>
          this.form.patchValue({
            firstName: customer.firstName,
            lastName: customer.lastName,
            phone: customer.phone,
            email: customer.email ?? '',
            address: customer.address ?? '',
            cin: customer.cin ?? '',
            notes: customer.notes ?? '',
          }),
        error: (error: unknown) => this.notifications.error(error),
      });
    }
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    const request = this.form.getRawValue();
    const id = this.id();
    const save$ = id ? this.customerService.update(Number(id), request) : this.customerService.create(request);
    save$.subscribe({
      next: () => {
        this.notifications.success(id ? 'Client modifie' : 'Client cree');
        void this.router.navigate(['/customers']);
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        applyServerErrors(this.form, error);
        this.notifications.error(error);
      },
    });
  }
}
