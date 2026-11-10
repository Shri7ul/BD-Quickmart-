import { StatCard } from '../components/ui/StatCard';

/**
 * Operational overview for store managers and administrators.
 *
 * Values are placeholders until the analytics module (Goal 16) aggregates
 * real database figures. No mock numbers are presented as live data.
 */
export function DashboardPage() {
  return (
    <section className="page">
      <header className="page__header">
        <h2 className="page__title">Dashboard</h2>
        <p className="page__subtitle">
          Operational snapshot across stores, orders and riders.
        </p>
      </header>

      <div className="stat-grid">
        <StatCard label="Total orders" value="—" hint="Awaiting analytics module" />
        <StatCard label="Revenue" value="—" hint="Awaiting analytics module" />
        <StatCard label="Active riders" value="—" hint="Awaiting rider module" />
        <StatCard label="Low stock items" value="—" hint="Awaiting inventory module" />
      </div>

      <div className="panel">
        <h3 className="panel__title">Recent orders</h3>
        <p className="panel__body">
          Order data appears here once the order module is connected.
        </p>
      </div>
    </section>
  );
}
