export type PaymentMethod = 'CASH' | 'CARD' | 'BANK_TRANSFER' | 'CHECK' | 'OTHER';
export type PaymentStatus = 'PAID' | 'PARTIALLY_PAID' | 'UNPAID';

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  CASH: 'Espèces',
  CARD: 'Carte',
  BANK_TRANSFER: 'Virement',
  CHECK: 'Chèque',
  OTHER: 'Autre',
};
export const PAYMENT_METHODS = Object.keys(PAYMENT_METHOD_LABELS) as PaymentMethod[];

export const PAYMENT_STATUS_LABELS: Record<PaymentStatus, string> = {
  PAID: 'Payé',
  PARTIALLY_PAID: 'Partiellement payé',
  UNPAID: 'Non payé',
};
export const PAYMENT_STATUSES = Object.keys(PAYMENT_STATUS_LABELS) as PaymentStatus[];

export interface PaymentRequest {
  amount: number;
  method: PaymentMethod;
  reference: string | null;
  /** Date du versement (yyyy-MM-dd), uniquement pour un paiement sur credit ; aujourd'hui par defaut. */
  paidOn?: string | null;
}

export interface Payment {
  id: number;
  amount: number;
  method: PaymentMethod;
  reference: string | null;
  paidAt: string;
  createdBy: string;
}

/** Statut d'un credit, calcule par le backend (paiements + echeance). */
export type CreditStatus = 'PENDING' | 'PARTIALLY_PAID' | 'PAID' | 'OVERDUE' | 'CANCELLED';

export const CREDIT_STATUS_LABELS: Record<CreditStatus, string> = {
  PENDING: 'En attente',
  PARTIALLY_PAID: 'Partiellement payé',
  PAID: 'Solde',
  OVERDUE: 'En retard',
  CANCELLED: 'Annulé',
};
export const CREDIT_STATUSES = Object.keys(CREDIT_STATUS_LABELS) as CreditStatus[];
