---
# Final KUET Mini Market Database Schema

## Tables (6)
---

# 1️⃣ users

Stores all system users (admin, seller, buyer).

| Field      | Type                | Meaning                       |
| ---------- | ------------------- | ----------------------------- |
| id         | BIGSERIAL (PK)      | Unique user ID                |
| full_name  | VARCHAR(150)        | User’s full name              |
| email      | VARCHAR(150) UNIQUE | Login email                   |
| password   | VARCHAR(255)        | BCrypt encrypted password     |
| enabled    | BOOLEAN             | Whether the account is active |
| created_at | TIMESTAMP           | Account creation time         |
| updated_at | TIMESTAMP           | Last update time              |

---

# 2️⃣ roles

Stores role types.

| Field | Type               | Meaning                                |
| ----- | ------------------ | -------------------------------------- |
| id    | BIGSERIAL (PK)     | Role ID                                |
| name  | VARCHAR(20) UNIQUE | Role name (`ADMIN`, `SELLER`, `BUYER`) |

---

# 3️⃣ user_roles

Join table for **Many-to-Many relationship** between users and roles.

| Field   | Type                   | Meaning                |
| ------- | ---------------------- | ---------------------- |
| user_id | BIGINT (FK → users.id) | User who owns the role |
| role_id | BIGINT (FK → roles.id) | Assigned role          |

Primary Key:

```
PK(user_id, role_id)
```

---

# 4️⃣ products

Products listed by sellers.

| Field       | Type                   | Meaning                                |
| ----------- | ---------------------- | -------------------------------------- |
| id          | BIGSERIAL (PK)         | Product ID                             |
| title       | VARCHAR(200)           | Product name                           |
| description | TEXT                   | Product description                    |
| price       | DECIMAL(10,2)          | Product price                          |
| stock       | INT                    | Available quantity                     |
| image_url   | TEXT                   | Product image URL (Cloudinary/S3 link) |
| status      | VARCHAR(20)            | `ACTIVE`, `SOLD_OUT`, `REMOVED`        |
| seller_id   | BIGINT (FK → users.id) | Seller who posted product              |
| created_at  | TIMESTAMP              | Product creation time                  |
| updated_at  | TIMESTAMP              | Last update time                       |

---

# 5️⃣ orders

Represents a purchase made by a buyer.

| Field        | Type                   | Meaning                            |
| ------------ | ---------------------- | ---------------------------------- |
| id           | BIGSERIAL (PK)         | Order ID                           |
| buyer_id     | BIGINT (FK → users.id) | Buyer                              |
| total_amount | DECIMAL(10,2)          | Total price                        |
| status       | VARCHAR(20)            | `PLACED`, `COMPLETED`, `CANCELLED` |
| created_at   | TIMESTAMP              | Order time                         |

---

# 6️⃣ order_items

Products included in an order.

| Field      | Type                      | Meaning                |
| ---------- | ------------------------- | ---------------------- |
| id         | BIGSERIAL (PK)            | Order item ID          |
| order_id   | BIGINT (FK → orders.id)   | Parent order           |
| product_id | BIGINT (FK → products.id) | Purchased product      |
| quantity   | INT                       | Units purchased        |
| unit_price | DECIMAL(10,2)             | Price at purchase time |

---

# Relationships

```
User 1 ───── M Product
User 1 ───── M Order

User M ───── M Role
      (via user_roles)

Order 1 ───── M OrderItem
Product 1 ─── M OrderItem
```

---

# ER Structure (clean)

```
User >────< Role

User ────< Product
User ────< Order

Order ────< OrderItem >──── Product
```

---
