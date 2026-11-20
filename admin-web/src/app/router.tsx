import { createBrowserRouter, Navigate } from 'react-router-dom';

import { AdminLayout } from '../components/layout/AdminLayout';
import { LoginPage } from '../features/auth/LoginPage';
import { RequireAuth } from '../features/auth/RequireAuth';
import { RequireRole } from '../features/auth/RequireRole';
import { UnauthorizedPage } from '../features/auth/UnauthorizedPage';
import { RIDER_APPROVER_ROLES } from '../features/auth/roles';
import { AnalyticsPage } from '../pages/AnalyticsPage';
import { CategoriesPage } from '../pages/CategoriesPage';
import { DashboardPage } from '../pages/DashboardPage';
import { InventoryPage } from '../pages/InventoryPage';
import { OrdersPage } from '../pages/OrdersPage';
import { ProductsPage } from '../pages/ProductsPage';
import { RidersPage } from '../pages/RidersPage';

/** Route table for the Phase 1 admin console. */
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
      { path: 'orders', element: <OrdersPage /> },
      { path: 'products', element: <ProductsPage /> },
      { path: 'categories', element: <CategoriesPage /> },
      { path: 'inventory', element: <InventoryPage /> },
      {
        path: 'riders',
        element: (
          <RequireRole allowedRoles={RIDER_APPROVER_ROLES}>
            <RidersPage />
          </RequireRole>
        ),
      },
      { path: 'analytics', element: <AnalyticsPage /> },
    ],
  },
  { path: '*', element: <Navigate to="/" replace /> },
]);
