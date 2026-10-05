export type StockMovementType = 'STOCK_IN' | 'STOCK_OUT' | 'ADJUSTMENT' | 'RETURN';
export type ProductUnitStatus = 'IN_STOCK' | 'SOLD' | 'REMOVED';

export const MOVEMENT_TYPE_LABELS: Record<StockMovementType, string> = {
  STOCK_IN: 'Entrée',
  STOCK_OUT: 'Sortie',
  ADJUSTMENT: 'Ajustement',
  RETURN: 'Retour client',
};
export const MOVEMENT_TYPES = Object.keys(MOVEMENT_TYPE_LABELS) as StockMovementType[];

export const UNIT_STATUS_LABELS: Record<ProductUnitStatus, string> = {
  IN_STOCK: 'En stock',
  SOLD: 'Vendue',
  REMOVED: 'Sortie',
};
export const UNIT_STATUSES = Object.keys(UNIT_STATUS_LABELS) as ProductUnitStatus[];

export interface UnitSummary {
  id: number;
  imei: string | null;
  serialNumber: string | null;
}

export interface StockMovement {
  id: number;
  product: { id: number; sku: string; name: string };
  type: StockMovementType;
  quantityChange: number;
  quantityAfter: number;
  reason: string | null;
  reference: string | null;
  createdBy: { id: number; username: string };
  createdAt: string;
  units: UnitSummary[];
}

export interface ProductUnitRequest {
  imei: string | null;
  serialNumber: string | null;
}

/** quantity : positive pour STOCK_IN / STOCK_OUT / RETURN, signee pour ADJUSTMENT. */
export interface StockMovementRequest {
  productId: number;
  type: StockMovementType;
  quantity: number;
  reason: string | null;
  reference: string | null;
  units: ProductUnitRequest[] | null;
  unitIds: number[] | null;
}

export interface StockMovementFilter {
  q?: string | null;
  productId?: number | null;
  type?: StockMovementType | null;
  /** ISO-8601, inclus */
  from?: string | null;
  /** ISO-8601, exclu */
  to?: string | null;
}

export interface ProductUnit {
  id: number;
  productId: number;
  productSku: string;
  productName: string;
  imei: string | null;
  serialNumber: string | null;
  status: ProductUnitStatus;
  createdAt: string;
  updatedAt: string;
}

export interface ProductUnitFilter {
  q?: string | null;
  productId?: number | null;
  status?: ProductUnitStatus | null;
}

export interface InventoryRequest {
  reference: string | null;
  reason: string | null;
  lines: { productId: number; countedQuantity: number }[];
}

export interface InventoryResponse {
  countedProducts: number;
  adjustedProducts: number;
  movements: StockMovement[];
}
