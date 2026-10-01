import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { filter, switchMap } from 'rxjs';

import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { confirmAction } from '../../../shared/components/confirm-dialog/confirm-dialog';
import { Category } from '../category.models';
import { CategoryService } from '../category.service';

@Component({
  selector: 'app-category-list',
  imports: [RouterLink, MatTableModule, MatButtonModule, MatIconModule, MatProgressBarModule, MatTooltipModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './category-list.html',
})
export class CategoryList {
  private readonly categoryService = inject(CategoryService);
  private readonly notifications = inject(NotificationService);
  private readonly dialog = inject(MatDialog);
  private readonly auth = inject(AuthService);

  protected readonly canManage = computed(() => this.auth.hasAnyRole(['MANAGER']));
  protected readonly columns = computed(() =>
    this.canManage() ? ['name', 'description', 'actions'] : ['name', 'description'],
  );
  protected readonly categories = signal<Category[]>([]);
  protected readonly loading = signal(true);

  constructor() {
    this.load();
  }

  protected remove(category: Category): void {
    confirmAction(this.dialog, {
      title: 'Supprimer la categorie',
      message: `Supprimer la categorie "${category.name}" ?`,
      confirmLabel: 'Supprimer',
    })
      .pipe(
        filter(Boolean),
        switchMap(() => this.categoryService.delete(category.id)),
      )
      .subscribe({
        next: () => {
          this.notifications.success('Categorie supprimee');
          this.load();
        },
        error: (error: unknown) => this.notifications.error(error),
      });
  }

  private load(): void {
    this.loading.set(true);
    this.categoryService.list().subscribe({
      next: (categories) => {
        this.categories.set(categories);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.notifications.error(error);
      },
    });
  }
}
