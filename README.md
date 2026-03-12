# 🛍️ KUET Mini Market

A full-stack e-commerce web application built as a university lab project at **Khulna University of Engineering & Technology (KUET)**. The system supports three user roles — **Admin**, **Seller**, and **Buyer** — with role-based access control, JWT authentication, and a complete order management workflow.

---

## 🧱 Tech Stack

### Backend

| Technology      | Version | Purpose                        |
| --------------- | ------- | ------------------------------ |
| Java            | 17      | Language                       |
| Spring Boot     | 4.0.3   | Application framework          |
| Spring Security | 6       | Authentication & authorization |
| Spring Data JPA | —       | ORM / database access          |
| PostgreSQL      | 16      | Production database            |
| JWT (jjwt)      | 0.12.6  | Stateless auth tokens          |
| Lombok          | —       | Boilerplate reduction          |
| Maven           | 3.9.9   | Build tool                     |
| H2              | —       | In-memory DB for tests only    |
| JUnit 5         | —       | Unit & integration testing     |
| Mockito         | —       | Mocking in unit tests          |
| Docker          | —       | Containerization               |

### Frontend

| Technology   | Version | Purpose              |
| ------------ | ------- | -------------------- |
| React        | 18      | UI library           |
| Vite         | 5       | Dev server & bundler |
| React Router | v6      | Client-side routing  |
| Axios        | 1.6.7   | HTTP client          |
| CSS (custom) | —       | Styling              |

---

## 📐 Architecture

```
┌─────────────────────────────┐
│   mini-market-ui            │   React + Vite  (port 5174)
│   (Frontend)                │
└───────────┬─────────────────┘
            │  HTTP REST (JSON)
            │  Proxy: /api → localhost:8081
            ▼
┌─────────────────────────────┐
│   mini-market-app           │   Spring Boot  (port 8081)
│   (Backend)                 │   JWT · Spring Security
└───────────┬─────────────────┘
            │  JPA / JDBC
            ▼
┌─────────────────────────────┐
│   mini-market-db            │   PostgreSQL 16  (port 5432)
│   (Database)                │
└─────────────────────────────┘
```

The frontend and backend are **separate projects**. The backend is a pure REST API — no server-side rendering.

---

## 🗄️ Database Schema

```
users ──────────< user_roles >────────── roles
  │
  ├──────────────< products
  │
  └──────────────< orders ──────< order_items >──── products
```

| Table         | Key Columns                                                |
| ------------- | ---------------------------------------------------------- |
| `users`       | `id`, `full_name`, `email`, `password`, `enabled`          |
| `roles`       | `id`, `name` (`ADMIN` \| `SELLER` \| `BUYER`)              |
| `user_roles`  | `user_id`, `role_id` — Many-to-Many join table             |
| `products`    | `id`, `title`, `price`, `stock`, `status`, `seller_id`     |
| `orders`      | `id`, `buyer_id`, `total_amount`, `status`, `cancelled_by` |
| `order_items` | `id`, `order_id`, `product_id`, `quantity`, `unit_price`   |

> A user can hold **multiple roles simultaneously** (e.g., BUYER + SELLER at the same time).

---

## 🔐 Roles & Permissions

| Action                         | BUYER | SELLER | ADMIN |
| ------------------------------ | :---: | :----: | :---: |
| Browse / view products         |  ✅   |   ✅   |  ✅   |
| Place an order                 |  ✅   |   ❌   |  ❌   |
| View own orders                |  ✅   |   ❌   |  ❌   |
| Cancel own order               |  ✅   |   ❌   |  ❌   |
| Add a new product              |  ❌   |   ✅   |  ❌   |
| Edit / delete own product      |  ❌   |   ✅   |  ❌   |
| View own sales orders          |  ❌   |   ✅   |  ❌   |
| Complete / cancel a sale       |  ❌   |   ✅   |  ❌   |
| Edit or delete **any** product |  ❌   |   ❌   |  ✅   |
| View **all** orders            |  ❌   |   ❌   |  ✅   |
| Activate / deactivate users    |  ❌   |   ❌   |  ✅   |
| View all users                 |  ❌   |   ❌   |  ✅   |

