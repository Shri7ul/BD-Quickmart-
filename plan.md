# 1. Final product scope

# BD QuickMart

Three client applications, one centralized platform.

Customer App

Shopping, AI recommendations, chatbot, checkout, tracking



Rider App

Assignments, navigation, GPS, delivery verification

Inventory & Admin Web

Store operations, inventory, orders, dispatch, analytics, AI insights

Java Spring Boot Backend

MySQL • Redis • WebSocket • Gemini API • Groq API

# 2. দুইটি feature list-এর পূর্ণ coverage


| Feature                     | কোথায় implement হবে        |
| --------------------------- | -------------------------- |
| Secure Login                | All three applications     |
| Smart Product Search        | Customer                   |
| AI Product Recommendation   | Customer + AI Backend      |
| Shopping Cart               | Customer                   |
| Secure Checkout             | Customer + Backend         |
| Live Order Tracking         | Customer + Rider           |
| AI Inventory Management     | Inventory Web + AI Backend |
| Smart Rider Assignment      | Backend + Rider + Admin    |
| Route Optimization          | Backend + Rider            |
| Bangla & English            | All three applications     |
| Ratings & Reviews           | Customer + Admin           |
| Admin Dashboard             | Inventory Web              |
| AI Chatbot                  | Customer + AI Backend      |
| Push Notifications          | Customer + Rider + Admin   |
| AI Demand Forecasting       | Inventory Web + AI Backend |
| Dynamic Rider Routing       | Backend + Rider            |
| Hyperlocal Dark Store       | Backend + Inventory Web    |
| Smart Delivery Optimization | Backend                    |
| Future Super App            | Future architecture        |

বিশেষ করে চারটি বিষয় আলাদা করে বুঝতে হবে:

- Demand Forecasting: ভবিষ্যতে কোন store-এ কোন product কত বিক্রি হবে।
- Inventory Management: কখন stock replenish করতে হবে, expiry কীভাবে manage করতে হবে।
- Rider Assignment: কোন rider কোন order পাবে।
- Dynamic Routing: assigned rider কীভাবে দ্রুত ও কার্যকর route দিয়ে destination-এ পৌঁছাবে।

এগুলো চারটি আলাদা problem। একটিমাত্র Gemini prompt দিয়ে এগুলো solve করা হবে না।

# 3. Sequential Implementation Goals

এটাই হবে আমাদের master implementation roadmap।

## Phase 1 — Foundation

Must complete first

### Goal 01 — System Architecture & Project Foundation

Objective: পুরো platform-এর technical foundation তৈরি করা।

Tasks:

- Monorepo GitHub structure তৈরি।
- Spring Boot backend initialize।
- Flutter customer app initialize।
- Flutter rider app initialize।
- React admin web initialize।
- MySQL database connection।
- Environment configuration।
- API versioning (`/api/v1`)।
- Swagger/OpenAPI configuration।
- Centralized error handling।
- Logging configuration।

Acceptance criteria:

- তিনটি application independently run করবে।
- Spring Boot server successfully start হবে।
- MySQL connection কাজ করবে।
- Swagger থেকে test API call করা যাবে।
- GitHub repository-তে clean project structure থাকবে।

### Goal 02 — Authentication & Role Management

Objective: Secure multi-role authentication system।

Roles:

- CUSTOMER
- RIDER
- SUPER_ADMIN
- STORE_MANAGER
- INVENTORY_OPERATOR

Tasks:

- Registration and login।
- JWT access/refresh token।
- OTP verification architecture।
- Password hashing।
- Role-based authorization।
- Protected routes।
- Rider and admin approval workflow।

Acceptance criteria:

- Customer শুধু customer functionality access করবে।
- Rider শুধু rider functionality access করবে।
- Store manager নিজের store-এর data access করবে।
- Admin সব store manage করতে পারবে।
- Unauthorized API requests reject হবে।

## Phase 2 — Core Commerce

### Goal 03 — Dark Store & Product Management

Objective: Hyperlocal grocery catalog তৈরি করা।

Tasks:

