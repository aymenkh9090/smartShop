import { CreditStatus, Payment } from '../../core/models/payment';

/** Origine d'un credit : achat non solde ou credit saisi a la main. */
export type SupplierCreditSource = 'PURCHASE' | 'MANUAL';

/** Ligne de la liste des credits fournisseurs. */
export interface SupplierCreditSummary {
  /** Id de l'achat (PURCHASE) ou du credit saisi (MANUAL). */
  id: number;
  source: SupplierCreditSource;
  reference: string;
  supplierInvoiceRef: string | null;
  description: string | null;
  supplier: { id: number; name: string; phone: string | null };
  /** Date du credit. */
  createdAt: string;
  createdBy: string;
  total: number;
  amountPaid: number;
  amountDue: number;
  dueDate: string | null;
  status: CreditStatus;
}

export interface SupplierCredit extends SupplierCreditSummary {
  cancelReason: string | null;
  payments: Payment[];
}

/** Credit saisi a la main : la boutique doit un montant au fournisseur, regle par tranches. */
export interface SupplierCreditRequest {
  supplierId: number;
  amount: number;
  /** yyyy-MM-dd ; aujourd'hui par defaut. */
  creditDate: string | null;
  dueDate: string | null;
  description: string | null;
}

export interface SupplierCreditFilter {
  q?: string | null;
  supplierId?: number | null;
  status?: CreditStatus | null;
  /** true : credits non soldes et non annules */
  open?: boolean | null;
}

export interface SupplierBalance {
  supplierId: number;
  name: string;
  phone: string | null;
  openCredits: number;
  outstanding: number;
  overdue: number;
  nextDueDate: string | null;
}

/** Lien vers le detail d'un credit, selon son origine. */
export function supplierCreditLink(credit: Pick<SupplierCreditSummary, 'id' | 'source'>): (string | number)[] {
  return credit.source === 'MANUAL' ? ['/supplier-credits', 'manual', credit.id] : ['/supplier-credits', credit.id];
}