> **Self-buy prevention:** A seller cannot purchase their own listed products.

---

## 🌐 REST API Endpoints

All protected endpoints require the header:

```
Authorization: Bearer <JWT_TOKEN>
```

### Auth — `/api/auth`

| Method | Endpoint             | Access | Description         |
| ------ | -------------------- | ------ | ------------------- |
| `POST` | `/api/auth/register` | Public | Register a new user |
| `POST` | `/api/auth/login`    | Public | Login, receive JWT  |
| `POST` | `/api/auth/logout`   | Any    | Client-side logout  |

### Products — `/api/products`

| Method   | Endpoint             | Access         | Description               |
| -------- | -------------------- | -------------- | ------------------------- |
| `GET`    | `/api/products`      | Public         | List all products         |
| `GET`    | `/api/products/{id}` | Public         | Get product by ID         |
| `GET`    | `/api/products/my`   | SELLER         | Get seller's own products |
| `POST`   | `/api/products`      | SELLER         | Create a new product      |
| `PUT`    | `/api/products/{id}` | SELLER / ADMIN | Update a product          |
| `DELETE` | `/api/products/{id}` | SELLER / ADMIN | Delete a product          |

### Orders — `/api/orders`

| Method  | Endpoint                    | Access         | Description                        |
| ------- | --------------------------- | -------------- | ---------------------------------- |
| `POST`  | `/api/orders`               | BUYER          | Place a new order                  |
| `GET`   | `/api/orders/my`            | BUYER          | View own purchase history          |
| `GET`   | `/api/orders/sales`         | SELLER         | View orders containing my products |
| `GET`   | `/api/orders`               | ADMIN          | View all orders                    |
| `PATCH` | `/api/orders/{id}/complete` | SELLER         | Mark order as completed            |
| `PATCH` | `/api/orders/{id}/cancel`   | BUYER / SELLER | Cancel an order                    |

### Admin — `/api/admin`

| Method  | Endpoint                           | Access | Description             |
| ------- | ---------------------------------- | ------ | ----------------------- |
| `GET`   | `/api/admin/users`                 | ADMIN  | List all users          |
| `PATCH` | `/api/admin/users/{id}/activate`   | ADMIN  | Activate a user account |
| `PATCH` | `/api/admin/users/{id}/deactivate` | ADMIN  | Disable a user account  |

### Health

| Method | Endpoint      | Access | Description  |
| ------ | ------------- | ------ | ------------ |
| `GET`  | `/api/health` | Public | Health check |

---

## 📦 Request & Response Examples

### Register

```http
POST /api/auth/register
Content-Type: application/json

{
  "fullName": "John Doe",
  "email": "john@example.com",
  "password": "secret123",
  "roles": ["BUYER"]
}
```

> `roles` is optional and defaults to `["BUYER"]` if omitted.  
> Supports multiple roles at once: `["BUYER", "SELLER"]`

### Login

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "secret123"
}
```

**Response:**

```json
{
  "id": 1,
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "john@example.com",
  "roles": ["BUYER"]
}
```

### Place an Order

```http
POST /api/orders
Authorization: Bearer <token>
Content-Type: application/json

{
  "items": [
    { "productId": 3, "quantity": 2 },
    { "productId": 7, "quantity": 1 }
  ]
}
```

---

## 🐳 Running with Docker

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running

### Start the application

```bash
# Clone the repository
git clone <repository-url>
cd mini-market

# Build image and start all services in background
docker compose up --build -d
```

Backend API is available at: **`http://localhost:8081/api`**

### Docker Services

| Service         | Container         | Port mapped |
| --------------- | ----------------- | ----------- |
| Spring Boot App | `mini-market-app` | `8081:8080` |
| PostgreSQL 16   | `mini-market-db`  | `5432:5432` |

