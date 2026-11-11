import { createBrowserRouter, Navigate } from 'react-router-dom';

import { AdminLayout } from '../components/layout/AdminLayout';
import { LoginPage } from '../features/auth/LoginPage';
import { RequireAuth } from '../features/auth/RequireAuth';
import { RequireRole } from '../features/auth/RequireRole';
import { UnauthorizedPage } from '../features/auth/UnauthorizedPage';
import { RIDER_APPROVER_ROLES } from '../features/auth/roles';
import { DashboardPage } from '../pages/DashboardPage';
import { FeaturePage } from '../pages/FeaturePage';

/**
 * Route table for the admin application (Goal 02).
 *
 * `/login` and `/unauthorized` are public. Everything else sits behind
 * RequireAuth. The riders area additionally demonstrates a role gate:
 * only ADMIN / SUPER_ADMIN (who hold PERM_APPROVE_RIDERS on the backend) may
 * open it; other privileged roles are redirected to /unauthorized.
 */
export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  { path: '/unauthorized', element: <UnauthorizedPage /> },
  {
    path: '/',
    element: (
      <RequireAuth>
        <AdminLayout />
      </RequireAuth>
    ),
    children: [
      { index: true, element: <DashboardPage /> },
      {
        path: 'orders',
        element: (
          <FeaturePage
            title="Orders"
            description="Order lifecycle management and dispatch."
            goal="Goal 06 - Order Management System"
          />
        ),
      },
      {
        path: 'products',
        element: (
          <FeaturePage
            title="Products"
            description="Catalogue and pricing across dark stores."
            goal="Goal 03 - Dark Store & Product Management"
          />
        ),
      },
      {
        path: 'inventory',
        element: (
          <FeaturePage
            title="Inventory"
            description="Stock levels, expiry and replenishment."
            goal="Goal 09 - Advanced Inventory Management"
          />
        ),
      },
      {
        path: 'riders',
        element: (
          <RequireRole allowedRoles={RIDER_APPROVER_ROLES}>
            <FeaturePage
              title="Riders"
              description="Rider availability, workload and assignment."
              goal="Goal 07 - Smart Rider Assignment"
            />
          </RequireRole>
        ),
      },
      {
        path: 'analytics',
        element: (
          <FeaturePage
            title="Analytics"
            description="Revenue, demand forecast and operational insights."
            goal="Goal 16 - Admin Dashboard & Analytics"
          />
        ),
      },
    ],
  },
  { path: '*', element: <Navigate to="/" replace /> },
]);
