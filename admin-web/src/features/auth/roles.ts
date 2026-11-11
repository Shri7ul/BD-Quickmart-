import type { UserRole } from '../../types';

/** Roles permitted to sign in to the admin web application. */
export const PRIVILEGED_ROLES: readonly UserRole[] = [
  'ADMIN',
  'SUPER_ADMIN',
  'STORE_MANAGER',
  'INVENTORY_OPERATOR',
];

/** Roles permitted to approve / reject rider applications. */
export const RIDER_APPROVER_ROLES: readonly UserRole[] = ['ADMIN', 'SUPER_ADMIN'];

export function isPrivileged(role: UserRole): boolean {
  return PRIVILEGED_ROLES.includes(role);
}
