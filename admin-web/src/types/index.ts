/**
 * Shared domain types for the admin web application.
 *
 * These mirror the backend contracts introduced by the goals that own each
 * feature. They are intentionally narrow for Goal 01 and expand as the
 * corresponding modules are implemented.
 */

/**
 * The six canonical roles defined by the backend auth module (Goal 02).
 * Identifiers are shared verbatim with the database, JWT claims and apps.
 */
export type UserRole =
  | 'CUSTOMER'
  | 'RIDER'
  | 'ADMIN'
  | 'SUPER_ADMIN'
  | 'STORE_MANAGER'
  | 'INVENTORY_OPERATOR';

/** Account lifecycle state (Goal 02). */
export type AccountStatus = 'ACTIVE' | 'PENDING' | 'REJECTED' | 'DISABLED';

/** Authenticated operator profile, mirroring the backend UserResponse. */
export interface AuthenticatedUser {
  publicId: string;
  fullName: string;
  email: string;
  phone: string;
  role: UserRole;
  status: AccountStatus;
}

/** Login accepts either an email address or a phone number. */
export interface LoginRequest {
  identifier: string;
  password: string;
}

/** Token pair returned by POST /auth/login and POST /auth/refresh. */
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
  user: AuthenticatedUser;
}

/** Order lifecycle status. */
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

/** Compact order row shown in the admin order table. */
export interface OrderSummary {
  id: string;
  customerName: string;
  storeName: string;
  totalAmount: number;
  status: OrderStatus;
  createdAt: string;
}

/** Dark store entity. */
export interface Store {
  id: string;
  name: string;
  address: string;
  serviceRadiusKm: number;
  isActive: boolean;
}

/** Product entity. */
export interface Product {
  id: string;
  name: string;
  nameBn: string;
  sku: string;
  categoryId: string;
  basePrice: number;
  isAvailable: boolean;
}

/** Inventory position for a product at a given store. */
export interface InventoryLevel {
  productId: string;
  storeId: string;
  quantityOnHand: number;
  reorderThreshold: number;
  nearestExpiryDate: string | null;
}

/** Rider entity. */
export interface Rider {
  id: string;
  fullName: string;
  phone: string;
  status: 'OFFLINE' | 'ONLINE' | 'BUSY';
  completedDeliveries: number;
}

/** Aggregated dashboard metrics. */
export interface DashboardSummary {
  totalOrders: number;
  totalRevenue: number;
  activeRiders: number;
  lowStockItems: number;
}
