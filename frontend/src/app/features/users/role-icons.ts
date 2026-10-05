import { Role } from '../../core/models/auth.models';

export const ROLE_ICONS: Record<Role, string> = {
  ADMIN: 'shield_person',
  MANAGER: 'manage_accounts',
  SELLER: 'point_of_sale',
  TECHNICIAN: 'build',
};
