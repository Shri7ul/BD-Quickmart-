import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';

import type { UserRole } from '../../types';
import { useAuth } from './auth-context';

interface RequireRoleProps {
  allowedRoles: readonly UserRole[];
  children: ReactNode;
}

/** Redirects to /unauthorized when the signed-in role is not permitted. */
export function RequireRole({ allowedRoles, children }: RequireRoleProps) {
  const { user } = useAuth();
  if (!user || !allowedRoles.includes(user.role)) {
    return <Navigate to="/unauthorized" replace />;
  }
  return <>{children}</>;
}
