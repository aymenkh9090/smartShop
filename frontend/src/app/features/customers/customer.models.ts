export interface Customer {
  id: number;
  firstName: string;
  lastName: string;
  phone: string;
  email: string | null;
  address: string | null;
  cin: string | null;
  notes: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CustomerRequest {
  firstName: string;
  lastName: string;
  phone: string;
  email: string | null;
  address: string | null;
  cin: string | null;
  notes: string | null;
}

export interface CustomerFilter {
  q?: string | null;
  active?: boolean | null;
}
