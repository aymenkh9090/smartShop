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
