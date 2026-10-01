export type PaymentMethod = 'CASH' | 'CARD' | 'BANK_TRANSFER' | 'CHECK' | 'OTHER';
export type PaymentStatus = 'PAID' | 'PARTIALLY_PAID' | 'UNPAID';

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  CASH: 'Especes',
  CARD: 'Carte',
  BANK_TRANSFER: 'Virement',
  CHECK: 'Cheque',
  OTHER: 'Autre',
};
export const PAYMENT_METHODS = Object.keys(PAYMENT_METHOD_LABELS) as PaymentMethod[];

export const PAYMENT_STATUS_LABELS: Record<PaymentStatus, string> = {
  PAID: 'Paye',
  PARTIALLY_PAID: 'Partiellement paye',
  UNPAID: 'Non paye',
};
export const PAYMENT_STATUSES = Object.keys(PAYMENT_STATUS_LABELS) as PaymentStatus[];

export interface PaymentRequest {
  amount: number;
  method: PaymentMethod;
  reference: string | null;
}

export interface Payment {
  id: number;
  amount: number;
  method: PaymentMethod;
  reference: string | null;
  paidAt: string;
  createdBy: string;
}
