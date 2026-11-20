import { useState, type FormEvent } from 'react';

import { DataTable } from '../components/ui/DataTable';
import { StatusBadge } from '../components/ui/StatusBadge';
import { categoryApi, errorMessage } from '../features/api/commerce';
import { useAsync } from '../hooks/useAsync';
import type { Category } from '../types';

/** Category CRUD with safe deletion (backend blocks deletion while in use). */
export function CategoriesPage() {
  const categories = useAsync(() => categoryApi.list());
  const [form, setForm] = useState({ name: '', description: '', imageUrl: '', active: true });
  const [editing, setEditing] = useState<Category | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!form.name.trim()) {
      setError('Category name is required.');
      return;
    }
    const payload = {
      name: form.name.trim(),
      description: form.description.trim() || null,
      imageUrl: form.imageUrl.trim() || null,
      active: form.active,
    };
    setSaving(true);
    setError(null);
    try {
      if (editing) await categoryApi.update(editing.publicId, payload);
      else await categoryApi.create(payload);
      setEditing(null);
      setForm({ name: '', description: '', imageUrl: '', active: true });
      await categories.reload();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  async function remove(category: Category) {
    if (!window.confirm(`Delete category "${category.name}"?`)) return;
    try {
      await categoryApi.remove(category.publicId);
      await categories.reload();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <section className="page">
      <header className="page__header">
        <div>
          <h1 className="page__title">Categories</h1>
          <p className="page__subtitle">Organise the product catalogue.</p>
        </div>
      </header>

      <section className="panel">
        <h2 className="panel__title">{editing ? 'Edit category' : 'New category'}</h2>
        <form className="form-grid" onSubmit={submit}>
          <label className="field"><span>Name *</span><input value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })} /></label>
          <label className="field"><span>Image URL</span><input value={form.imageUrl}
            onChange={(e) => setForm({ ...form, imageUrl: e.target.value })} /></label>
          <label className="field field--wide"><span>Description</span><input value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })} /></label>
          <label className="field checkbox"><input type="checkbox" checked={form.active}
            onChange={(e) => setForm({ ...form, active: e.target.checked })} /><span>Active</span></label>
          {error && <p className="alert alert--error field--wide">{error}</p>}
          <div className="form-actions field--wide">
            {editing && <button className="btn btn--ghost" type="button"
              onClick={() => { setEditing(null); setForm({ name: '', description: '', imageUrl: '', active: true }); }}>
              Cancel
            </button>}
            <button className="btn btn--primary" type="submit" disabled={saving}>
              {saving ? 'Saving…' : editing ? 'Update category' : 'Create category'}
            </button>
          </div>
        </form>
      </section>

      <section className="panel">
        <div className="panel__header"><h2 className="panel__title">All categories</h2></div>
        {categories.loading && <p className="panel__body">Loading…</p>}
        {categories.error && <p className="alert alert--error">{errorMessage(categories.error)}</p>}
        {categories.data && (
          <DataTable<Category>
            rows={categories.data}
            rowKey={(row) => row.publicId}
            emptyMessage="No categories yet."
            columns={[
              { key: 'name', header: 'Name' },
              { key: 'description', header: 'Description', render: (row) => row.description ?? '—' },
              { key: 'active', header: 'Status', render: (row) => (
                row.active ? <StatusBadge status="CONFIRMED" /> : <StatusBadge status="CANCELLED" />
              ) },
              { key: 'actions', header: 'Actions', render: (row) => (
                <div className="row-actions">
                  <button className="btn btn--small" onClick={() => {
                    setEditing(row);
                    setForm({ name: row.name, description: row.description ?? '',
                      imageUrl: row.imageUrl ?? '', active: row.active });
                  }}>Edit</button>
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
