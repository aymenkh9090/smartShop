export type Role = 'ADMIN' | 'MANAGER' | 'SELLER' | 'TECHNICIAN';

export interface User {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
  enabled: boolean;
  createdAt: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

/** Miroir de la hierarchie backend : ADMIN > MANAGER > SELLER / TECHNICIAN. */
export const ROLE_HIERARCHY: Record<Role, readonly Role[]> = {
  ADMIN: ['ADMIN', 'MANAGER', 'SELLER', 'TECHNICIAN'],
  MANAGER: ['MANAGER', 'SELLER', 'TECHNICIAN'],
  SELLER: ['SELLER'],
  TECHNICIAN: ['TECHNICIAN'],
};

export const ROLES: readonly Role[] = ['ADMIN', 'MANAGER', 'SELLER', 'TECHNICIAN'];

export const ROLE_LABELS: Record<Role, string> = {
  ADMIN: 'Administrateur',
  MANAGER: 'Gérant',
  SELLER: 'Vendeur',
  TECHNICIAN: 'Technicien',
};

export const ROLE_DESCRIPTIONS: Record<Role, string> = {
  ADMIN: 'Accès complet, y compris la gestion des utilisateurs',
  MANAGER: 'Achats, fournisseurs, crédits et statistiques complètes',
  SELLER: 'Ventes, clients, crédits clients et réception SAV',
  TECHNICIAN: 'Diagnostic et réparation des tickets SAV',
};
