# BD QuickMart — Project Features & Team Responsibility Matrix

> **Project purpose:** Build an AI-powered hyperlocal quick-commerce platform for Bangladesh, connecting customers with nearby Dark Stores and enabling fast delivery of groceries and daily essentials.

## 1. Project Overview

### Technology Stack

| Component | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.5.6 |
| Database | MySQL |
| Customer App | Flutter |
| Rider App | Flutter |
| Admin Web | React, TypeScript, Vite |
| Authentication | JWT, BCrypt, Refresh Token Rotation |
| AI/ML | Python-based ML pipeline |
| Real-time communication | WebSocket |
| API | REST |

## 2. Team Structure & Ownership

| Member | Primary ownership |
|---|---|
| **Shriful** | Existing implementation, architecture, authentication, AI/ML and model training, integration and technical leadership |
| **Arnab** | Customer application and shopping/order experience |
| **Sabbir** | Rider application and delivery operations |
| **Munjil** | Admin web, Dark Store and inventory management |

**Allocation rules**
- All work already implemented up to the current project state remains under **Shriful**.
- All AI/ML, data pipeline, model training, evaluation and inference work remains under **Shriful**.
- Arnab, Sabbir and Munjil own the remaining product modules described below.
- Each module owner is responsible for its frontend, backend APIs, integration and tests unless a task is explicitly marked shared.
- Shared infrastructure must be coordinated; it is not a reason to duplicate implementation.
- This is a proposed responsibility allocation. It does not claim that unimplemented features are already complete.

## 3. Shriful — Existing Work and Technical Ownership

### 3.1 Work implemented so far

| Feature / deliverable | Current status |
|---|---|
| Monorepo and project foundation | Done |
| Spring Boot backend foundation | Done |
| Customer Flutter app foundation | Done |
| Rider Flutter app foundation | Done |
| Admin React web foundation | Done |
| MySQL integration foundation | Done |
| Initial project/API documentation | Done |
| JWT authentication implementation | Implemented |
| BCrypt password hashing | Implemented |
| Access and refresh token flow | Implemented |
| Refresh token rotation and replay detection | Implemented |
| Six-role RBAC foundation | Implemented |
| Customer registration and login | Implemented |
| Rider registration and login | Implemented |
| Rider `PENDING` approval workflow | Implemented |
| Admin rider approval APIs | Implemented |
| Customer session restoration and logout | Implemented |
| Rider session restoration and logout | Implemented |
| Admin login and protected routes | Implemented |
| Authentication database migrations | Implemented |
| Authentication/authorization tests | Implemented |
| Customer and Rider debug APK builds | Built |

**Verification status:** Goal 01 is accepted. Goal 02 implementation has been pushed to GitHub and is marked `READY_FOR_REVIEW`; physical Android-device walkthrough and Admin browser E2E verification remain outstanding. Do not describe Goal 02 as fully accepted until those checks pass.

### 3.2 Remaining Shriful responsibilities — AI/ML and model training

| AI/ML workstream | Scope |
|---|---|
| Data collection | Define data sources, schemas and collection pipelines |
| Data preprocessing | Cleaning, validation, missing-value handling and feature preparation |
| Exploratory Data Analysis | Data quality analysis, distributions, trends and leakage checks |
| Demand forecasting | Forecast demand by product, store and time window |
| Product demand prediction | Predict item-level demand and replenishment needs |
| Inventory optimization | Estimate reorder quantities and stock-risk signals |
| Product recommendation | Build recommendation/ranking models |
| Rider assignment intelligence | Develop an optimization or prediction component to support rider assignment |
| Delivery-time prediction | Estimate delivery ETA from available operational data |
| Model training | Training pipelines, reproducibility and experiment tracking |
| Model evaluation | Appropriate baselines, metrics, validation and error analysis |
| Hyperparameter tuning | Controlled tuning and model selection |
| Model versioning | Track datasets, features, parameters and model artifacts |
| Model inference API | Expose model predictions through a documented service/API |
| Deployment and monitoring | Deploy, monitor quality/latency and detect model/data drift |
| Spring Boot integration | Integrate inference outputs into application workflows |

