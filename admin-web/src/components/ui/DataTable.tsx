import type { ReactNode } from 'react';

interface DataTableProps<T> {
  columns: ReadonlyArray<{ key: string; header: string; render?: (row: T) => ReactNode }>;
  rows: readonly T[];
  rowKey: (row: T) => string;
  emptyMessage?: string;
}

/**
 * Generic table used across admin feature modules.
 */
export function DataTable<T>({
  columns,
  rows,
  rowKey,
  emptyMessage = 'No records found.',
}: DataTableProps<T>) {
  return (
    <div className="table-wrapper">
      <table className="data-table">
        <thead>
          <tr>
            {columns.map((column) => (
              <th key={column.key} scope="col">
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.length === 0 ? (
            <tr>
              <td className="data-table__empty" colSpan={columns.length}>
                {emptyMessage}
              </td>
            </tr>
          ) : (
            rows.map((row) => (
              <tr key={rowKey(row)}>
                {columns.map((column) => (
                  <td key={column.key}>
                    {column.render
                      ? column.render(row)
                      : String((row as Record<string, unknown>)[column.key] ?? '')}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