- Multiple dark store support।
- Store location and service radius।
- Product categories।
- Product CRUD।
- Product images।
- SKU management।
- Product pricing।
- Product availability।
- Store-specific product mapping।

Acceptance criteria:

- একাধিক store তৈরি করা যাবে।
- একই product বিভিন্ন store-এ থাকতে পারবে।
- প্রতিটি store-এর price ও availability আলাদা হতে পারবে।
- Customer-এর location অনুযায়ী eligible store খুঁজে পাওয়া যাবে।

### Goal 04 — Smart Product Search & Discovery

Objective: Customer যেন দ্রুত প্রয়োজনীয় product খুঁজে পায়।

Tasks:

- Product search।
- Category filtering।
- Price filtering।
- Typo-tolerant search।
- Search suggestions।
- Availability-aware results।
- Bangla and English product search।

Acceptance criteria:

- Product name দিয়ে search করা যাবে।
- Category ও price filter কাজ করবে।
- Unavailable products সঠিকভাবে identify হবে।
- বাংলা ও ইংরেজি search support থাকবে।

### Goal 05 — Shopping Cart & Secure Checkout

Objective: Complete purchasing workflow তৈরি করা।

Tasks:

- Add to cart।
- Remove from cart।
- Quantity update।
- Cart persistence।
- Inventory validation।
- Stock reservation।
- Delivery address।
- Delivery charge calculation।
- COD payment।
- Online payment integration architecture।

Acceptance criteria:

- Customer cart তৈরি করতে পারবে।
- Checkout-এর সময় stock revalidate হবে।
- Overselling prevent হবে।
- Successful checkout-এ order তৈরি হবে।
- Failed checkout-এ stock reservation rollback হবে।

## Phase 3 — Order & Delivery Operations

### Goal 06 — Order Management System

Objective: Complete order lifecycle তৈরি করা।

Order status:

```
PENDING
   ↓
CONFIRMED
   ↓
PREPARING
   ↓
READY_FOR_PICKUP
   ↓
ASSIGNED
   ↓
PICKED_UP
   ↓
OUT_FOR_DELIVERY
   ↓
DELIVERED
```

Additional states:

- CANCELLED
- REJECTED
- FAILED_DELIVERY

Tasks:

- Order creation।
- Order status management।
- Order history।
- Store order dashboard।
- Order cancellation rules।
- Order status timeline।
- Inventory deduction।
- Order notifications।

Acceptance criteria:

- Customer order করতে পারবে।
- Store manager order process করতে পারবে।
- Order status transition validate হবে।
- Customer updated status দেখতে পাবে।
- Order এবং inventory consistent থাকবে।

### Goal 07 — Smart Rider Assignment

Objective: Order-এর জন্য সবচেয়ে উপযুক্ত available rider নির্বাচন করা।

Assignment factors:

1. Rider distance from store।
2. Rider availability।
3. Current delivery workload।
4. Delivery capacity।
5. Estimated pickup time।

Tasks:

- Rider online/offline।
- Rider availability management।
- Rider GPS location।
- Eligible rider filtering।
- Assignment scoring algorithm।
- Assignment acceptance/rejection।
- Assignment timeout and reassignment।

Acceptance criteria:

- Available rider-দের মধ্যে eligible rider identify হবে।
- Nearest rider-কে শুধু distance নয়, workload বিবেচনা করেও select করা হবে।
- Rider assignment গ্রহণ করতে পারবে।
- Rejection বা timeout হলে reassignment হবে।

### Goal 08 — Live Tracking & Dynamic Rider Routing

Objective: Real-time delivery tracking এবং route optimization।

Tasks:

- Rider background GPS।
- WebSocket location updates।
- Customer live map।
- Google Maps integration।
- Distance calculation।
- Traffic-aware route estimation।
- ETA calculation।
- Route recalculation।
- Pickup and drop-off navigation।

Acceptance criteria:

- Customer rider-এর live location দেখতে পারবে।
- Rider location update server-এ পৌঁছাবে।
- ETA update হবে।
- Route পরিবর্তন হলে নতুন route calculate হবে।
- GPS disconnect হলে system stale location দেখাবে, fake live location নয়।

