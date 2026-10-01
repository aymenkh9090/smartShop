import { Payment, PaymentRequest, PaymentStatus } from '../../core/models/payment';

export type SaleStatus = 'COMPLETED' | 'CANCELLED';

export const SALE_STATUS_LABELS: Record<SaleStatus, string> = {
  COMPLETED: 'Validee',
  CANCELLED: 'Annulee',
};

export interface SaleCustomer {
  id: number;
  firstName: string;
  lastName: string;
  phone: string;
}

export interface SaleItem {
  id: number;
  productId: number;
  productSku: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
  warrantyMonths: number | null;
  units: { id: number; imei: string | null; serialNumber: string | null }[];
}

export interface Sale {
  id: number;
  reference: string;
  customer: SaleCustomer | null;
  status: SaleStatus;
  items: SaleItem[];
  subtotal: number;
  discount: number;
  total: number;
  amountPaid: number;
  amountDue: number;
  paymentStatus: PaymentStatus;
  dueDate: string | null;
  payments: Payment[];
  notes: string | null;
  createdBy: string;
  createdAt: string;
  cancelledBy: string | null;
  cancelledAt: string | null;
  cancelReason: string | null;
}

export type SaleSummary = Omit<Sale, 'items' | 'payments' | 'subtotal' | 'discount' | 'notes' | 'cancelledBy' | 'cancelledAt' | 'cancelReason'>;

export interface SaleRequest {
  customerId: number | null;
  items: { productId: number; quantity: number; unitIds: number[] | null }[];
  discount: number | null;
  payments: PaymentRequest[];
  dueDate: string | null;
  notes: string | null;
}

export interface SaleFilter {
  q?: string | null;
  customerId?: number | null;
  status?: SaleStatus | null;
  paymentStatus?: PaymentStatus | null;
  from?: string | null;
  to?: string | null;
}