The database uses a named volume (`postgres_data`) so data persists across container restarts.

### Stop / clean up

```bash
# Stop containers (keep database data)
docker compose down

# Stop containers AND remove the database volume
docker compose down -v
```

### Inspect the database (optional)

```bash
docker exec -it mini-market-db psql -U postgres -d mini-market
```

---

## 🖥️ Running the Frontend

The frontend is a **separate project** in the `mini-market-ui/` directory.

```bash
cd mini-market-ui
npm install
npm run dev
```

Dev server: **`http://localhost:5174`**

> Vite's dev proxy automatically forwards all `/api` requests to `http://localhost:8081`. Make sure the backend is running first.

---

## 🗺️ Frontend Routes

| Route                | Access         | Description                         |
| -------------------- | -------------- | ----------------------------------- |
| `/`                  | Public         | Redirects to `/products`            |
| `/login`             | Public         | Login page                          |
| `/register`          | Public         | Register (select one or more roles) |
| `/products`          | Public         | Browse all products                 |
| `/products/:id`      | Public         | Product detail page                 |
| `/products/new`      | SELLER         | Create a new product listing        |
| `/products/:id/edit` | SELLER / ADMIN | Edit an existing product            |
| `/my-products`       | SELLER         | View & manage own products          |
| `/cart`              | BUYER          | Shopping cart & checkout            |
| `/orders/my`         | BUYER          | View own purchase history           |
| `/orders/sales`      | SELLER         | View incoming sales                 |
| `/orders/all`        | ADMIN          | View all orders in the system       |
| `/admin/users`       | ADMIN          | Manage user accounts                |

---

## 🧪 Running Tests

Tests use an **H2 in-memory database** — no Docker or PostgreSQL required.

```bash
./mvnw test
```

### Test Coverage

| Test Class                         | Type        | What It Covers                                    |
| ---------------------------------- | ----------- | ------------------------------------------------- |
| `AuthServiceTest`                  | Unit        | Register, login, duplicate email, role mapping    |
| `ProductServiceTest`               | Unit        | CRUD, ownership enforcement, admin override       |
| `OrderServiceTest`                 | Unit        | Place order, self-buy block, complete, cancel     |
| `AdminServiceTest`                 | Unit        | Activate/deactivate, user listing                 |
| `CustomUserDetailsServiceTest`     | Unit        | User loading, disabled users, not-found exception |
| `AuthControllerIntegrationTest`    | Integration | Register, login, role-based route access          |
| `ProductControllerIntegrationTest` | Integration | Product CRUD, 401/403 enforcement                 |
| `OrderControllerIntegrationTest`   | Integration | Place order, view orders, role checks             |
| `AdminControllerIntegrationTest`   | Integration | Admin user management, non-admin rejection        |
| `MiniMarketApplicationTests`       | Smoke       | Spring context loads successfully                 |

---

## 📁 Project Structure

### Backend (`mini-market/`)

```
src/
├── main/
│   ├── java/com/kuet/minimarket/
│   │   ├── config/
│   │   │   └── SecurityConfig.java          # Spring Security filter chain
│   │   ├── controller/
│   │   │   ├── AuthController.java
│   │   │   ├── ProductController.java
│   │   │   ├── OrderController.java
│   │   │   ├── AdminController.java
│   │   │   └── HealthController.java
│   │   ├── dto/                             # Request & Response DTOs
│   │   ├── entity/                          # JPA entities
│   │   ├── exception/
│   │   │   └── GlobalExceptionHandler.java  # RFC 7807 ProblemDetail errors
│   │   ├── repository/                      # Spring Data JPA repositories
│   │   ├── security/
│   │   │   ├── JwtUtil.java
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   ├── JwtAuthEntryPoint.java
│   │   │   ├── CustomUserDetails.java
│   │   │   └── CustomUserDetailsService.java
│   │   └── service/                         # Business logic
│   └── resources/
│       ├── application.properties           # Local dev config
│       └── application-docker.properties    # Docker profile config
└── test/
    ├── java/                                # Unit & integration tests
    └── resources/application.properties    # H2 in-memory test config
```

