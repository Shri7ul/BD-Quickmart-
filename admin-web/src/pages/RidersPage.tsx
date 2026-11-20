import { useState } from 'react';

import { DataTable } from '../components/ui/DataTable';
import { StatusBadge } from '../components/ui/StatusBadge';
import { errorMessage, riderApi } from '../features/api/commerce';
import { useAsync } from '../hooks/useAsync';
import type { RiderEligibility } from '../types';

/** Rider approval queue and fleet overview. */
export function RidersPage() {
  const riders = useAsync(() => riderApi.all());
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState<string | null>(null);

  async function act(publicId: string, action: 'approve' | 'reject') {
    setBusy(publicId);
    setError(null);
    try {
      if (action === 'approve') await riderApi.approve(publicId);
      else await riderApi.reject(publicId);
      await riders.reload();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(null);
    }
  }

  const pending = (riders.data ?? []).filter((rider) => rider.status === 'PENDING');

  return (
    <section className="page">
      <header className="page__header">
        <div>
          <h1 className="page__title">Riders</h1>
          <p className="page__subtitle">Approve new riders and review fleet availability.</p>
        </div>
      </header>

      {error && <p className="alert alert--error">{error}</p>}
      {riders.error && <p className="alert alert--error">{errorMessage(riders.error)}</p>}

      <section className="panel">
        <div className="panel__header"><h2 className="panel__title">Pending approval ({pending.length})</h2></div>
        {riders.loading && <p className="panel__body">Loading…</p>}
        {!riders.loading && pending.length === 0 && (
          <p className="panel__body">No riders are waiting for approval.</p>
        )}
        {pending.length > 0 && (
          <DataTable<RiderEligibility>
            rows={pending}
            rowKey={(row) => row.publicId}
            columns={[
              { key: 'fullName', header: 'Name' },
              { key: 'phone', header: 'Phone' },
              { key: 'status', header: 'Status', render: () => <StatusBadge status="PENDING" /> },
              { key: 'actions', header: 'Decision', render: (row) => (
                <div className="row-actions">
                  <button className="btn btn--small btn--primary" disabled={busy === row.publicId}
                    onClick={() => act(row.publicId, 'approve')}>Approve</button>
                  <button className="btn btn--small btn--danger" disabled={busy === row.publicId}
                    onClick={() => act(row.publicId, 'reject')}>Reject</button>
                </div>
              ) },
            ]}
          />
        )}
      </section>

      <section className="panel">
        <div className="panel__header"><h2 className="panel__title">All riders ({riders.data?.length ?? 0})</h2></div>
        {riders.data && (
          <DataTable<RiderEligibility>
            rows={riders.data}
            rowKey={(row) => row.publicId}
            emptyMessage="No rider accounts yet."
            columns={[
              { key: 'fullName', header: 'Name' },
              { key: 'phone', header: 'Phone' },
              { key: 'status', header: 'Approval', render: (row) => (
                row.status === 'ACTIVE' ? <StatusBadge status="DELIVERED" />
                  : row.status === 'PENDING' ? <StatusBadge status="PENDING" />
                    : <StatusBadge status="CANCELLED" />
              ) },
              { key: 'online', header: 'Availability', render: (row) => (
                <span className={row.online ? 'text-success' : 'text-muted'}>
                  {row.online ? 'Online' : 'Offline'}
                </span>
              ) },
            ]}
          />
        )}
      </section>
    </section>
  );
}
