import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { catchError, finalize, of, switchMap, tap } from 'rxjs';

import { emptyPage } from '../../../core/models/page';
import { NotificationService } from '../../../core/services/notification.service';
import { CustomerBalance } from '../customer-credit.models';
import { CustomerCreditService } from '../customer-credit.service';

/** Encours par client, du plus gros au plus petit. */
@Component({
  selector: 'app-balance-list',
  imports: [CurrencyPipe, DatePipe, RouterLink, MatTableModule, MatPaginatorModule, MatButtonModule, MatIconModule, MatProgressBarModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './balance-list.html',
})
export class BalanceList {
  private readonly creditService = inject(CustomerCreditService);
  private readonly notifications = inject(NotificationService);

  protected readonly columns = ['party', 'openCredits', 'outstanding', 'overdue', 'nextDueDate'];
  protected readonly pageRequest = signal({ page: 0, size: 20 });
  protected readonly loading = signal(false);

  protected readonly page = toSignal(
    toObservable(this.pageRequest).pipe(
      tap(() => this.loading.set(true)),
      switchMap((request) =>
        this.creditService.balances(null, request).pipe(
          catchError((error: unknown) => {
            this.notifications.error(error);
            return of(emptyPage<CustomerBalance>());
          }),
          finalize(() => this.loading.set(false)),
        ),
      ),
    ),
    { initialValue: emptyPage<CustomerBalance>() },
  );

  protected onPage(event: PageEvent): void {
    this.pageRequest.set({ page: event.pageIndex, size: event.pageSize });
  }
}
