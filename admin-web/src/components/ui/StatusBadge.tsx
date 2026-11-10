import type { OrderStatus } from '../../types';

interface StatusBadgeProps {
  status: OrderStatus;
}

/**
 * Coloured badge for an order status.
 */
export function StatusBadge({ status }: StatusBadgeProps) {
  const tone = STATUS_TONE[status] ?? 'neutral';

  return <span className={`badge badge--${tone}`}>{status.replace(/_/g, ' ')}</span>;
}

const STATUS_TONE: Record<OrderStatus, string> = {
  PENDING: 'warning',
  CONFIRMED: 'info',
  PREPARING: 'info',
  READY_FOR_PICKUP: 'info',
  ASSIGNED: 'info',
  PICKED_UP: 'accent',
  OUT_FOR_DELIVERY: 'accent',
  DELIVERED: 'success',
  CANCELLED: 'neutral',
  REJECTED: 'danger',
  FAILED_DELIVERY: 'danger',
};
