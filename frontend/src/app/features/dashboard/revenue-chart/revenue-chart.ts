import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { BarController, BarElement, CategoryScale, ChartConfiguration, LinearScale, Tooltip } from 'chart.js';
import { BaseChartDirective, provideCharts } from 'ng2-charts';

import { fromIsoDate } from '../../../shared/components/reschedule-dialog/reschedule-dialog';
import { DailyAmount } from '../dashboard.models';

/**
 * Palette de reference dataviz (theme clair uniquement, comme l'application) :
 * serie unique = slot 1, texte et grille en encres neutres. Validee avec scripts/validate_palette.js.
 */
const SERIES_COLOR = '#2a78d6';
const TEXT_SECONDARY = '#52514e';
const GRID_COLOR = '#e4e3df';

const MONEY = new Intl.NumberFormat('fr', { style: 'currency', currency: 'TND', minimumFractionDigits: 3 });
const MONEY_AXIS = new Intl.NumberFormat('fr', { notation: 'compact', maximumFractionDigits: 1 });
const DAY = new Intl.DateTimeFormat('fr', { day: '2-digit', month: '2-digit' });
const LONG_DAY = new Intl.DateTimeFormat('fr', { weekday: 'long', day: 'numeric', month: 'long' });

/**
 * CA des ventes par jour : colonnes d'une seule serie (pas de legende, le titre la nomme),
 * infobulle au survol et tableau des valeurs pour l'accessibilite.
 */
@Component({
  selector: 'app-revenue-chart',
  imports: [BaseChartDirective, CurrencyPipe, DatePipe],
  // Enregistrement limite aux elements utilises (bundle plus leger que withDefaultRegisterables)
  providers: [provideCharts({ registerables: [BarController, BarElement, CategoryScale, LinearScale, Tooltip] })],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="chart" role="img" [attr.aria-label]="summary()">
      <canvas baseChart type="bar" [data]="data()" [options]="options"></canvas>
    </div>
    <details>
      <summary>Voir les valeurs</summary>
      <table class="detail-table">
        <thead><tr><th>Jour</th><th class="amount">CA</th></tr></thead>
        <tbody>
          @for (day of days(); track day.date) {
            <tr>
              <td>{{ day.date | date: 'EEEE dd/MM' }}</td>
              <td class="amount">{{ day.amount | currency: 'TND' : 'symbol' : '1.3-3' }}</td>
            </tr>
          }
        </tbody>
      </table>
    </details>
  `,
  styles: `
    .chart {
      position: relative;
      height: 260px;
    }

    details {
      margin-top: 0.5rem;
      color: var(--mat-sys-on-surface-variant);
    }

    .detail-table {
      width: 100%;
      max-width: 420px;
      border-collapse: collapse;

      td,
      th {
        padding: 0.25rem 0.5rem;
        text-align: left;
        border-bottom: 1px solid var(--mat-sys-outline-variant);
      }

      .amount {
        text-align: right;
      }
    }
  `,
})
export class RevenueChart {
  readonly days = input.required<DailyAmount[]>();

  protected readonly data = computed<ChartConfiguration<'bar'>['data']>(() => ({
    labels: this.days().map((day) => DAY.format(fromIsoDate(day.date))),
    datasets: [
      {
        data: this.days().map((day) => day.amount),
        backgroundColor: SERIES_COLOR,
        hoverBackgroundColor: SERIES_COLOR,
        // Colonnes fines, extremite arrondie de 4px, base carree
        maxBarThickness: 24,
        borderRadius: { topLeft: 4, topRight: 4 },
        borderSkipped: 'start',
        categoryPercentage: 0.8,
        barPercentage: 0.9,
      },
    ],
  }));

  protected readonly summary = computed(() => {
    const days = this.days();
    const total = days.reduce((sum, day) => sum + day.amount, 0);
    return `CA des ventes sur ${days.length} jours : ${MONEY.format(total)}. Valeurs detaillees dans le tableau.`;
  });

  protected readonly options: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    animation: false,
    interaction: { mode: 'index', intersect: false },
    scales: {
      x: {
        grid: { display: false },
        border: { color: GRID_COLOR },
        ticks: { color: TEXT_SECONDARY, autoSkip: true, maxRotation: 0, autoSkipPadding: 12 },
      },
      y: {
        beginAtZero: true,
        grid: { color: GRID_COLOR },
        border: { display: false },
        ticks: { color: TEXT_SECONDARY, maxTicksLimit: 5, callback: (value) => MONEY_AXIS.format(Number(value)) },
      },
    },
    plugins: {
      tooltip: {
        displayColors: false,
        callbacks: {
          title: (items) => {
            const day = this.days()[items[0]?.dataIndex ?? 0];
            return day ? LONG_DAY.format(fromIsoDate(day.date)) : '';
          },
          label: (item) => MONEY.format(Number(item.raw)),
        },
      },
    },
  };
}
