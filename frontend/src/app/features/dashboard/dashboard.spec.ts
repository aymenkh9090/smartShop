import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';

import { NotificationService } from '../../core/services/notification.service';
import { Dashboard } from './dashboard';
import { Dashboard as DashboardData } from './dashboard.models';
import { DashboardService } from './dashboard.service';

const MANAGER_DATA: DashboardData = {
  date: '2026-10-15',
  todaySales: { count: 2, amount: 180 },
  monthSales: { count: 12, amount: 1100, previousAmount: 1000 },
  monthPurchases: { count: 1, amount: 348 },
  lowStock: { count: 1, products: [{ id: 3, sku: 'CAB-USB-C', name: 'Cable USB-C', quantity: 1, minStock: 5 }] },
  customerCredits: { count: 1, outstanding: 70, overdue: 70 },
  supplierCredits: { count: 1, outstanding: 328, overdue: 0 },
  afterSales: {
    openCount: 2,
    byStatus: { RECEIVED: 1, READY: 1 },
    ready: [
      { id: 9, ticketNumber: 'SAV-2026-000009', customerName: 'Ben Salah Ali', customerPhone: '22333444', deviceDescription: 'Samsung A55', amountDue: 60 },
    ],
  },
  monthAfterSales: { count: 1, amount: 40 },
  topProducts: [{ productId: 3, sku: 'CAB-USB-C', name: 'Cable USB-C', quantity: 4, revenue: 80 }],
  dailyRevenue: Array.from({ length: 30 }, (_, i) => ({ date: `2026-09-${String(16 + i).padStart(2, '0')}`, amount: i * 10 }))
    .map((day, i) => (i >= 15 ? { ...day, date: `2026-10-${String(i - 14).padStart(2, '0')}` } : day)),
};

const SELLER_DATA: DashboardData = {
  ...MANAGER_DATA,
  monthSales: null,
  monthPurchases: null,
  customerCredits: null,
  supplierCredits: null,
  monthAfterSales: null,
  topProducts: null,
  dailyRevenue: null,
};

describe('Dashboard', () => {
  function render(response: Observable<DashboardData>): HTMLElement {
    TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        provideRouter([]),
        { provide: DashboardService, useValue: { get: () => response } },
        { provide: NotificationService, useValue: jasmine.createSpyObj('NotificationService', ['success', 'error']) },
      ],
    });
    const fixture: ComponentFixture<Dashboard> = TestBed.createComponent(Dashboard);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('shows every section returned for a manager, with the month trend and the chart', () => {
    const element = render(of(MANAGER_DATA));
    const text = element.textContent ?? '';

    expect(text).toContain('CA du mois');
    expect(text).toContain('+10');
    expect(text).toContain('Credits fournisseurs');
    expect(text).toContain('CA SAV du mois');
    expect(text).toContain('Produits les plus vendus');
    expect(text).toContain('SAV-2026-000009');
    expect(element.querySelector('app-revenue-chart canvas')).not.toBeNull();
    expect(element.querySelectorAll('app-revenue-chart tbody tr').length).toBe(30);
  });

  it('shows only the sections the backend returned for a seller', () => {
    const text = render(of(SELLER_DATA)).textContent ?? '';

    expect(text).toContain('Ventes du jour');
    expect(text).toContain('Stock faible');
    expect(text).toContain('SAV en cours');
    expect(text).not.toContain('CA du mois');
    expect(text).not.toContain('Credits clients');
    expect(text).not.toContain('Produits les plus vendus');
  });

  it('shows the previous amount instead of a trend when last month had no sales', () => {
    const text = render(of({ ...MANAGER_DATA, monthSales: { count: 1, amount: 50, previousAmount: 0 } })).textContent ?? '';

    expect(text).toContain('mois dernier a date');
    expect(text).not.toContain('vs mois dernier');
  });

  it('reports a loading error', () => {
    const text = render(throwError(() => new Error('boom'))).textContent ?? '';

    expect(text).toContain("n'a pas pu etre charge");
    expect(TestBed.inject(NotificationService).error).toHaveBeenCalled();
  });
});
