import { useState } from 'react';
import type { FormEvent } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';

import { appConfig } from '../../lib/config/env';
import { useAuth } from './auth-context';

/** Sign-in screen for privileged operators (CUSTOMER/RIDER are rejected). */
export function LoginPage() {
  const { login, status, user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const from = (location.state as { from?: { pathname?: string } } | null)
    ?.from?.pathname ?? '/';

  if (status === 'authenticated' && user) {
    return <Navigate to={from} replace />;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login(identifier, password);
      navigate(from, { replace: true });
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Sign in failed. Please try again.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="auth-page">
      <form className="auth-card" onSubmit={handleSubmit}>
        <div className="auth-card__brand">
          <span className="auth-card__mark">QM</span>
          <span>{appConfig.appName}</span>
        </div>
        <h1 className="auth-card__title">Sign in</h1>
        <p className="auth-card__subtitle">
          Use your administrator email or phone number.
        </p>

        <label className="auth-field">
          <span className="auth-field__label">Email or phone</span>
          <input
            className="auth-field__input"
            type="text"
            autoComplete="username"
            value={identifier}
            onChange={(event) => setIdentifier(event.target.value)}
            required
          />
        </label>

        <label className="auth-field">
          <span className="auth-field__label">Password</span>
          <input
            className="auth-field__input"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
        </label>

        {error ? (
          <p className="auth-error" role="alert">{error}</p>
        ) : null}

        <button className="app-button app-button--primary" type="submit" disabled={submitting}>
          {submitting ? 'Signing in…' : 'Sign in'}
        </button>

        <p className="auth-card__note">
          Only ADMIN, SUPER_ADMIN, STORE_MANAGER and INVENTORY_OPERATOR accounts
          may use this console. Customer and rider accounts are rejected.
        </p>
      </form>
    </div>
  );
}
