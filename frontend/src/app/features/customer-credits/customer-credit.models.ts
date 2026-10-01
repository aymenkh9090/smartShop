import { CreditStatus, Payment } from '../../core/models/payment';

export interface CustomerCredit {
  saleId: number;
  reference: string;
  customer: { id: number; firstName: string; lastName: string; phone: string };
  createdAt: string;
  createdBy: string;
  total: number;
  amountPaid: number;
  amountDue: number;
  dueDate: string | null;
  status: CreditStatus;
  payments: Payment[];
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
