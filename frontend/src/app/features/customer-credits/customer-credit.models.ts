import { CreditStatus, Payment } from '../../core/models/payment';

/** Origine d'un credit : vente non soldee ou credit saisi a la main. */
export type CustomerCreditSource = 'SALE' | 'MANUAL';

/** Ligne de la liste des credits clients. */
export interface CustomerCreditSummary {
  /** Id de la vente (SALE) ou du credit saisi (MANUAL). */
  id: number;
  source: CustomerCreditSource;
  reference: string;
  description: string | null;
  customer: { id: number; firstName: string; lastName: string; phone: string };
  /** Date du credit. */
  createdAt: string;
  createdBy: string;
  total: number;
  amountPaid: number;
  amountDue: number;
  dueDate: string | null;
  status: CreditStatus;
}

export interface CustomerCredit extends CustomerCreditSummary {
  cancelReason: string | null;
  payments: Payment[];
}

/** Credit saisi a la main : le client doit un montant, rembourse par tranches. */
export interface CustomerCreditRequest {
  customerId: number;
  amount: number;
  /** yyyy-MM-dd ; aujourd'hui par defaut. */
  creditDate: string | null;
  dueDate: string | null;
  description: string | null;
}

export interface CustomerCreditFilter {
  q?: string | null;
  customerId?: number | null;
  status?: CreditStatus | null;
  /** true : credits non soldes et non annules */
  open?: boolean | null;
}

export interface CustomerBalance {
  customerId: number;
  firstName: string;
  lastName: string;
  phone: string;
  openCredits: number;
  outstanding: number;
  overdue: number;
  nextDueDate: string | null;
}

/** Lien vers le detail d'un credit, selon son origine. */
export function customerCreditLink(credit: Pick<CustomerCreditSummary, 'id' | 'source'>): (string | number)[] {
  return credit.source === 'MANUAL' ? ['/customer-credits', 'manual', credit.id] : ['/customer-credits', credit.id];
}
