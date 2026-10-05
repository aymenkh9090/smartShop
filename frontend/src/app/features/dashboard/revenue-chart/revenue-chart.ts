import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { BarController, BarElement, CategoryScale, ChartConfiguration, LinearScale, Tooltip } from 'chart.js';
import { BaseChartDirective, provideCharts } from 'ng2-charts';

import { ThemeService } from '../../../core/services/theme.service';
import { fromIsoDate } from '../../../shared/components/reschedule-dialog/reschedule-dialog';
import { DailyAmount } from '../dashboard.models';

/**
 * Couleurs du graphique par theme : serie unique = indigo de la marque (clair #4f46e5, sombre #8582ff),
 * validees avec le script dataviz (bande de luminance, chroma, contraste >= 3:1 sur la surface).
 * Texte et grille en encres neutres, alignees sur les jetons --app-text-muted / --app-border.
 */
const CHART_COLORS = {
  light: { series: '#4f46e5', text: '#5b6275', grid: '#e6e8ef' },
  dark: { series: '#8582ff', text: '#a3a9b8', grid: '#2b2f3a' },
};

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
      <canvas baseChart type="bar" [data]="data()" [options]="options()"></canvas>
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
      margin-top: 0.75rem;
      color: var(--app-text-muted);

      summary {
        cursor: pointer;
      }
    }

    .detail-table {
      max-width: 420px;

      .amount {
        text-align: right;
      }
    }
  `,
})
export class RevenueChart {
  readonly days = input.required<DailyAmount[]>();

  private readonly theme = inject(ThemeService);
  private readonly colors = computed(() => (this.theme.isDark() ? CHART_COLORS.dark : CHART_COLORS.light));

  protected readonly data = computed<ChartConfiguration<'bar'>['data']>(() => ({
    labels: this.days().map((day) => DAY.format(fromIsoDate(day.date))),
    datasets: [
      {
        data: this.days().map((day) => day.amount),
        backgroundColor: this.colors().series,
        hoverBackgroundColor: this.colors().series,
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
    return `CA des ventes sur ${days.length} jours : ${MONEY.format(total)}. Valeurs détaillées dans le tableau.`;
  });

  /** Recalcule a chaque changement de theme : le canvas ne lit pas les variables CSS. */
  protected readonly options = computed<ChartConfiguration<'bar'>['options']>(() => {
    const colors = this.colors();
    return {
      responsive: true,
      maintainAspectRatio: false,
      animation: false,
      interaction: { mode: 'index', intersect: false },
      scales: {
        x: {
          grid: { display: false },
          border: { color: colors.grid },
          ticks: { color: colors.text, autoSkip: true, maxRotation: 0, autoSkipPadding: 12, font: { family: 'Inter' } },
        },
        y: {
          beginAtZero: true,
          grid: { color: colors.grid },
          border: { display: false },
          ticks: {
            color: colors.text,
            maxTicksLimit: 5,
            font: { family: 'Inter' },
            callback: (value) => MONEY_AXIS.format(Number(value)),
          },
        },
      },
      plugins: {
        tooltip: {
          displayColors: false,
          padding: 10,
          cornerRadius: 8,
          titleFont: { family: 'Inter', weight: 600 },
          bodyFont: { family: 'Inter' },
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
  });
}
