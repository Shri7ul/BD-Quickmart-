import { useMemo, useState } from 'react';

import { DataTable } from '../components/ui/DataTable';
import { StatusBadge } from '../components/ui/StatusBadge';
import { errorMessage, orderApi, riderApi } from '../features/api/commerce';
import { useAsync } from '../hooks/useAsync';
import type { Order, OrderStatus } from '../types';

const ADMIN_ACTIONS: OrderStatus[] = ['CONFIRMED', 'PREPARING', 'READY_FOR_PICKUP', 'CANCELLED'];

/** Order management: search, detail, lifecycle transitions and rider dispatch. */
export function OrdersPage() {
  const orders = useAsync(() => orderApi.list());
  const riders = useAsync(() => riderApi.eligible());
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [selected, setSelected] = useState<string | null>(null);
  const [riderId, setRiderId] = useState('');
  const [note, setNote] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const rows = useMemo(() => (orders.data ?? []).filter((order) => {
    const matchesSearch = !search || `${order.publicId} ${order.customerName} ${order.contactPhone}`
      .toLowerCase().includes(search.toLowerCase());
    const matchesStatus = !status || order.status === status;
    return matchesSearch && matchesStatus;
  }), [orders.data, search, status]);

  const detail = useAsync(
    () => selected ? orderApi.get(selected) : Promise.resolve(null),
    [selected],
  );

  async function run(action: () => Promise<unknown>) {
    setBusy(true);
    setError(null);
    try {
      await action();
      await orders.reload();
      await detail.reload();
      await riders.reload();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="page">
      <header className="page__header">
        <div>
          <h1 className="page__title">Orders</h1>
          <p className="page__subtitle">Confirm, prepare, dispatch and track every order.</p>
        </div>
      </header>

      <section className="panel">
        <div className="filter-bar">
          <label className="field"><span>Search</span><input value={search}
            onChange={(e) => setSearch(e.target.value)} placeholder="ID, customer or phone" /></label>
          <label className="field"><span>Status</span><select value={status}
            onChange={(e) => setStatus(e.target.value)}>
            <option value="">All statuses</option>
            {['PENDING', 'CONFIRMED', 'PREPARING', 'READY_FOR_PICKUP', 'ASSIGNED',
              'PICKED_UP', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED',
              'REJECTED', 'FAILED_DELIVERY'].map((s) => <option key={s} value={s}>{s.replace(/_/g, ' ')}</option>)}
          </select></label>
        </div>
      </section>

      {error && <p className="alert alert--error">{error}</p>}
      {orders.error && <p className="alert alert--error">{errorMessage(orders.error)}</p>}

      <section className="panel">
        <div className="panel__header"><h2 className="panel__title">All orders ({rows.length})</h2></div>
        {orders.loading && <p className="panel__body">Loading…</p>}
        {!orders.loading && rows.length === 0 && <p className="panel__body">No orders match these filters.</p>}
        {rows.length > 0 && (
          <DataTable<Order>
            rows={rows}
            rowKey={(row) => row.publicId}
            columns={[
              { key: 'publicId', header: 'Order', render: (row) => row.publicId.slice(0, 8) },
              { key: 'customerName', header: 'Customer' },
              { key: 'totalAmount', header: 'Total', render: (row) => `৳${row.totalAmount.toFixed(2)}` },
              { key: 'paymentMethod', header: 'Payment' },
              { key: 'status', header: 'Status', render: (row) => <StatusBadge status={row.status} /> },
              { key: 'createdAt', header: 'Placed', render: (row) => new Date(row.createdAt).toLocaleString() },
              { key: 'view', header: '', render: (row) => (
                <button className="btn btn--small" onClick={() => { setSelected(row.publicId); setNote(''); }}>
                  Details
                </button>
              ) },
            ]}
          />
        )}
      </section>

      {selected && detail.data && (
        <section className="panel">
          <div className="panel__header">
            <h2 className="panel__title">Order {detail.data.publicId.slice(0, 8)}</h2>
            <StatusBadge status={detail.data.status} />
          </div>
          <div className="detail-grid">
            <div>
              <h3>Items</h3>
              <ul className="detail-list">
                {detail.data.items.map((item) => (
                  <li key={item.productPublicId}>
                    {item.quantity} × {item.productName} — ৳{item.lineTotal.toFixed(2)}
                  </li>
                ))}
              </ul>
              <p><strong>Subtotal:</strong> ৳{detail.data.subtotalAmount.toFixed(2)}</p>
              <p><strong>Discount:</strong> ৳{detail.data.discountAmount.toFixed(2)}</p>
              <p><strong>Delivery:</strong> ৳{detail.data.deliveryFee.toFixed(2)}</p>
              <p><strong>Total:</strong> ৳{detail.data.totalAmount.toFixed(2)}</p>
            </div>
            <div>
              <h3>Fulfilment</h3>
              <p><strong>Customer:</strong> {detail.data.customerName}</p>
              <p><strong>Phone:</strong> {detail.data.contactPhone}</p>
              <p><strong>Address:</strong> {detail.data.deliveryAddress}</p>
              <p><strong>Notes:</strong> {detail.data.notes ?? '—'}</p>
              <p><strong>Rider:</strong> {detail.data.riderName ?? 'Not assigned'}</p>
              <h3>History</h3>
              <ul className="detail-list">
                {detail.data.events.map((event, index) => (
                  <li key={index}>{event.status} · {new Date(event.at).toLocaleString()}
                    {event.note ? ` — ${event.note}` : ''}</li>
                ))}
              </ul>
            </div>
          </div>

          <div className="form-grid">
            <label className="field"><span>Note</span><input value={note}
              onChange={(e) => setNote(e.target.value)} /></label>
            <div className="form-actions field--wide">
              {ADMIN_ACTIONS.filter((action) => detail.data?.status !== action).map((action) => (
                <button key={action} className="btn btn--small" disabled={busy}
                  onClick={() => run(() => orderApi.setStatus(selected, action, note || null))}>
                  {action.replace(/_/g, ' ')}
                </button>
              ))}
            </div>
          </div>

          {(detail.data.status === 'CONFIRMED' || detail.data.status === 'READY_FOR_PICKUP') && (
            <div className="form-grid">
              <label className="field"><span>Assign approved rider</span>
                <select value={riderId} onChange={(e) => setRiderId(e.target.value)}>
                  <option value="">Select rider…</option>
                  {riders.data?.map((rider) => (
                    <option key={rider.publicId} value={rider.publicId}>
                      {rider.fullName} · {rider.online ? 'online' : 'offline'}
                    </option>
                  ))}
                </select>
              </label>
              <div className="form-actions field--wide">
                <button className="btn btn--primary" disabled={busy || !riderId}
                  onClick={() => run(() => orderApi.assign(selected, riderId))}>
                  Assign rider
                </button>
              </div>
            </div>
          )}
        </section>
      )}
    </section>
  );
}
