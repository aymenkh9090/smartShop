import { CreditStatus, Payment } from '../../core/models/payment';

export interface SupplierCredit {
  purchaseId: number;
  reference: string;
  supplierInvoiceRef: string | null;
  supplier: { id: number; name: string; phone: string | null };
  createdAt: string;
  createdBy: string;
  total: number;
  amountPaid: number;
  amountDue: number;
  dueDate: string | null;
  status: CreditStatus;
  payments: Payment[];
}

export interface SupplierCreditFilter {
  q?: string | null;
  supplierId?: number | null;
  status?: CreditStatus | null;
  /** true : credits non soldes */
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