**Important:** These are assigned responsibilities and planned workstreams, not claims that the models have already been trained or deployed.

### 3.3 Shriful — cross-cutting technical responsibilities

- Own overall system architecture and technical decisions.
- Review authentication, authorization and security-sensitive changes.
- Coordinate API contracts and integration between modules.
- Review database design and migration safety.
- Coordinate deployment and release readiness.
- Lead final system integration and technical QA.

## 4. Arnab — Customer Application

**Primary ownership:** Customer Flutter app and customer-facing shopping/order workflows.

### 4.1 Customer experience

1. Customer profile management
2. Address management and saved locations
3. Home page and product discovery
4. Product categories and subcategories
5. Product listing and product details
6. Product search
7. Filters and sorting
8. Shopping cart
9. Cart quantity management
10. Wishlist and favorites
11. Coupon and discount application
12. Checkout flow
13. Delivery address selection
14. Order placement
15. Order history
16. Order cancellation (subject to order state and policy)
17. Order details and status history
18. Customer reviews and ratings
19. Customer notification UI and integration
20. Customer support interface

### 4.2 Backend ownership

- Customer profile and address APIs
- Product browsing/search APIs in coordination with Munjil
- Cart and wishlist APIs
- Coupon validation integration in coordination with Munjil
- Checkout and customer order-placement APIs
- Customer order history/details/cancellation APIs
- Review and rating APIs

### 4.3 Testing and delivery

- Customer app UI and integration tests
- API validation and error-state handling
- Checkout and order-placement test scenarios
- Session-expiry and authorization tests
- Accessibility and responsive-layout checks where applicable

## 5. Sabbir — Rider Application & Delivery Operations

**Primary ownership:** Rider Flutter app and delivery lifecycle.

### 5.1 Rider experience

1. Rider dashboard
2. Rider profile management
3. Rider document submission and verification interface
4. Rider approval-status UI
5. Online/offline availability
6. Delivery task assignment interface
7. Accept/reject delivery requests
8. Pickup location details
9. Customer delivery address
10. Order pickup confirmation
11. Delivery status updates
12. Live GPS location sharing
13. Map and navigation integration
14. Delivery completion confirmation
15. Delivery proof (photo and/or OTP, subject to product decision)
16. Delivery history
17. Rider earnings dashboard
18. Incentive and bonus history
19. Rider notifications
20. Rider support interface

### 5.2 Backend ownership

- Rider profile and availability APIs
- Rider document/verification workflow APIs, coordinated with existing approval APIs
- Delivery task and assignment workflow APIs
- Accept/reject and pickup/delivery status APIs
- Rider location update APIs
- Delivery proof APIs
- Rider delivery history and earnings APIs
- Rider notification integration

### 5.3 Testing and delivery

- Rider app UI and integration tests
- Delivery state-transition tests
- GPS/location permission and network-failure tests
- Assignment acceptance/rejection tests
- Delivery completion and proof tests
- Rider authorization and approval-state tests

## 6. Munjil — Admin Web, Dark Store & Inventory

**Primary ownership:** Admin React web application and store/inventory operations.

### 6.1 Admin and operations features

1. Admin dashboard
2. User management
3. Role and permission management UI
4. Rider approval management UI
5. Dark Store management
6. Store location management
7. Product management
8. Product category management
9. Product variants and SKU management
10. Inventory management
11. Stock-in and stock-out
12. Low-stock alerts
13. Stock adjustment
14. Supplier management
15. Purchase management
16. Order management dashboard
17. Order status monitoring
18. Refund and cancellation management
19. Coupon and promotion management
20. Payment transaction monitoring
21. Sales reports
22. Inventory reports
23. Rider performance reports
24. Store performance analytics
25. Admin audit logs

### 6.2 Backend ownership

- Admin operations APIs
- Dark Store and store-location APIs
- Product/category/SKU administration APIs
- Inventory and stock movement APIs
- Supplier and purchase APIs
- Admin order-management APIs
- Coupon and promotion management APIs
- Refund/cancellation management APIs
- Reporting and audit-log APIs

### 6.3 Testing and delivery

