import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';

import { useAuth } from './auth-context';

interface RequireAuthProps {
  children: ReactNode;
}

/** Blocks rendering of protected routes until a session is authenticated. */
export function RequireAuth({ children }: RequireAuthProps) {
  const { user, status } = useAuth();
  const location = useLocation();

  if (status === 'loading') {
    return (
      <div className="auth-loading" role="status">
        Restoring your session…
      </div>
    );
  }
  if (!user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }
  return <>{children}</>;
}
