import { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';

import { useAuth } from '../../features/auth/auth-context';
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
 *
 * Goal 02 wires the previously inert "Sign out" button to AuthContext.logout
 * (which calls POST /auth/logout first) and shows the signed-in operator.
 */
export function AdminLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [signingOut, setSigningOut] = useState(false);

  async function handleSignOut(): Promise<void> {
    setSigningOut(true);
    try {
      await logout();
    } finally {
      navigate('/login', { replace: true });
      setSigningOut(false);
    }
  }

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
            {user ? (
              <div className="app-topbar__user">
                <span className="app-topbar__user-name">{user.fullName}</span>
                <span className="app-topbar__user-role">{user.role}</span>
              </div>
            ) : null}
            <button
              type="button"
              className="app-button app-button--ghost"
              onClick={handleSignOut}
              disabled={signingOut}
            >
              {signingOut ? 'Signing out…' : 'Sign out'}
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
