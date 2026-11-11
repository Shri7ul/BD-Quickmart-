import { Link } from 'react-router-dom';

/** Shown when a signed-in operator lacks the role required for a route. */
export function UnauthorizedPage() {
  return (
    <div className="auth-page">
      <div className="auth-card">
        <h1 className="auth-card__title">Access denied</h1>
        <p className="auth-card__subtitle">
          Your role does not have permission to view this page.
        </p>
        <Link className="app-button app-button--primary" to="/">
          Back to dashboard
        </Link>
      </div>
    </div>
  );
}
