import type { ReactNode } from 'react';

interface StatCardProps {
  label: string;
  value: string | number;
  hint?: string;
  icon?: ReactNode;
}

/**
 * Single KPI tile used on the admin dashboard.
 */
export function StatCard({ label, value, hint, icon }: StatCardProps) {
  return (
    <article className="stat-card">
      {icon ? <div className="stat-card__icon">{icon}</div> : null}
      <p className="stat-card__label">{label}</p>
      <p className="stat-card__value">{value}</p>
      {hint ? <p className="stat-card__hint">{hint}</p> : null}
    </article>
  );
}
