import { RouterProvider } from 'react-router-dom';

import { router } from './router';

/** Router provider mounted by the application root. */
export function AppRouter() {
  return <RouterProvider router={router} />;
}
