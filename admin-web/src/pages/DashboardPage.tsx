import { useState } from 'react';
import { Link } from 'react-router-dom';

import { StatCard } from '../components/ui/StatCard';
import { StatusBadge } from '../components/ui/StatusBadge';
import { errorMessage, orderApi } from '../features/api/commerce';
import { useAsync } from '../hooks/useAsync';

/** Live operational snapshot. Every number comes from the backend database. */
export function DashboardPage() {
  const [refreshing, setRefreshing] = useState(false);
  const { data, loading, error, reload } = useAsync(() => orderApi.dashboard());

  async function refresh() {
    setRefreshing(true);
    try {
      await reload();
    } finally {
      setRefreshing(false);
    }
  }

  const money = (value: number) => `৳${value.toLocaleString('en-BD', { minimumFractionDigits: 2 })}`;

  return (
    <section className="page">
      <header className="page__header">
        <div>
          <h1 className="page__title">Dashboard</h1>
          <p className="page__subtitle">Live operational snapshot from the production database.</p>
        </div>
        <button className="btn btn--ghost" type="button" onClick={refresh} disabled={refreshing}>
          {refreshing ? 'Refreshing…' : 'Refresh'}
        </button>
      </header>

      {error && <p className="alert alert--error" role="alert">{errorMessage(error)}</p>}

      <div className="stat-grid">
        <StatCard label="Total orders" value={data?.totalOrders ?? '—'} hint="All recorded orders" />
        <StatCard label="Pending orders" value={data?.pendingOrders ?? '—'} hint="Awaiting fulfilment" />
        <StatCard label="Revenue" value={data ? money(data.revenue) : '—'} hint="Delivered orders only" />
        <StatCard label="Active riders" value={data?.activeRiders ?? '—'} hint="Approved accounts" />
        <StatCard label="Low-stock items" value={data?.lowStockItems ?? '—'} hint="At or below threshold" />
      </div>

      <section className="panel">
        <div className="panel__header">
          <h2 className="panel__title">Recent orders</h2>
          <Link className="link" to="/orders">View all</Link>
        </div>
        {loading && <p className="panel__body">Loading…</p>}
        {!loading && !error && (data?.recentOrders.length ?? 0) === 0 && (
          <p className="panel__body">No orders have been placed yet.</p>
        )}
        {!loading && (data?.recentOrders.length ?? 0) > 0 && (
          <div className="table-wrapper">
            <table className="data-table">
              <thead>
                <tr>
                  <th scope="col">Order</th>
                  <th scope="col">Customer</th>
                  <th scope="col">Status</th>
                  <th scope="col">Total</th>
                  <th scope="col">Placed</th>
                </tr>
              </thead>
              <tbody>
                {data?.recentOrders.map((order) => (
                  <tr key={order.publicId}>
                    <td><Link className="link" to="/orders">{order.publicId.slice(0, 8)}</Link></td>
                    <td>{order.customerName}</td>
                    <td><StatusBadge status={order.status} /></td>
                    <td>{money(order.totalAmount)}</td>
                    <td>{new Date(order.createdAt).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </section>
  );
}