- Admin web component and integration tests
- Role/permission access tests
- Inventory consistency and stock-adjustment tests
- Product and store CRUD validation
- Order-management workflow tests
- Report correctness and audit-log tests

## 7. Shared Features and Integration Ownership

| Shared feature | Primary contributors | Coordination |
|---|---|---|
| Payment gateway integration (bKash, Nagad, cards, COD) | Arnab + Munjil | Shriful reviews architecture/security |
| WebSocket real-time order updates | Sabbir + Arnab | Shriful coordinates event/API contract |
| Push notification infrastructure | Sabbir + Arnab | Shared event contract |
| Order lifecycle integration | Arnab + Sabbir | Munjil provides store-side workflow |
| Inventory-to-order synchronization | Munjil + Arnab | Define stock reservation and rollback rules |
| Dark Store picking/packing workflow | Munjil | Integrate with order lifecycle |
| Rider assignment integration | Sabbir + Shriful | AI assignment is owned by Shriful |
| Security and RBAC review | Shriful + all module owners | Module owners enforce authorization |
| API integration and architecture | Shriful + all module owners | OpenAPI/API contract review |
| Deployment and production release | Shriful + all module owners | Each owner supplies deployable, tested module |

## 8. Canonical User Roles

The project uses these six canonical roles:

| Role | Responsibility |
|---|---|
| `CUSTOMER` | Browse products, shop and manage orders |
| `RIDER` | Accept and complete delivery tasks |
| `ADMIN` | Administrative operations |
| `SUPER_ADMIN` | Full system administration |
| `STORE_MANAGER` | Dark Store operations |
| `INVENTORY_OPERATOR` | Inventory and stock operations |

New rider registrations must remain in `PENDING` status until an authorized administrator approves them. Protected rider operations must enforce the approval state on the backend, not only in the UI.

## 9. Suggested Delivery Sequence

This is a suggested sequence, not a claim that the work is already scheduled or complete.

1. **Stabilize foundation:** Finish Goal 02 manual device and browser verification; resolve local backend startup and phone-to-PC LAN connectivity.
2. **Core catalog and store setup:** Implement Dark Store, product, category, SKU and inventory foundations.
3. **Customer shopping:** Build catalog browsing, search, cart, checkout and order placement.
4. **Store order operations:** Implement order acceptance, picking/packing and stock synchronization.
5. **Rider delivery:** Implement assignment, acceptance, pickup, GPS updates and delivery completion.
6. **Payments and notifications:** Integrate payment methods, transaction status and push notifications.
7. **AI/ML integration:** Train and evaluate models using valid operational data; integrate inference into relevant workflows.
8. **Admin reporting:** Complete operational dashboards, reports and audit logs.
9. **End-to-end QA:** Test customer-to-store-to-rider workflows, security, failure recovery and production readiness.

## 10. Definition of Done

A feature is not complete merely because its UI exists. Each feature should meet the following criteria:

- Frontend flow is implemented and handles loading, empty, success and error states.
- Backend API is implemented with validation and authorization.
- Database changes use safe, versioned migrations.
- API contract and relevant documentation are updated.
- Automated tests cover important success and failure cases.
- Integration with dependent modules is verified.
- No secrets or credentials are committed to Git.
- Feature is manually verified in the target environment where applicable.
- Owner records implementation status and evidence.

## 11. Workload Allocation Summary

| Member | Main workload |
|---|---|
| **Shriful** | Existing foundation, authentication, AI/ML, model training, architecture, security review and integration |
| **Arnab** | Customer app, shopping, cart, checkout, customer orders and reviews |
| **Sabbir** | Rider app, delivery operations, GPS tracking, delivery proof and earnings |
| **Munjil** | Admin web, Dark Store, catalog administration, inventory, suppliers and reports |

The allocation is based on functional ownership, not a measured estimate of equal person-hours. Review scope and workload after breaking these features into sprint-sized tasks.

---

**Project:** BD QuickMart  
**Document:** Full Feature List & Team Responsibility Matrix  
**Team members:** Shriful, Arnab, Sabbir, Munjil  
**Status:** Proposed team allocation; implementation statuses are stated individually above.