Important: Smart Rider Assignment এবং Dynamic Routing একই feature নয়। Goal 07 rider নির্বাচন করবে, Goal 08 delivery route পরিচালনা করবে।

## Phase 4 — Advanced Inventory & AI

Project differentiation

### Goal 09 — Advanced Inventory Management

Objective: Store-wise intelligent stock management।

Tasks:

- Stock in/out।
- Stock adjustment।
- Inventory batches।
- Expiry date tracking।
- FEFO (First Expired, First Out)।
- Low-stock alerts।
- Reorder thresholds।
- Stock movement history।
- Wastage management।
- Automated replenishment suggestions।

Acceptance criteria:

- প্রতিটি store-এর stock আলাদাভাবে track হবে।
- Batch-wise expiry information থাকবে।
- Expiring products identify হবে।
- Low stock হলে notification তৈরি হবে।
- Replenishment suggestion generate হবে।

### Goal 10 — AI Demand Forecasting

Objective: কোন store-এ কোন product-এর কত demand হবে তা predict করা।

প্রথমে প্রয়োজনীয় data collection:

- Historical sales।
- Product-wise demand।
- Store-wise demand।
- Day of week।
- Seasonality।
- Promotions।
- Stock availability।

Implementation stages:

| Stage        | Implementation                |
| ------------ | ----------------------------- |
| Initial      | Historical moving averages    |
| Intermediate | Gemini/Groq-assisted analysis |
| Advanced     | XGBoost / LightGBM            |
| Future       | Store-SKU forecasting model   |

Acceptance criteria:

- Store এবং SKU অনুযায়ী forecast তৈরি হবে।
- Daily demand prediction থাকবে।
- Actual sales-এর সঙ্গে forecast comparison হবে।
- Forecast database-এ save হবে।
- Forecast accuracy measure করা হবে।

একটি গুরুত্বপূর্ণ বিষয়: Gemini/Groq দিয়ে data explain এবং insights generate করা যাবে। কিন্তু quantitative forecasting-এর মূল engine হিসেবে statistical/ML model ব্যবহার করা বেশি উপযুক্ত।

### Goal 11 — AI Inventory Optimization

Objective: Demand forecast ব্যবহার করে intelligent replenishment।

Tasks:

- Forecast থেকে expected demand গ্রহণ।
- Current stock analysis।
- Expiry-aware stock rotation।
- Safety stock calculation।
- Reorder point calculation।
- Suggested order quantity।
- Overstock detection।
- Stockout risk detection।
- Store manager recommendations।

Acceptance criteria:

- System automatically low-stock এবং overstock risk identify করবে।
- Suggested replenishment quantity দেখাবে।
- Expiring inventory highlight করবে।
- Store manager suggestion approve/reject করতে পারবে।
- Approved replenishment stock records update করবে।

### Goal 12 — AI Product Recommendation

Objective: Customer-এর জন্য personalized product recommendations।

Tasks:

- Popular products।
- Frequently bought together।
- Customer purchase history।
- Category preferences।
- Local store trends।
- Similar product recommendations।
- Context-aware recommendations।

Acceptance criteria:

- Customer-specific recommendation দেখাবে।
- Store availability যাচাই করবে।
- Unavailable product recommend করবে না।
- Recommendation click এবং purchase tracking করবে।

### Goal 13 — AI Chatbot

Objective: Bangla এবং English conversational shopping assistant।

Tasks:

- Gemini/Groq integration।
- Bangla-English language detection।
- Product discovery।
- Product recommendation।
- Order status query।
- Delivery information।
- Customer FAQ।
- Context-aware conversation।
- Tool/function calling।

উদাহরণ:

Customer: "আমার ৫০০ টাকার মধ্যে ভালো কিছু snacks দেখাও।"

Chatbot:

- Available snacks search করবে।
- Price filter apply করবে।
- Relevant products দেখাবে।
- Add-to-cart action initiate করতে পারবে।

Acceptance criteria:

- Bangla ও English support।
- Real product catalog থেকে তথ্য।
- Real order status retrieve।
- Hallucinated price বা stock information নয়।
- Customer-এর অনুমতি ছাড়া order place নয়।

# Phase 5 — Customer Experience & Management

### Goal 14 — Ratings, Reviews & Feedback

Tasks:

- Product ratings।
- Delivery ratings।
- Written reviews।
- Verified purchase validation।
- Review moderation।
- Feedback analytics।

Acceptance criteria: Delivered order-এর customer review দিতে পারবে এবং admin review moderation করতে পারবে।

### Goal 15 — Push Notifications

Tasks:

- Firebase Cloud Messaging।
- Order status notifications।
- Rider assignment notifications।
- Rider arrival alerts।
- Low-stock alerts।
- Replenishment alerts।
- Promotional notifications।

Acceptance criteria: Relevant user-এর device-এ relevant notification যাবে এবং notification history থাকবে।

### Goal 16 — Admin Dashboard & Analytics

Tasks:

- Total sales।
- Revenue।
- Total orders।
- Active riders।
- Store performance।
- Inventory health।
- Low-stock products।
- Expiry reports।
- Delivery performance।
- Demand forecast visualization।
- AI-generated operational insights।

Acceptance criteria: Admin dashboard-এ real database data থেকে analytics generate হবে। Mock numbers দিয়ে final dashboard তৈরি করা যাবে না।

### Goal 17 — Bangla & English Localization

Tasks:

- Language switching।
- Product localization।
- Interface translation।
- Order status translation।
- Notifications translation।
- AI chatbot localization।

Acceptance criteria: তিনটি application-এ Bangla ও English support থাকবে এবং language switch করলে interface-এর text সঠিকভাবে পরিবর্তিত হবে।

# Phase 6 — Production Readiness

### Goal 18 — Testing, Security & Deployment

Tasks:

- Unit testing।
- Integration testing।
- API testing।
- End-to-end testing।
- Payment security।
- Authorization testing।
- Concurrent order testing।
- Load testing।
- Database backup।
- Deployment।
- Monitoring and logging।

Acceptance criteria:

- Complete order-to-delivery workflow কাজ করবে।
- Concurrent stock updates safe হবে।
- Unauthorized access prevent হবে।
- Critical APIs tested থাকবে।
- Production deployment documentation থাকবে।

# 4. Feature-to-Goal Traceability


| Original feature            | Goal         |
| --------------------------- | ------------ |
| Secure Login                | 02           |
| Smart Product Search        | 04           |
| AI Product Recommendation   | 12           |
| Shopping Cart               | 05           |
| Secure Checkout             | 05           |
| Live Order Tracking         | 08           |
| AI Inventory Management     | 09, 11       |
| Smart Rider Assignment      | 07           |
| Route Optimization          | 08           |
| Bangla & English Support    | 17           |
| Ratings & Reviews           | 14           |
| Admin Dashboard             | 16           |
| AI Chatbot                  | 13           |
| Push Notifications          | 15           |
| AI Demand Forecasting       | 10           |
| Dynamic Rider Routing       | 08           |
| Hyperlocal Dark Store Model | 03, 07       |
| Smart Delivery Optimization | 07, 08       |
| Future Super App Vision     | Future scope |

# 5. Goal execution rules

প্রতিটি Goal-এর workflow

1. Requirements — Goal-এর scope এবং constraints নির্ধারণ।
2. Design — Database, API, architecture ও UI design।
3. Implementation — Backend ও সংশ্লিষ্ট frontend development।
4. Testing — Unit, integration এবং manual testing।
5. Acceptance — Acceptance criteria verify করা।
6. Documentation — Implementation summary, API এবং known limitations update।
7. Next Goal — Acceptance complete হলেই পরবর্তী Goal শুরু।

প্রতিটি Goal-এর status হবে:

- `NOT_STARTED`
- `IN_PROGRESS`
- `BLOCKED`
- `READY_FOR_REVIEW`
- `ACCEPTED`

একটি Goal-এর কাজ শেষ হলেই সেটি ACCEPTED হবে না। Test evidence এবং acceptance criteria verify করতে হবে।
