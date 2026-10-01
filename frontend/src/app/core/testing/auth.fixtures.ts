import { AuthResponse, User } from '../models/auth.models';

/** Donnees de test partagees par les specs d'authentification. */
export const SELLER: User = {
  id: 2,
  username: 'seller',
  email: 'seller@smartshop.test',
  firstName: 'Sami',
  lastName: 'Ben Ali',
  role: 'SELLER',
  enabled: true,
  createdAt: '2026-10-01T10:00:00Z',
};

export function authResponse(suffix: string, user: User = SELLER): AuthResponse {
  return {
    accessToken: `access-${suffix}`,
    refreshToken: `refresh-${suffix}`,
    tokenType: 'Bearer',
    expiresIn: 900,
    user,
  };
}
