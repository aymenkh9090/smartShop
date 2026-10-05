import { Role, User } from '../../core/models/auth.models';

export type ManagedUser = User;

export interface UserCreateRequest {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  role: Role;
}

export interface UserUpdateRequest {
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
}

export interface UserFilter {
  q?: string | null;
  role?: Role | null;
  enabled?: boolean | null;
}
