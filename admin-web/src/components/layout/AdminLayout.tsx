import { NavLink, Outlet } from 'react-router-dom';

import { appConfig } from '../../lib/config/env';

interface NavItem {
  to: string;
  label: string;
  end?: boolean;
}

const navItems: NavItem[] = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/orders', label: 'Orders' },
  { to: '/products', label: 'Products' },
  { to: '/inventory', label: 'Inventory' },
  { to: '/riders', label: 'Riders' },
  { to: '/analytics', label: 'Analytics' },
];

/**
 * Primary application shell: fixed sidebar navigation plus the routed content
 * area. The layout collapses to a stacked arrangement on narrow viewports.
 */
export function AdminLayout() {
  return (
    <div className="app-shell">
      <aside className="app-sidebar">
        <div className="app-brand">
          <span className="app-brand__mark">QM</span>
          <span className="app-brand__name">{appConfig.appName}</span>
        </div>
        <nav className="app-nav" aria-label="Main navigation">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                isActive ? 'app-nav__link app-nav__link--active' : 'app-nav__link'
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="app-sidebar__footer">
          <span className="app-status-dot" aria-hidden="true" />
          {appConfig.isDevelopment ? 'Development' : 'Production'}
        </div>
      </aside>

      <div className="app-content">
        <header className="app-topbar">
          <h1 className="app-topbar__title">{appConfig.appName}</h1>
          <div className="app-topbar__actions">
            <button type="button" className="app-button app-button--ghost">
              Sign out
            </button>
          </div>
        </header>
        <main className="app-main">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
