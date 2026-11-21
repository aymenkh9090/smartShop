import { CurrencyPipe, DatePipe, PercentPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';
import { catchError, map, of, startWith } from 'rxjs';

import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { AFTER_SALES_STATUS_LABELS, AfterSalesStatus } from '../after-sales/after-sales.models';
import { Dashboard as DashboardData } from './dashboard.models';
import { DashboardService } from './dashboard.service';
import { RevenueChart } from './revenue-chart/revenue-chart';

/** Statuts SAV ouverts, dans l'ordre du cycle de vie. */
const OPEN_STATUSES: AfterSalesStatus[] = ['RECEIVED', 'DIAGNOSING', 'WAITING_FOR_PART', 'IN_REPAIR', 'READY'];

type LoadState = { status: 'loading' } | { status: 'error' } | { status: 'ready'; data: DashboardData };

/**
 * Tableau de bord. Le backend ne renvoie que les sections autorisees pour le role :
 * le template affiche simplement les sections presentes.
 */
@Component({
  selector: 'app-dashboard',
  imports: [CurrencyPipe, DatePipe, PercentPipe, RouterLink, MatButtonModule, MatIconModule, MatProgressBarModule, RevenueChart],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  private readonly notifications = inject(NotificationService);
  private readonly auth = inject(AuthService);

  protected readonly openStatuses = OPEN_STATUSES;
  protected readonly statusLabels = AFTER_SALES_STATUS_LABELS;

  protected readonly state = toSignal(
    inject(DashboardService)
      .get()
      .pipe(
        map((data): LoadState => ({ status: 'ready', data })),
        startWith<LoadState>({ status: 'loading' }),
        catchError((error: unknown) => {
          this.notifications.error(error);
          return of<LoadState>({ status: 'error' });
        }),
      ),
    { initialValue: { status: 'loading' } as LoadState },
  );

  protected readonly data = computed(() => {
    const state = this.state();
    return state.status === 'ready' ? state.data : null;
  });

  /** Accueil personnalise : "Bonjour" en journee, "Bonsoir" a partir de 18h. */
  protected readonly greeting = computed(() => {
    const salutation = new Date().getHours() >= 18 ? 'Bonsoir' : 'Bonjour';
    const firstName = this.auth.currentUser()?.firstName;
    return firstName ? `${salutation}, ${firstName}` : salutation;
  });

  /** Evolution du CA du mois par rapport a la meme periode du mois precedent (null si pas de base). */
  protected readonly monthTrend = computed(() => {
    const month = this.data()?.monthSales;
    if (!month || month.previousAmount <= 0) {
      return null;
    }
    return (month.amount - month.previousAmount) / month.previousAmount;
  });

  protected statusCount(byStatus: Partial<Record<AfterSalesStatus, number>>, status: AfterSalesStatus): number {
    return byStatus[status] ?? 0;
  }
}
