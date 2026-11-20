package com.bdquickmart.modules.order;

import com.bdquickmart.common.api.ApiCode;
import com.bdquickmart.common.exception.BadRequestException;
import com.bdquickmart.common.exception.BusinessException;
import com.bdquickmart.common.exception.ResourceNotFoundException;
import com.bdquickmart.modules.catalog.ProductEntity;
import com.bdquickmart.modules.catalog.ProductRepository;
import com.bdquickmart.modules.inventory.InventoryService;
import com.bdquickmart.modules.order.dto.AssignRiderRequest;
import com.bdquickmart.modules.order.dto.CartItemRequest;
import com.bdquickmart.modules.order.dto.CheckoutRequest;
import com.bdquickmart.modules.order.dto.DashboardSummaryResponse;
import com.bdquickmart.modules.order.dto.OrderItemResponse;
import com.bdquickmart.modules.order.dto.OrderResponse;
import com.bdquickmart.modules.order.dto.OrderStatusEventResponse;
import com.bdquickmart.modules.order.dto.StatusUpdateRequest;
import com.bdquickmart.modules.rider.RiderAvailabilityRepository;
import com.bdquickmart.modules.user.AccountStatus;
import com.bdquickmart.modules.user.UserEntity;
import com.bdquickmart.modules.user.UserRepository;
import com.bdquickmart.modules.user.UserRole;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Order lifecycle, authoritative pricing, idempotent checkout and all
 * server-side business rules. Every stock mutation happens in the SAME
 * transaction as order creation and is serialised per product.
 */
@Service
@Transactional(readOnly = true)
public class OrderService {

