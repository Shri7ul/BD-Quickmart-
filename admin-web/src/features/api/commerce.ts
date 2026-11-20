/**
 * Typed API layer for all Phase 1 admin operations.
 *
 * Every call goes through the shared ApiClient, so the bearer token, silent
 * refresh and the standard ApiResponse envelope are handled in one place.
 */
import { apiClient } from '../../lib/api/client';
import type {
  Adjustment,
  Category,
  DashboardSummary,
  InventoryRow,
  Order,
  Product,
  RiderEligibility,
} from '../../types';

const base = '/admin';

export interface ProductQuery {
  search?: string;
  categoryId?: number;
  availableOnly?: boolean;
}

export const categoryApi = {
  list: (activeOnly = false) =>
    apiClient.get<Category[]>(`${base}/categories`, { activeOnly }),
  get: (publicId: string) => apiClient.get<Category>(`${base}/categories/${publicId}`),
  create: (body: Omit<Category, 'id' | 'publicId' | 'createdAt'>) =>
    apiClient.post<Category>(`${base}/categories`, body),
  update: (publicId: string, body: Omit<Category, 'id' | 'publicId' | 'createdAt'>) =>
    apiClient.put<Category>(`${base}/categories/${publicId}`, body),
  remove: (publicId: string) => apiClient.delete<void>(`${base}/categories/${publicId}`),
};

export const productApi = {
  list: (query: ProductQuery = {}) =>
    apiClient.get<Product[]>(`${base}/products`, query as Record<string, unknown>),
  get: (publicId: string) => apiClient.get<Product>(`${base}/products/${publicId}`),
  create: (body: ProductInput) => apiClient.post<Product>(`${base}/products`, body),
  update: (publicId: string, body: ProductInput) =>
    apiClient.put<Product>(`${base}/products/${publicId}`, body),
  remove: (publicId: string) => apiClient.delete<void>(`${base}/products/${publicId}`),
};

export interface ProductInput {
  name: string;
  description: string | null;
  sku: string | null;
  categoryId: number | null;
  basePrice: number;
  discountPrice: number | null;
  imageUrl: string | null;
  available: boolean;
}

export const inventoryApi = {
  list: () => apiClient.get<InventoryRow[]>(`${base}/inventory`),
  get: (productPublicId: string) =>
    apiClient.get<InventoryRow>(`${base}/inventory/${productPublicId}`),
  setStock: (productPublicId: string, quantityOnHand: number, reorderThreshold: number) =>
    apiClient.put<InventoryRow>(`${base}/inventory/${productPublicId}/stock`, {
      quantityOnHand,
      reorderThreshold,
    }),
  adjust: (productPublicId: string, delta: number, reason: string) =>
    apiClient.post<InventoryRow>(`${base}/inventory/${productPublicId}/adjust`, { delta, reason }),
  history: (productPublicId: string) =>
    apiClient.get<Adjustment[]>(`${base}/inventory/${productPublicId}/history`),
};

export const orderApi = {
  list: () => apiClient.get<Order[]>(`${base}/orders`),
  get: (publicId: string) => apiClient.get<Order>(`${base}/orders/${publicId}`),
  setStatus: (publicId: string, status: string, note: string | null) =>
    apiClient.post<Order>(`${base}/orders/${publicId}/status`, { status, note }),
  assign: (publicId: string, riderPublicId: string) =>
    apiClient.post<Order>(`${base}/orders/${publicId}/assign`, { riderPublicId }),
  availableAssignments: () => apiClient.get<Order[]>(`${base}/orders/assignments/available`),
  dashboard: () => apiClient.get<DashboardSummary>(`${base}/dashboard/summary`),
};

export const riderApi = {
  all: () => apiClient.get<RiderEligibility[]>(`${base}/riders`),
  eligible: () => apiClient.get<RiderEligibility[]>(`${base}/riders/eligible`),
  pending: () => apiClient.get<RiderEligibility[]>(`${base}/riders/pending`),
  approve: (publicId: string) => apiClient.post<void>(`${base}/riders/${publicId}/approve`),
  reject: (publicId: string) => apiClient.post<void>(`${base}/riders/${publicId}/reject`),
};

/** Human-readable API errors for inline display. */
export function errorMessage(error: unknown): string {
  if (error instanceof Error) return error.message;
  return 'Unable to complete the request.';
}
