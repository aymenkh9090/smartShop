import { ChangeDetectionStrategy, Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';

import { NotificationService } from '../../../core/services/notification.service';
import { applyServerErrors, fieldErrorMessage } from '../../../shared/utils/form-errors';
import { CategoryService } from '../category.service';

@Component({
  selector: 'app-category-form',
  imports: [ReactiveFormsModule, RouterLink, MatFormFieldModule, MatInputModule, MatButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './category-form.html',
})
export class CategoryForm implements OnInit {
  private readonly categoryService = inject(CategoryService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);

  /** Parametre de route :id (absent en creation). */
  readonly id = input<string>();
  protected readonly isEdit = computed(() => !!this.id());
  protected readonly submitting = signal(false);
  protected readonly errorOf = fieldErrorMessage;

  protected readonly form = inject(FormBuilder).nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    description: ['', Validators.maxLength(500)],
  });

  ngOnInit(): void {
    const id = this.id();
    if (id) {
      this.categoryService.get(Number(id)).subscribe({
        next: (category) => this.form.patchValue({ ...category, description: category.description ?? '' }),
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
    const save$ = id ? this.categoryService.update(Number(id), request) : this.categoryService.create(request);
    save$.subscribe({
      next: () => {
        this.notifications.success(id ? 'Catégorie modifiée' : 'Catégorie créée');
        void this.router.navigate(['/categories']);
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        applyServerErrors(this.form, error);
        this.notifications.error(error);
      },
    });
  }
}
