export type ProductType =
  | 'SMARTPHONE'
  | 'RECEIVER'
  | 'IPTV_SUBSCRIPTION'
  | 'CHARGER'
  | 'CABLE'
  | 'EARPHONE'
  | 'SMARTWATCH'
  | 'ACCESSORY'
  | 'OTHER';

export type ProductCondition = 'NEW' | 'USED' | 'REFURBISHED';

export const PRODUCT_TYPE_LABELS: Record<ProductType, string> = {
  SMARTPHONE: 'Smartphone',
  RECEIVER: 'Récepteur',
  IPTV_SUBSCRIPTION: 'Abonnement IPTV',
  CHARGER: 'Chargeur',
  CABLE: 'Cable',
  EARPHONE: 'Écouteurs',
  SMARTWATCH: 'Montre connectée',
  ACCESSORY: 'Accessoire',
  OTHER: 'Autre',
};

export const PRODUCT_CONDITION_LABELS: Record<ProductCondition, string> = {
  NEW: 'Neuf',
  USED: 'Occasion',
  REFURBISHED: 'Reconditionné',
};

export const PRODUCT_TYPES = Object.keys(PRODUCT_TYPE_LABELS) as ProductType[];
export const PRODUCT_CONDITIONS = Object.keys(PRODUCT_CONDITION_LABELS) as ProductCondition[];

export interface Product {
  id: number;
  sku: string;
  barcode: string | null;
  reference: string | null;
  name: string;
  description: string | null;
  category: { id: number; name: string };
  type: ProductType;
  condition: ProductCondition;
  brand: string | null;
  model: string | null;
  purchasePrice: number;
  salePrice: number;
  quantity: number;
  minStock: number;
  lowStock: boolean;
  serialTracked: boolean;
  warrantyMonths: number | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

/** La quantite n'est jamais envoyee : elle evolue uniquement par les mouvements de stock. */
/** Saisie simple : nom, categorie, type et prix de vente suffisent (SKU genere si vide). */
export interface ProductRequest {
  sku: string | null;
  barcode: string | null;
  reference: string | null;
  name: string;
  description: string | null;
  categoryId: number;
  type: ProductType;
  condition: ProductCondition;
  brand: string | null;
  model: string | null;
  purchasePrice: number | null;
  salePrice: number;
  minStock: number;
  serialTracked: boolean;
  warrantyMonths: number | null;
}

export interface ProductFilter {
  q?: string | null;
  categoryId?: number | null;
  type?: ProductType | null;
  active?: boolean | null;
  /** true : produits dont la quantite est inferieure ou egale au seuil */
  lowStock?: boolean | null;
}
