export interface Supplier {
  id: number;
  name: string;
  phone: string | null;
  email: string | null;
  address: string | null;
  /** Matricule fiscal */
  taxId: string | null;
  notes: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SupplierRequest {
  name: string;
  phone: string | null;
  email: string | null;
  address: string | null;
  taxId: string | null;
  notes: string | null;
}

export interface SupplierFilter {
  q?: string | null;
  active?: boolean | null;
}
