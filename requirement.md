# KUET Mini Market — Security/Auth/CI Requirements

You must do **ONLY** these 3 things for the KUET Mini Market project.  
Do **not** implement anything outside these requirements.

## Tasks

1. Spring Security based Authentication and Authorization using **JWT token**
2. Unit tests and integration tests for Authentication/Authorization
3. CI workflow so tests run automatically on pull requests in `main` and `develop` branch, and on push to any `feature/*` branch

---

- First Configure PostgreSQL connection locally before coding
  Database name must be:

```
kuet-mini-market
```

---

## Important scope restriction

Do **only** the 3 tasks above.

Do **not** do these:

- do not implement products module
- do not implement orders module
- do not implement cart module
- do not implement Docker
- do not implement deployment
- do not implement unnecessary UI
- do not add unrelated features

If something is not necessary for authentication/authorization/tests/CI, do not implement it.

---

## Initial database scope

the `users` , 'roles' and 'user_roles' table/entity is guaranteed as the starting point.

You must build the authentication/authorization foundation in a **clean, extensible, scalable** way so that later we can connect:


- `products`
- `orders`
- 'order_items'

without major refactoring.

---

## Project structure

```text
mini-market/
┣ src/
┃ ┣ main/
┃ ┃ ┣ java/com/kuet/minimarket/
┃ ┃ ┃ ┣ config/
┃ ┃ ┃ ┣ controller/
┃ ┃ ┃ ┣ dto/
┃ ┃ ┃ ┣ entity/
┃ ┃ ┃ ┣ repository/
┃ ┃ ┃ ┣ service/
┃ ┃ ┃ ┗ exception/
┃ ┃ ┗ resources/
┃ ┃ ┣ templates/
┃ ┃ ┣ static/
┃ ┃ ┗ application.properties
┃ ┗ test/
┣ pom.xml
```

---

## Database requirement

Create and use PostgreSQL database:

```
kuet-mini-market
```

## Tables for user:

### 1. users

```
id (PK)
full_name
email (unique)
password
enabled
created_at
updated_at
```

### 2. roles

```
id (PK)
name  (ADMIN | SELLER | BUYER)
```

### 3. user_roles (join table)

```
user_id (FK → users.id)
role_id (FK → roles.id)

PK(user_id, role_id)
```

---

## Current marketplace roles

- ADMIN
- SELLER
- BUYER

---

## Authentication/Authorization requirements

Implement these now:

1. Register
2. Login
3. Logout
4. Role-based authorization using JWT token
5. Disabled users cannot log in
6. URL access control:
   - `/admin/**` -> ADMIN
   - `/seller/**` -> SELLER
   - `/buyer/**`, `/orders/**`, `/cart/**`, `/checkout/**` -> BUYER
   - public routes like `/login`, `/register`, `/products`, and static assets -> permit all

7. Use custom `UserDetailsService`
8. Use `BCryptPasswordEncoder`
9. Proper validation and error handling
10. Keep implementation ready for many-to-many user-role support, even if only `users` table is initially available
11. On successful login, the server must return a JWT token
12. JWT must support role-based authorization cleanly

---

## Important design instruction

Even if only the `users` table is initially available, design the security layer in a scalable way.

If needed, create role-related skeleton classes/entities/repositories/services so future `roles` and `user_roles` integration is smooth.

Do **not** hardcode unscalable logic.

---

## Implementation expectations

Implement only the minimum necessary code to support auth/security properly:

- security config
- JWT utility/service
- JWT authentication filter
- authentication entry point / unauthorized handler if needed
- custom `UserDetails` and `UserDetailsService`
- auth controller for register/login/logout
- DTOs for auth requests/responses
- user entity/repository/service logic related to auth
- role-related skeleton if needed for future extensibility
- validation and exception handling
- minimal protected test endpoints if needed to verify role-based access in tests

---

## Testing requirements

Create strong tests for Authentication/Authorization only.

### Unit tests should cover

- password encoding works
- register user success
- duplicate email rejected
- disabled user cannot authenticate
- role mapping works
- user loading works
- login-related service behavior where applicable
- validation logic where applicable

### Integration tests using MockMvc should cover

- register endpoint/page works
- login works with valid credentials
- login fails with invalid credentials
- protected route blocks unauthenticated user
- buyer cannot access admin route
- seller cannot access admin route
- admin can access admin route
- disabled user blocked from login if implemented in integration path

---

## CI requirements

Create this file:

```
.github/workflows/ci.yml
```

The workflow must:

- run on push to any `feature/*` branch
- not run on direct push to `main` or `develop`
- run on pull requests targeting `develop` and `main`
- build the project
- run tests
- stay simple and reliable

### Exact CI trigger behavior

```yaml
on:
  push:
    branches:
      - "feature/**"
  pull_request:
    branches:
      - develop
      - main
```

---

## Final instruction

Read this file carefully and implement exactly what is written here.
Do not go beyond this scope.

```

```
