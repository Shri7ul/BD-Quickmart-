import { StatCard } from '../components/ui/StatCard';
import { errorMessage, orderApi } from '../features/api/commerce';
import { useAsync } from '../hooks/useAsync';

/** Operational analytics backed by real aggregates; unavailable metrics are labelled. */
export function AnalyticsPage() {
  const { data, loading, error } = useAsync(() => orderApi.dashboard());

  return (
    <section className="page">
      <header className="page__header">
        <div>
          <h1 className="page__title">Analytics</h1>
          <p className="page__subtitle">Figures computed directly from the database.</p>
        </div>
      </header>

      {error && <p className="alert alert--error">{errorMessage(error)}</p>}
      {loading && <p className="panel__body">Loading…</p>}

      <div className="stat-grid">
        <StatCard label="Orders placed" value={data?.totalOrders ?? '—'} />
        <StatCard label="Awaiting fulfilment" value={data?.pendingOrders ?? '—'} />
        <StatCard label="Delivered revenue" value={data ? `৳${data.revenue.toFixed(2)}` : '—'} />
        <StatCard label="Approved riders" value={data?.activeRiders ?? '—'} />
        <StatCard label="Low-stock SKUs" value={data?.lowStockItems ?? '—'} />
        <StatCard label="Demand forecast" value="Unavailable" hint="Not implemented in Phase 1" />
      </div>
    </section>
  );
}