### Frontend (`mini-market-ui/`)

```
src/
├── api/
│   └── axios.js              # Axios instance with JWT interceptor
├── components/
│   ├── Navbar.jsx             # Role-aware navigation bar
│   ├── ProductCard.jsx        # Product card (add-to-cart hidden for own products)
│   └── ProtectedRoute.jsx     # Route guard for role-based access
├── context/
│   ├── AuthContext.jsx        # JWT decode, login/logout state
│   └── CartContext.jsx        # Cart state management
└── pages/
    ├── LoginPage.jsx
    ├── RegisterPage.jsx        # Multi-role checkbox selection
    ├── ProductsPage.jsx
    ├── ProductDetailPage.jsx
    ├── ProductFormPage.jsx     # Create / edit product form
    ├── MyProductsPage.jsx
    ├── CartPage.jsx            # Cart + order placement
    ├── MyOrdersPage.jsx
    ├── SalesOrdersPage.jsx     # Seller: complete / cancel orders
    ├── AllOrdersPage.jsx       # Admin: all orders
    └── AdminUsersPage.jsx      # Admin: user management
```

---

## ⚙️ CI/CD

A **GitHub Actions** workflow runs on every push to `feature/**`, `develop`, and `main` branches, and on pull requests targeting `develop` and `main`.

**Pipeline steps:**

1. Checkout code
2. Set up JDK 17 (Eclipse Temurin)
3. Cache Maven packages (`~/.m2`)
4. Build and run all tests: `./mvnw clean test`

---

## 🔑 Authentication Flow

```
Register  →  POST /api/auth/register  →  returns { token, email, roles }
Login     →  POST /api/auth/login     →  returns { token, email, roles }

All protected requests  →  Authorization: Bearer <token>
```

- JWT expires in **24 hours**
- Token is **stateless** — the server stores no session
- On `401`, axios interceptor clears local storage and redirects to `/login`

---

## 🔄 Order Status Flow

```
                             PATCH /{id}/cancel
                           (BUYER / SELLER only)
              ┌─────────────────────────────────► CANCELLED
              │
   PLACED ────┤
              │
              └─────────────────────────────────► COMPLETED
                         PATCH /{id}/complete
                              (SELLER)
```

> **Stock deduction timing:** Stock is deducted **only when a seller marks an order COMPLETED**, not when the buyer places it. If an order is cancelled, no stock change occurs (since none was deducted at placement).

---

## 📌 Product Status Values

| Status     | When set                                           |
| ---------- | -------------------------------------------------- |
| `ACTIVE`   | Default — product is available for purchase        |
| `SOLD_OUT` | Automatically set when stock reaches 0 on COMPLETE |
| `REMOVED`  | Manually set by seller or admin to delist          |

---

## 🛡️ Security Notes

- Passwords hashed with **BCrypt**
- JWT signed with **HMAC-SHA256** using a Base64-encoded secret key
- Role checks enforced at the method level via `@PreAuthorize`
- All errors return **RFC 7807 `ProblemDetail`** format — no stack traces exposed
- `WWW-Authenticate` header removed in Vite proxy to prevent browser login popups

---

## 🚀 Local Development (Without Docker)

To run the backend locally without Docker, you need a local PostgreSQL instance.

1. Create the database:

   ```sql
   CREATE DATABASE "kuet-mini-market";
   ```

2. Update `src/main/resources/application.properties`:

   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/kuet-mini-market
   spring.datasource.username=postgres
   spring.datasource.password=your_password
   ```

3. Run the backend:
   ```bash
   ./mvnw spring-boot:run
   ```

---

## � Team Members

| Name                  | Roll No. |
| --------------------- | -------- |
| Akash Biswas          | 2107013  |
| Md. Ariful Alam Mahim | 2107023  |

**Department of Computer Science & Engineering**  
**Khulna University of Engineering & Technology (KUET)**
