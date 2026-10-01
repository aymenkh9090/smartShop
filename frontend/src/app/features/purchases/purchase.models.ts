import { Payment, PaymentRequest, PaymentStatus } from '../../core/models/payment';
import { ProductUnitRequest } from '../stock/stock.models';

export interface PurchaseItem {
  id: number;
  productId: number;
  productSku: string;
  productName: string;
  quantity: number;
  unitCost: number;
  lineTotal: number;
  units: { id: number; imei: string | null; serialNumber: string | null }[];
}

export interface Purchase {
  id: number;
  reference: string;
  supplier: { id: number; name: string };
  supplierInvoiceRef: string | null;
  items: PurchaseItem[];
  total: number;
  amountPaid: number;
  amountDue: number;
  paymentStatus: PaymentStatus;
  dueDate: string | null;
  payments: Payment[];
  notes: string | null;
  createdBy: string;
  createdAt: string;
}

export type PurchaseSummary = Omit<Purchase, 'items' | 'payments' | 'notes'>;

export interface PurchaseRequest {
  supplierId: number;
  supplierInvoiceRef: string | null;
  items: { productId: number; quantity: number; unitCost: number; units: ProductUnitRequest[] | null }[];
  payments: PaymentRequest[];
  dueDate: string | null;
  notes: string | null;
}

export interface PurchaseFilter {
  q?: string | null;
  supplierId?: number | null;
  paymentStatus?: PaymentStatus | null;
  from?: string | null;
  to?: string | null;
}
