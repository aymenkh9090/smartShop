import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { catchError, debounceTime, filter, finalize, of, switchMap, tap } from 'rxjs';

import { ROLES, ROLE_LABELS, Role } from '../../../core/models/auth.models';
import { PageRequest, emptyPage } from '../../../core/models/page';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { confirmAction } from '../../../shared/components/confirm-dialog/confirm-dialog';
import { askNewPassword } from '../password-dialog/password-dialog';
import { ROLE_ICONS } from '../role-icons';
import { ManagedUser, UserFilter } from '../user.models';
import { UserService } from '../user.service';

const DEFAULT_SORT = 'lastName,asc';

interface UserQuery extends PageRequest {
  filter: UserFilter;
}

@Component({
  selector: 'app-user-list',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatProgressBarModule,
    MatTooltipModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './user-list.html',
  styleUrl: './user-list.scss',
})
export class UserList {
  private readonly userService = inject(UserService);
  private readonly notifications = inject(NotificationService);
  private readonly dialog = inject(MatDialog);
  private readonly auth = inject(AuthService);

  protected readonly roles = ROLES;
  protected readonly roleLabels = ROLE_LABELS;
  protected readonly roleIcons = ROLE_ICONS;
  protected readonly columns = ['name', 'email', 'role', 'status', 'createdAt', 'actions'];

  protected readonly filters = new FormGroup({
    q: new FormControl('', { nonNullable: true }),
    role: new FormControl<Role | ''>('', { nonNullable: true }),
    /** '' = tous, 'true' = actifs, 'false' = desactives */
    enabled: new FormControl<'' | 'true' | 'false'>('', { nonNullable: true }),
  });

  protected readonly query = signal<UserQuery>({
    filter: this.toFilter(),
    page: 0,
    size: 20,
    sort: DEFAULT_SORT,
  });
  protected readonly loading = signal(false);

  protected readonly page = toSignal(
    toObservable(this.query).pipe(
      tap(() => this.loading.set(true)),
      switchMap((query) =>
        this.userService.search(query.filter, query).pipe(
          catchError((error: unknown) => {
            this.notifications.error(error);
            return of(emptyPage<ManagedUser>());
          }),
          finalize(() => this.loading.set(false)),
        ),
      ),
    ),
    { initialValue: emptyPage<ManagedUser>() },
  );

  constructor() {
    this.filters.valueChanges
      .pipe(debounceTime(300), takeUntilDestroyed())
      .subscribe(() => this.query.update((query) => ({ ...query, filter: this.toFilter(), page: 0 })));
  }

  protected isSelf(user: ManagedUser): boolean {
    return user.id === this.auth.currentUser()?.id;
  }

  protected initials(user: ManagedUser): string {
    return `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase();
  }

  protected roleLabel(role: Role): string {
    return ROLE_LABELS[role];
  }

  protected roleIcon(role: Role): string {
    return ROLE_ICONS[role];
  }

  protected onPage(event: PageEvent): void {
    this.query.update((query) => ({ ...query, page: event.pageIndex, size: event.pageSize }));
  }

  protected onSort(sort: Sort): void {
    const value = sort.direction ? `${sort.active},${sort.direction}` : DEFAULT_SORT;
    this.query.update((query) => ({ ...query, sort: value, page: 0 }));
  }

  protected resetPassword(user: ManagedUser): void {
    askNewPassword(this.dialog, { name: `${user.firstName} ${user.lastName}` })
      .pipe(
        filter((password): password is string => !!password),
        switchMap((password) => this.userService.resetPassword(user.id, password)),
      )
      .subscribe({
        next: () => this.notifications.success('Mot de passe réinitialisé'),
        error: (error: unknown) => this.notifications.error(error),
      });
  }

  protected toggleStatus(user: ManagedUser): void {
    const activate = !user.enabled;
    const name = `${user.firstName} ${user.lastName}`;
    confirmAction(this.dialog, {
      title: activate ? 'Réactiver le compte' : 'Désactiver le compte',
      message: activate
        ? `${name} pourra de nouveau se connecter.`
        : `${name} ne pourra plus se connecter et ses sessions seront fermées. L'historique est conservé.`,
      confirmLabel: activate ? 'Réactiver' : 'Désactiver',
    })
      .pipe(
        filter(Boolean),
        switchMap(() => this.userService.updateStatus(user.id, activate)),
      )
      .subscribe({
        next: () => {
          this.notifications.success(activate ? 'Compte réactivé' : 'Compte désactivé');
          this.query.update((query) => ({ ...query }));
        },
        error: (error: unknown) => this.notifications.error(error),
      });
  }

  private toFilter(): UserFilter {
    const value = this.filters.getRawValue();
    return {
      q: value.q.trim() || null,
      role: value.role || null,
      enabled: value.enabled === '' ? null : value.enabled === 'true',
    };
  }
}
