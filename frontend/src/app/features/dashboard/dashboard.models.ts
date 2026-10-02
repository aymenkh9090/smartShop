import { AfterSalesStatus } from '../after-sales/after-sales.models';

export interface Totals {
  count: number;
  amount: number;
}

export interface MonthTotals extends Totals {
  /** Meme periode du mois precedent (du 1er au meme jour). */
  previousAmount: number;
}

export interface Credits {
  count: number;
  outstanding: number;
  overdue: number;
}

export interface LowStockProduct {
  id: number;
  sku: string;
  name: string;
  quantity: number;
  minStock: number;
}

export interface ReadyTicket {
  id: number;
  ticketNumber: string;
  customerName: string;
  customerPhone: string;
  deviceDescription: string;
  amountDue: number;
}

export interface TopProduct {
  productId: number;
  sku: string;
  name: string;
  quantity: number;
  revenue: number;
}

export interface DailyAmount {
  date: string;
  amount: number;
}

/** Une section vaut null quand le role de l'utilisateur n'y a pas acces (decide par le backend). */
export interface Dashboard {
  date: string;
  todaySales: Totals | null;
  monthSales: MonthTotals | null;
  monthPurchases: Totals | null;
  lowStock: { count: number; products: LowStockProduct[] } | null;
  customerCredits: Credits | null;
  supplierCredits: Credits | null;
  afterSales: {
    openCount: number;
    byStatus: Partial<Record<AfterSalesStatus, number>>;
    ready: ReadyTicket[];
  } | null;
  monthAfterSales: Totals | null;
  topProducts: TopProduct[] | null;
  dailyRevenue: DailyAmount[] | null;
}