    private static final BigDecimal DELIVERY_FEE = new BigDecimal("40.00");
    private static final BigDecimal FREE_DELIVERY_THRESHOLD = new BigDecimal("1000.00");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusEventRepository eventRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final RiderAvailabilityRepository availabilityRepository;
    private final Clock clock;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        OrderStatusEventRepository eventRepository,
                        ProductRepository productRepository,
                        UserRepository userRepository,
                        InventoryService inventoryService,
                        RiderAvailabilityRepository availabilityRepository,
                        Clock clock) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.eventRepository = eventRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.inventoryService = inventoryService;
        this.availabilityRepository = availabilityRepository;
        this.clock = clock;
    }

    // ------------------------------------------------------------------
    // Checkout
    // ------------------------------------------------------------------

    @Transactional
    public OrderResponse checkout(String customerPublicId, CheckoutRequest request) {
        UserEntity customer = userRepository.findByPublicId(customerPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "publicId", customerPublicId));

        // Idempotent retry returns the original order instead of a duplicate.
        OrderEntity existing = orderRepository.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
        if (existing != null) {
            if (!existing.getCustomerId().equals(customer.getId())) {
                throw new BusinessException(ApiCode.CONFLICT,
                        "Idempotency key belongs to a different customer");
            }
            return toResponse(existing);
        }

        Map<Long, Integer> quantities = mergeQuantities(request.items());
        if (quantities.isEmpty()) {
            throw new BadRequestException("Cart must contain at least one item");
        }

        Instant now = clock.instant();
        List<Line> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            ProductEntity product = productRepository.findById(entry.getKey())
                    .filter(p -> p.getDeletedAt() == null)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id",
                            String.valueOf(entry.getKey())));
            if (!product.isAvailable()) {
                throw new BusinessException(ApiCode.CONFLICT,
                        "Product is unavailable: " + product.getName());
            }
            int quantity = entry.getValue();
            // Lock + deduct in this transaction: no overselling, no double deduction.
            inventoryService.deductForOrder(product.getId(), quantity, request.idempotencyKey(), now);
            BigDecimal unitPrice = product.effectivePrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            if (product.getDiscountPrice() != null) {
                discount = discount.add(product.getBasePrice().subtract(unitPrice)
                        .multiply(BigDecimal.valueOf(quantity)));
            }
            subtotal = subtotal.add(product.getBasePrice().multiply(BigDecimal.valueOf(quantity)));
            lines.add(new Line(product, quantity, unitPrice, lineTotal));
        }

        BigDecimal deliveryFee = subtotal.compareTo(FREE_DELIVERY_THRESHOLD) >= 0
                ? BigDecimal.ZERO : DELIVERY_FEE;
        BigDecimal total = subtotal.subtract(discount).add(deliveryFee);

        OrderEntity order = new OrderEntity(UUID.randomUUID().toString(), request.idempotencyKey(),
                customer.getId(), request.deliveryAddress().trim(), request.contactPhone().trim(),
                request.notes(), subtotal, discount, deliveryFee, total);
        OrderEntity saved = orderRepository.saveAndFlush(order);

        for (Line line : lines) {
            orderItemRepository.save(new OrderItemEntity(saved.getId(), line.product().getId(),
                    line.product().getPublicId(), line.product().getName(), line.unitPrice(),
                    line.quantity(), line.lineTotal()));
        }
        eventRepository.save(new OrderStatusEventEntity(saved.getId(), UUID.randomUUID().toString(),
                null, OrderStatus.PENDING, "Order placed", now));
        return toResponse(saved);
    }

    private Map<Long, Integer> mergeQuantities(List<CartItemRequest> items) {
        Map<Long, Integer> merged = new LinkedHashMap<>();
        for (CartItemRequest item : items) {
            ProductEntity product = productRepository.findByPublicId(item.productPublicId())
                    .filter(p -> p.getDeletedAt() == null)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "publicId",
                            item.productPublicId()));
            merged.merge(product.getId(), item.quantity(), Integer::sum);
        }
        return merged;
    }

    private record Line(ProductEntity product, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    public List<OrderResponse> listAll() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    public List<OrderResponse> listForCustomer(String customerPublicId) {
        UserEntity customer = userRepository.findByPublicId(customerPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "publicId", customerPublicId));
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())
                .stream().map(this::toResponse).toList();
    }

    public List<OrderResponse> listForRider(String riderPublicId) {
        UserEntity rider = userRepository.findByPublicId(riderPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "publicId", riderPublicId));
        return orderRepository.findByRiderIdOrderByCreatedAtDesc(rider.getId())
                .stream().map(this::toResponse).toList();
    }

    /** Customers may only read their own order; admins/riders may read by id. */
    public OrderResponse getOrder(String publicId, String requesterPublicId, boolean privileged) {
        OrderEntity order = findOrder(publicId);
        if (!privileged) {
            UserEntity requester = userRepository.findByPublicId(requesterPublicId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "publicId", requesterPublicId));
            if (!order.getCustomerId().equals(requester.getId())) {
                throw new BusinessException(ApiCode.FORBIDDEN, "You cannot access this order");
            }
        }
        return toResponse(order);
    }

    private OrderEntity findOrder(String publicId) {
        return orderRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "publicId", publicId));
    }

    // ------------------------------------------------------------------
    // Status transitions
    // ------------------------------------------------------------------

    @Transactional
    public OrderResponse transition(String publicId, StatusUpdateRequest request, boolean riderAction) {
        OrderEntity order = findOrder(publicId);
        OrderStatus target;
        try {
            target = OrderStatus.valueOf(request.status().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unknown order status: " + request.status());
        }
        if (riderAction && target != OrderStatus.PICKED_UP && target != OrderStatus.OUT_FOR_DELIVERY
                && target != OrderStatus.DELIVERED && target != OrderStatus.FAILED_DELIVERY) {
            throw new BusinessException(ApiCode.FORBIDDEN,
                    "Riders may only update pickup and delivery statuses");
        }
        if (!riderAction && (target == OrderStatus.PICKED_UP || target == OrderStatus.OUT_FOR_DELIVERY
                || target == OrderStatus.DELIVERED || target == OrderStatus.FAILED_DELIVERY)) {
            throw new BusinessException(ApiCode.FORBIDDEN,
                    "Delivery statuses can only be updated by the assigned rider");
        }
        OrderStatus previous = order.getStatus();
        if (!previous.canTransitionTo(target)) {
            throw new BusinessException(ApiCode.CONFLICT,
                    "Invalid order status transition: " + previous + " -> " + target);
        }
        Instant now = clock.instant();
        order.transitionTo(target, now, order.getRiderId());
        orderRepository.save(order);
        if (target.isCancellation() && !previous.isTerminal()) {
            for (OrderItemEntity item : orderItemRepository.findByOrderId(order.getId())) {
                inventoryService.restoreForCancellation(item.getProductId(), item.getQuantity(),
                        order.getPublicId(), now);
            }
        }
        eventRepository.save(new OrderStatusEventEntity(order.getId(), UUID.randomUUID().toString(),
                previous, target, request.note(), now));
        return toResponse(order);
    }

    // ------------------------------------------------------------------
    // Rider assignment
    // ------------------------------------------------------------------

    @Transactional
    public OrderResponse assignRider(String publicId, AssignRiderRequest request) {
        OrderEntity order = findOrder(publicId);
        if (order.getStatus() != OrderStatus.READY_FOR_PICKUP && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BusinessException(ApiCode.CONFLICT,
                    "Only confirmed or ready-for-pickup orders can be assigned");
        }
        UserEntity rider = userRepository.findByPublicId(request.riderPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Rider", "publicId",
                        request.riderPublicId()));
        if (rider.getRole() != UserRole.RIDER || rider.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(ApiCode.CONFLICT, "Only approved, active riders can be assigned");
        }
        long activeDeliveries = orderRepository.countByRiderIdAndStatus(rider.getId(), OrderStatus.ASSIGNED)
                + orderRepository.countByRiderIdAndStatus(rider.getId(), OrderStatus.PICKED_UP)
                + orderRepository.countByRiderIdAndStatus(rider.getId(), OrderStatus.OUT_FOR_DELIVERY);
        if (activeDeliveries > 0) {
            throw new BusinessException(ApiCode.CONFLICT,
                    "Rider already has an active delivery assignment");
        }
        Instant now = clock.instant();
        OrderStatus previous = order.getStatus();
        order.transitionTo(OrderStatus.ASSIGNED, now, rider.getId());
        orderRepository.save(order);
        eventRepository.save(new OrderStatusEventEntity(order.getId(), UUID.randomUUID().toString(),
                previous, OrderStatus.ASSIGNED, "Rider assigned: " + rider.getFullName(), now));
        return toResponse(order);
    }

    public List<OrderResponse> availableAssignments() {
        return orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.READY_FOR_PICKUP)
                .stream().map(this::toResponse).toList();
    }

    // ------------------------------------------------------------------
    // Dashboard
    // ------------------------------------------------------------------

    public DashboardSummaryResponse dashboard() {
        List<OrderEntity> all = orderRepository.findAllByOrderByCreatedAtDesc();
        long pending = all.stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING
                        || o.getStatus() == OrderStatus.CONFIRMED
                        || o.getStatus() == OrderStatus.PREPARING
                        || o.getStatus() == OrderStatus.READY_FOR_PICKUP
                        || o.getStatus() == OrderStatus.ASSIGNED)
                .count();
        BigDecimal revenue = all.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .map(OrderEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long activeRiders = userRepository.findAllByRoleAndStatus(UserRole.RIDER, AccountStatus.ACTIVE).size();
        List<DashboardSummaryResponse.RecentOrder> recent = all.stream().limit(8)
                .map(o -> new DashboardSummaryResponse.RecentOrder(o.getPublicId(),
                        customerName(o), o.getStatus().name(), o.getTotalAmount(), o.getCreatedAt()))
                .toList();
        return new DashboardSummaryResponse(all.size(), pending, revenue, activeRiders,
                inventoryService.countLowStock(), recent);
    }

    // ------------------------------------------------------------------
    // Projection
    // ------------------------------------------------------------------

    public OrderResponse toResponse(OrderEntity order) {
        List<OrderItemResponse> items = orderItemRepository.findByOrderId(order.getId()).stream()
                .map(i -> new OrderItemResponse(i.getProductPublicId(), i.getProductName(),
                        i.getUnitPrice(), i.getQuantity(), i.getLineTotal()))
                .sorted(Comparator.comparing(OrderItemResponse::productName))
                .toList();
        List<OrderStatusEventResponse> events = eventRepository.findByOrderIdOrderByCreatedAtAsc(order.getId())
                .stream().map(e -> new OrderStatusEventResponse(e.getToStatus(), e.getCreatedAt(), e.getNote()))
                .toList();
        String riderPublicId = null;
        String riderName = null;
        if (order.getRiderId() != null) {
            UserEntity rider = userRepository.findById(order.getRiderId()).orElse(null);
            if (rider != null) {
                riderPublicId = rider.getPublicId();
                riderName = rider.getFullName();
            }
        }
        return new OrderResponse(order.getPublicId(), order.getStatus().name(),
                order.getSubtotalAmount(), order.getDiscountAmount(), order.getDeliveryFee(),
                order.getTotalAmount(), order.getPaymentMethod().name(), order.getDeliveryAddress(),
                order.getContactPhone(), order.getNotes(), customerPublicId(order),
                customerName(order), riderPublicId, riderName, items, events,
                order.getCreatedAt(), order.getUpdatedAt());
    }

    private String customerPublicId(OrderEntity order) {
        return userRepository.findById(order.getCustomerId())
                .map(UserEntity::getPublicId).orElse(null);
    }

    private String customerName(OrderEntity order) {
        return userRepository.findById(order.getCustomerId())
                .map(UserEntity::getFullName).orElse("Unknown customer");
    }
}
