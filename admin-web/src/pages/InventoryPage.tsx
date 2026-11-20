import { useState } from 'react';

import { DataTable } from '../components/ui/DataTable';
import { errorMessage, inventoryApi } from '../features/api/commerce';
import { useAsync } from '../hooks/useAsync';

/** Stock console: absolute set, relative adjustment and immutable history. */
export function InventoryPage() {
  const inventory = useAsync(() => inventoryApi.list());
  const [selected, setSelected] = useState<string | null>(null);
  const [quantity, setQuantity] = useState(0);
  const [threshold, setThreshold] = useState(5);
  const [delta, setDelta] = useState(0);
  const [reason, setReason] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const history = useAsync(
    () => selected ? inventoryApi.history(selected) : Promise.resolve([]),
    [selected],
  );

  async function run(action: () => Promise<unknown>) {
    setBusy(true);
    setError(null);
    try {
      await action();
      await inventory.reload();
      await history.reload();
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
          <h1 className="page__title">Inventory</h1>
          <p className="page__subtitle">Authoritative stock levels and adjustment history.</p>
        </div>
      </header>

      {error && <p className="alert alert--error">{error}</p>}

      <section className="panel">
        <div className="panel__header"><h2 className="panel__title">Stock levels</h2></div>
        {inventory.loading && <p className="panel__body">Loading…</p>}
        {inventory.error && <p className="alert alert--error">{errorMessage(inventory.error)}</p>}
        {inventory.data && (
          <DataTable<typeof inventory.data[number]>
            rows={inventory.data}
            rowKey={(row) => row.productPublicId ?? String(row.updatedAt)}
            emptyMessage="No inventory records yet. Create a product first."
            columns={[
              { key: 'productName', header: 'Product', render: (row) => (
                <div><strong>{row.productName ?? '—'}</strong>
                  {row.sku && <small className="muted"> · {row.sku}</small>}</div>
              ) },
              { key: 'quantityOnHand', header: 'On hand' },
              { key: 'reorderThreshold', header: 'Threshold' },
              { key: 'lowStock', header: 'Health', render: (row) => (
                <span className={row.lowStock ? 'text-danger' : 'text-success'}>
                  {row.lowStock ? 'LOW STOCK' : 'OK'}
                </span>
              ) },
              { key: 'actions', header: 'Actions', render: (row) => (
                <button className="btn btn--small" disabled={!row.productPublicId} onClick={() => {
                  setSelected(row.productPublicId);
                  setQuantity(row.quantityOnHand);
                  setThreshold(row.reorderThreshold);
                  setDelta(0);
                  setReason('');
                }}>Manage</button>
              ) },
            ]}
          />
        )}
      </section>

      {selected && (
        <section className="panel">
          <h2 className="panel__title">Manage stock · {inventory.data?.find((r) => r.productPublicId === selected)?.productName}</h2>
          <div className="form-grid">
            <label className="field"><span>Set quantity on hand</span><input type="number" min="0"
              value={quantity} onChange={(e) => setQuantity(Number(e.target.value))} /></label>
            <label className="field"><span>Reorder threshold</span><input type="number" min="0"
              value={threshold} onChange={(e) => setThreshold(Number(e.target.value))} /></label>
            <div className="form-actions field--wide">
              <button className="btn btn--primary" disabled={busy} onClick={() => run(
                () => inventoryApi.setStock(selected, quantity, threshold))}>Save stock</button>
            </div>
            <label className="field"><span>Adjust by (+/-)</span><input type="number"
              value={delta} onChange={(e) => setDelta(Number(e.target.value))} /></label>
            <label className="field"><span>Reason</span><input value={reason}
              onChange={(e) => setReason(e.target.value)} placeholder="e.g. stock count" /></label>
            <div className="form-actions field--wide">
              <button className="btn btn--ghost" disabled={busy || !reason.trim()} onClick={() => run(
                () => inventoryApi.adjust(selected, delta, reason.trim()))}>Apply adjustment</button>
            </div>
          </div>
        </section>
      )}

      {selected && (
        <section className="panel">
          <div className="panel__header"><h2 className="panel__title">Adjustment history</h2></div>
          {history.loading && <p className="panel__body">Loading…</p>}
          {history.data && (
            <DataTable<typeof history.data[number]>
              rows={history.data}
              rowKey={(row) => row.publicId}
              emptyMessage="No adjustments recorded."
              columns={[
                { key: 'createdAt', header: 'When', render: (row) => new Date(row.createdAt).toLocaleString() },
                { key: 'delta', header: 'Change', render: (row) => row.delta > 0 ? `+${row.delta}` : row.delta },
                { key: 'quantityAfter', header: 'New level' },
                { key: 'reason', header: 'Reason' },
                { key: 'reference', header: 'Reference', render: (row) => row.reference ?? '—' },
              ]}
            />
          )}
        </section>
      )}
    </section>
  );
}
