import { useState, type FormEvent } from 'react';

import { DataTable } from '../components/ui/DataTable';
import { StatusBadge } from '../components/ui/StatusBadge';
import { categoryApi, errorMessage, productApi, type ProductInput } from '../features/api/commerce';
import { useAsync } from '../hooks/useAsync';
import type { Product } from '../types';

const EMPTY: ProductInput = {
  name: '',
  description: '',
  sku: '',
  categoryId: null,
  basePrice: 0,
  discountPrice: null,
  imageUrl: '',
  available: true,
};

/** Product management: search, filter, create, edit and delete against the DB. */
export function ProductsPage() {
  const [search, setSearch] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [availableOnly, setAvailableOnly] = useState('');
  const [editing, setEditing] = useState<Product | null>(null);
  const [form, setForm] = useState<ProductInput>(EMPTY);
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [showForm, setShowForm] = useState(false);

  const categories = useAsync(() => categoryApi.list());
  const products = useAsync(() => productApi.list({
    search: search || undefined,
    categoryId: categoryId ? Number(categoryId) : undefined,
    availableOnly: availableOnly === '' ? undefined : availableOnly === 'true',
  }), [search, categoryId, availableOnly]);

  function openCreate() {
    setEditing(null);
    setForm(EMPTY);
    setFormError(null);
    setShowForm(true);
  }

  function openEdit(product: Product) {
    setEditing(product);
    setForm({
      name: product.name,
      description: product.description ?? '',
      sku: product.sku ?? '',
      categoryId: product.categoryId,
      basePrice: product.basePrice,
      discountPrice: product.discountPrice,
      imageUrl: product.imageUrl ?? '',
      available: product.available,
    });
    setFormError(null);
    setShowForm(true);
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!form.name.trim() || form.basePrice <= 0) {
      setFormError('Name is required and base price must be greater than zero.');
      return;
    }
    if (form.discountPrice !== null && form.discountPrice >= form.basePrice) {
      setFormError('Discount price must be lower than base price.');
      return;
    }
    const payload: ProductInput = {
      ...form,
      name: form.name.trim(),
      description: form.description?.trim() || null,
      sku: form.sku?.trim() || null,
      imageUrl: form.imageUrl?.trim() || null,
      categoryId: form.categoryId,
    };
    setSaving(true);
    setFormError(null);
    try {
      if (editing) await productApi.update(editing.publicId, payload);
      else await productApi.create(payload);
      setShowForm(false);
      await products.reload();
    } catch (error) {
      setFormError(errorMessage(error));
    } finally {
      setSaving(false);
    }
  }

  async function remove(product: Product) {
    if (!window.confirm(`Delete "${product.name}"? It can no longer be ordered.`)) return;
    try {
      await productApi.remove(product.publicId);
      await products.reload();
    } catch (error) {
      setFormError(errorMessage(error));
    }
  }

  return (
    <section className="page">
      <header className="page__header">
        <div>
          <h1 className="page__title">Products</h1>
          <p className="page__subtitle">Manage the catalogue, pricing and availability.</p>
        </div>
        <button className="btn btn--primary" type="button" onClick={openCreate}>New product</button>
      </header>

      <section className="panel">
        <div className="filter-bar">
          <label className="field">
            <span>Search</span>
            <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Product name" />
          </label>
          <label className="field">
            <span>Category</span>
            <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
              <option value="">All categories</option>
              {categories.data?.map((category) => (
                <option key={category.publicId} value={category.id}>
                  {category.name}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>Availability</span>
            <select value={availableOnly} onChange={(e) => setAvailableOnly(e.target.value)}>
              <option value="">All</option>
              <option value="true">Available</option>
              <option value="false">Unavailable</option>
            </select>
          </label>
        </div>
      </section>

      {formError && !showForm && <p className="alert alert--error">{formError}</p>}
      {products.error && <p className="alert alert--error">{errorMessage(products.error)}</p>}

      {showForm && (
        <section className="panel">
          <h2 className="panel__title">{editing ? 'Edit product' : 'Create product'}</h2>
          <form className="form-grid" onSubmit={submit}>
            <label className="field"><span>Name *</span><input value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })} /></label>
            <label className="field"><span>SKU</span><input value={form.sku ?? ''}
              onChange={(e) => setForm({ ...form, sku: e.target.value })} /></label>
            <label className="field field--wide"><span>Description</span><textarea value={form.description ?? ''}
              onChange={(e) => setForm({ ...form, description: e.target.value })} rows={2} /></label>
            <label className="field"><span>Category</span><select
              value={form.categoryId ?? ''}
              onChange={(e) => setForm({ ...form, categoryId: e.target.value ? Number(e.target.value) : null })}>
              <option value="">None</option>
              {categories.data?.map((category) => (
                <option key={category.publicId} value={category.id}>{category.name}</option>
              ))}
            </select></label>
            <label className="field"><span>Base price (৳) *</span><input type="number" min="0.01" step="0.01"
              value={form.basePrice}
              onChange={(e) => setForm({ ...form, basePrice: Number(e.target.value) })} /></label>
            <label className="field"><span>Discount price (৳)</span><input type="number" min="0" step="0.01"
              value={form.discountPrice ?? ''}
              onChange={(e) => setForm({ ...form, discountPrice: e.target.value ? Number(e.target.value) : null })} /></label>
            <label className="field field--wide"><span>Image URL</span><input value={form.imageUrl ?? ''}
              onChange={(e) => setForm({ ...form, imageUrl: e.target.value })} /></label>
            <label className="field checkbox"><input type="checkbox" checked={form.available}
              onChange={(e) => setForm({ ...form, available: e.target.checked })} /><span>Available</span></label>
            {formError && <p className="alert alert--error field--wide">{formError}</p>}
            <div className="form-actions field--wide">
              <button className="btn btn--ghost" type="button" onClick={() => setShowForm(false)}>Cancel</button>
              <button className="btn btn--primary" type="submit" disabled={saving}>
                {saving ? 'Saving…' : 'Save product'}
              </button>
            </div>
          </form>
        </section>
      )}

      <section className="panel">
        <div className="panel__header">
          <h2 className="panel__title">Catalogue ({products.data?.length ?? 0})</h2>
        </div>
        {products.loading && <p className="panel__body">Loading…</p>}
        {!products.loading && (products.data?.length ?? 0) === 0 && (
          <p className="panel__body">No products match these filters.</p>
        )}
        {products.data && products.data.length > 0 && (
          <DataTable<Product>
            rows={products.data}
            rowKey={(row) => row.publicId}
            columns={[
              { key: 'name', header: 'Product', render: (row) => (
                <div>
                  <strong>{row.name}</strong>
                  {row.sku && <small className="muted"> · {row.sku}</small>}
                </div>
              ) },
              { key: 'categoryName', header: 'Category', render: (row) => row.categoryName ?? '—' },
              { key: 'effectivePrice', header: 'Price', render: (row) => (
                <span>{row.effectivePrice.toFixed(2)}
                  {row.discountPrice !== null && <small className="muted"> (was {row.basePrice.toFixed(2)})</small>}
                </span>
              ) },
              { key: 'stockQuantity', header: 'Stock', render: (row) => (
                <span className={row.stockQuantity <= 5 ? 'text-warning' : ''}>{row.stockQuantity}</span>
              ) },
              { key: 'available', header: 'Status', render: (row) => (
                row.available ? <StatusBadge status="CONFIRMED" /> : <StatusBadge status="CANCELLED" />
              ) },
              { key: 'actions', header: 'Actions', render: (row) => (
                <div className="row-actions">
                  <button className="btn btn--small" onClick={() => openEdit(row)}>Edit</button>
                  <button className="btn btn--small btn--danger" onClick={() => remove(row)}>Delete</button>
                </div>
              ) },
            ]}
          />
        )}
      </section>
    </section>
  );
}
