import { createBrowserRouter } from 'react-router-dom';

import { AdminLayout } from '../components/layout/AdminLayout';
import { DashboardPage } from '../pages/DashboardPage';
import { FeaturePage } from '../pages/FeaturePage';

/**
 * Route table for the admin application.
 *
 * Every feature route is registered up front so that navigation is stable and
 * each area can be filled in independently as its goal is delivered.
 */
export const router = createBrowserRouter([
  {
    path: '/',
    element: <AdminLayout />,
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
          <FeaturePage
            title="Riders"
            description="Rider availability, workload and assignment."
            goal="Goal 07 - Smart Rider Assignment"
          />
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
]);
