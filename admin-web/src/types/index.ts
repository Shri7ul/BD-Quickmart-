/**
 * Shared domain types for the admin web application.
 *
 * These mirror the Phase 1 backend contracts exactly: external IDs are
 * publicId strings, money comes back as JSON numbers from NUMERIC(12,2),
 * and order status uses the canonical server-defined lifecycle.
 */

export type UserRole =
  | 'CUSTOMER'
  | 'RIDER'
  | 'ADMIN'
  | 'SUPER_ADMIN'
  | 'STORE_MANAGER'
  | 'INVENTORY_OPERATOR';

export type AccountStatus = 'ACTIVE' | 'PENDING' | 'REJECTED' | 'DISABLED';

export interface AuthenticatedUser {
  publicId: string;
  fullName: string;
  email: string;
  phone: string;
  role: UserRole;
  status: AccountStatus;
}

export interface LoginRequest {
  identifier: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
  user: AuthenticatedUser;
}

export type OrderStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'PREPARING'
  | 'READY_FOR_PICKUP'
  | 'ASSIGNED'
  | 'PICKED_UP'
  | 'OUT_FOR_DELIVERY'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'REJECTED'
  | 'FAILED_DELIVERY';

export interface Category {
  id: number;
  publicId: string;
  name: string;
  description: string | null;
  imageUrl: string | null;
  active: boolean;
  createdAt: string;
}

export interface Product {
  publicId: string;
  name: string;
  description: string | null;
  sku: string | null;
  categoryId: number | null;
  categoryName: string | null;
  basePrice: number;
  discountPrice: number | null;
  effectivePrice: number;
  imageUrl: string | null;
  available: boolean;
  stockQuantity: number;
  inStock: boolean;
}

export interface InventoryRow {
  productPublicId: string | null;
  productName: string | null;
  sku: string | null;
  quantityOnHand: number;
  reorderThreshold: number;
  lowStock: boolean;
  updatedAt: string;
}

export interface Adjustment {
  publicId: string;
  delta: number;
  quantityAfter: number;
  reason: string;
  reference: string | null;
  createdAt: string;
}

export interface OrderItem {
  productPublicId: string;
  productName: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface OrderEvent {
  status: OrderStatus;
  at: string;
  note: string | null;
}

export interface Order {
  publicId: string;
  status: OrderStatus;
  subtotalAmount: number;
  discountAmount: number;
  deliveryFee: number;
  totalAmount: number;
  paymentMethod: string;
  deliveryAddress: string;
  contactPhone: string;
  notes: string | null;
  customerPublicId: string | null;
  customerName: string;
  riderPublicId: string | null;
  riderName: string | null;
  items: OrderItem[];
  events: OrderEvent[];
  createdAt: string;
  updatedAt: string;
}

export interface RiderEligibility {
  publicId: string;
  fullName: string;
  phone: string;
  status: AccountStatus;
  online: boolean;
}

export interface DashboardSummary {
  totalOrders: number;
  pendingOrders: number;
  revenue: number;
  activeRiders: number;
  lowStockItems: number;
  recentOrders: Array<{
    publicId: string;
    customerName: string;
    status: OrderStatus;
    totalAmount: number;
    createdAt: string;
  }>;
}
