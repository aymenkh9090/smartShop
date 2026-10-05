import { Payment, PaymentRequest, PaymentStatus } from '../../core/models/payment';

export type AfterSalesStatus =
  | 'RECEIVED'
  | 'DIAGNOSING'
  | 'WAITING_FOR_PART'
  | 'IN_REPAIR'
  | 'READY'
  | 'DELIVERED'
  | 'CANCELLED';

export const AFTER_SALES_STATUS_LABELS: Record<AfterSalesStatus, string> = {
  RECEIVED: 'Reçu',
  DIAGNOSING: 'Diagnostic',
  WAITING_FOR_PART: 'Attente pièce',
  IN_REPAIR: 'En réparation',
  READY: 'Prêt',
  DELIVERED: 'Livre',
  CANCELLED: 'Annulé',
};
export const AFTER_SALES_STATUSES = Object.keys(AFTER_SALES_STATUS_LABELS) as AfterSalesStatus[];

/** Etapes ou les pieces sont modifiables (miroir de AfterSalesStatus.isInWorkshop cote backend). */
export const WORKSHOP_STATUSES: readonly AfterSalesStatus[] = ['DIAGNOSING', 'WAITING_FOR_PART', 'IN_REPAIR'];

export function isClosed(status: AfterSalesStatus): boolean {
  return status === 'DELIVERED' || status === 'CANCELLED';
}

export interface Technician {
  id: number;
  username: string;
  firstName: string;
  lastName: string;
}

export interface TicketCustomer {
  id: number;
  firstName: string;
  lastName: string;
  phone: string;
}

export interface StatusChange {
  id: number;
  fromStatus: AfterSalesStatus | null;
  toStatus: AfterSalesStatus;
  comment: string | null;
  changedBy: string;
  changedAt: string;
}

export interface TicketPart {
  id: number;
  productId: number;
  productSku: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
  units: { id: number; imei: string | null; serialNumber: string | null }[];
  addedBy: string;
  addedAt: string;
}

export interface AfterSalesTicket {
  id: number;
  ticketNumber: string;
  customer: TicketCustomer;
  deviceDescription: string;
  product: { id: number; sku: string; name: string } | null;
  unitId: number | null;
  saleId: number | null;
  saleReference: string | null;
  imei: string | null;
  serialNumber: string | null;
  reportedIssue: string;
  diagnosis: string | null;
  repairDescription: string | null;
  technician: Technician | null;
  status: AfterSalesStatus;
  /** Statuts accessibles par le workflow technique (hors livraison et annulation). */
  nextStatuses: AfterSalesStatus[];
  warrantyUntil: string | null;
  underWarranty: boolean;
  estimatedCost: number | null;
  laborCost: number;
  partsTotal: number;
  totalCost: number;
  amountPaid: number;
  amountDue: number;
  paymentStatus: PaymentStatus;
  receivedAt: string;
  expectedDate: string | null;
  deliveredAt: string | null;
  notes: string | null;
  createdBy: string;
  statusHistory: StatusChange[];
  parts: TicketPart[];
  payments: Payment[];
}

export interface AfterSalesSummary {
  id: number;
  ticketNumber: string;
  customer: TicketCustomer;
  deviceDescription: string;
  imei: string | null;
  serialNumber: string | null;
  technician: Technician | null;
  status: AfterSalesStatus;
  underWarranty: boolean;
  totalCost: number;
  amountDue: number;
  paymentStatus: PaymentStatus;
  receivedAt: string;
  expectedDate: string | null;
}

export interface AfterSalesCreateRequest {
  customerId: number;
  deviceDescription: string;
  productId: number | null;
  imei: string | null;
  serialNumber: string | null;
  reportedIssue: string;
  estimatedCost: number | null;
  expectedDate: string | null;
  notes: string | null;
}

export interface AfterSalesWorkRequest {
  diagnosis: string | null;
  repairDescription: string | null;
  laborCost: number;
  estimatedCost: number | null;
  expectedDate: string | null;
  underWarranty: boolean;
  notes: string | null;
}

export interface AfterSalesPartRequest {
  productId: number;
  quantity: number;
  unitIds: number[] | null;
}

export interface AfterSalesFilter {
  q?: string | null;
  status?: AfterSalesStatus | null;
  technicianId?: number | null;
  customerId?: number | null;
  open?: boolean | null;
}

export interface DeliverRequest {
  payments: PaymentRequest[];
  comment: string | null;
}
