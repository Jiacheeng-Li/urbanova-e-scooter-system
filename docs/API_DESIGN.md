# Urbanova API Design (Implemented v1)

Last updated: 2026-03-14

## 1. Purpose and Scope

This document describes the **currently implemented** backend APIs in this repository.

Implemented backlog/new scope:
- ID 1: User registration & login
- ID 4: View hire options and cost
- ID 5: Book an e-scooter
- ID 12: Cancel booking
- New requirement: booking query and booking modify APIs
- New requirement: scooter ID query by status

Not yet implemented from the previous full design draft (examples): refresh/logout/password reset, payments, confirmations, staff/manager/admin APIs, analytics.

## 2. Global API Conventions

### 2.1 Base URL and Version

- Base URL: `/api/v1`

### 2.2 Content Type

- Request: `Content-Type: application/json`
- Response: `Content-Type: application/json`

### 2.3 Authentication

- Protected endpoints require: `Authorization: Bearer <access_token>`
- JWT access token only (no refresh token endpoint implemented yet)
- Default token TTL: 15 minutes (`app.jwt.expiration-minutes`)

### 2.4 Standard Response Envelope

All endpoints return a unified envelope:

```json
{
  "success": true,
  "data": {},
  "meta": {
    "requestId": "uuid",
    "timestamp": "2026-03-14T05:00:00Z"
  }
}
```

Error envelope:

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "details": {
      "field": "error message"
    }
  },
  "meta": {
    "requestId": "uuid",
    "timestamp": "2026-03-14T05:00:00Z"
  }
}
```

### 2.5 Date/Time and Currency

- Date-time fields are serialized from backend `LocalDateTime`
- Currency used in pricing responses: `GBP`

## 3. Endpoint Catalog (Implemented)

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/auth/register` | Public | Register customer account and issue access token |
| POST | `/auth/login` | Public | Login and issue access token |
| GET | `/users/me` | Bearer token | Get current user profile |
| GET | `/hire-options` | Public | List active hire options |
| POST | `/pricing/quotes` | Public | Quote price by hire option code |
| POST | `/bookings` | Bearer token | Create booking for current user |
| GET | `/bookings` | Bearer token | Query current user's bookings |
| GET | `/bookings/{bookingId}` | Bearer token | Query booking detail |
| PATCH | `/bookings/{bookingId}` | Bearer token | Modify booking |
| POST | `/bookings/{bookingId}/cancel` | Bearer token | Cancel booking |
| GET | `/scooters/ids` | Public | Query scooter IDs by scooter status |
| GET | `/health` | Public | Health check |

## 4. ID1: Auth and Account APIs

### 4.1 POST `/api/v1/auth/register`

Request:

```json
{
  "email": "user@example.com",
  "password": "Passw0rd!",
  "fullName": "Urbanova User",
  "phone": "12345678"
}
```

Validation:
- `email` required, valid email format
- `password` required, length 8-72
- `fullName` required, max 100 chars
- `phone` optional, max 30 chars

### 4.2 POST `/api/v1/auth/login`

Request:

```json
{
  "email": "user@example.com",
  "password": "Passw0rd!"
}
```

### 4.3 GET `/api/v1/users/me`

Header:
- `Authorization: Bearer <access_token>`

### 4.4 Auth Response Payload (register/login)

```json
{
  "accessToken": "jwt-token",
  "tokenType": "Bearer",
  "expiresInSeconds": 900,
  "user": {
    "userId": "uuid",
    "email": "user@example.com",
    "fullName": "Urbanova User",
    "phone": "12345678",
    "role": "CUSTOMER",
    "discountCategory": "NONE",
    "accountStatus": "ACTIVE",
    "createdAt": "2026-03-14T13:00:00"
  }
}
```

## 5. ID4: Hire Options and Pricing APIs

### 5.1 GET `/api/v1/hire-options`

Returns active options sorted by duration.

Response `data` item:

```json
{
  "hireOptionId": "HIRE-H1",
  "code": "H1",
  "durationMinutes": 60,
  "basePrice": 3.0,
  "active": true
}
```

### 5.2 POST `/api/v1/pricing/quotes`

Request:

```json
{
  "scooterId": "SCO-0001",
  "hireOptionCode": "H4"
}
```

Validation:
- `hireOptionCode` required
- `scooterId` optional (currently not used in pricing calculation)

Response `data`:

```json
{
  "basePrice": 12.0,
  "appliedDiscounts": [],
  "finalPrice": 12.0,
  "currency": "GBP"
}
```

## 6. Booking APIs (Create / Query / Modify / Cancel)

### 6.1 POST `/api/v1/bookings`

Header:
- `Authorization: Bearer <access_token>`

Request:

```json
{
  "scooterId": "SCO-0001",
  "hireOptionId": "HIRE-H1",
  "plannedStartAt": "2026-03-14T13:30:00"
}
```

Validation and behavior:
- `scooterId` required
- `hireOptionId` required
- `hireOptionId` accepts either `hire_option_id` (e.g. `HIRE-H1`) or code (e.g. `H1`)
- Scooter must be in `AVAILABLE` status
- Scooter reservation update is conditional (`AVAILABLE` -> `RESERVED`) to reduce race conflicts
- Created booking status is `CONFIRMED`
- Created booking payment status is `UNPAID`

Response `data`:

```json
{
  "bookingId": "BKG-1234abcd",
  "status": "CONFIRMED",
  "scooterStatusSnapshot": "RESERVED",
  "startAt": "2026-03-14T13:30:00",
  "endAt": "2026-03-14T14:30:00",
  "priceBreakdown": {
    "base": 3.0,
    "discount": 0.0,
    "finalPrice": 3.0
  }
}
```

### 6.2 GET `/api/v1/bookings`

Header:
- `Authorization: Bearer <access_token>`

Query params:
- `status` optional, values: `CONFIRMED` / `CANCELLED`

Response `data`:

```json
[
  {
    "bookingId": "BKG-1234abcd",
    "bookingRef": "REF-1234abcd56",
    "scooterId": "SCO-0001",
    "hireOptionId": "HIRE-H1",
    "status": "CONFIRMED",
    "startAt": "2026-03-14T13:30:00",
    "endAt": "2026-03-14T14:30:00",
    "priceFinal": 3.0,
    "paymentStatus": "UNPAID",
    "updatedAt": "2026-03-14T13:00:00"
  }
]
```

### 6.3 GET `/api/v1/bookings/{bookingId}`

Header:
- `Authorization: Bearer <access_token>`

Behavior:
- Only booking owner can query detail

Response `data`:

```json
{
  "bookingId": "BKG-1234abcd",
  "bookingRef": "REF-1234abcd56",
  "customerType": "REGISTERED",
  "userId": "uuid",
  "scooterId": "SCO-0001",
  "hireOptionId": "HIRE-H1",
  "status": "CONFIRMED",
  "startAt": "2026-03-14T13:30:00",
  "endAt": "2026-03-14T14:30:00",
  "priceBase": 3.0,
  "priceDiscount": 0.0,
  "priceFinal": 3.0,
  "paymentStatus": "UNPAID",
  "cancelReason": null,
  "createdAt": "2026-03-14T13:00:00",
  "updatedAt": "2026-03-14T13:00:00"
}
```

### 6.4 PATCH `/api/v1/bookings/{bookingId}`

Header:
- `Authorization: Bearer <access_token>`

Request (at least one field required):

```json
{
  "scooterId": "SCO-0002",
  "hireOptionId": "H4",
  "plannedStartAt": "2026-03-14T15:00:00"
}
```

Behavior:
- Only booking owner can modify
- Only `CONFIRMED` bookings can be modified
- If scooter is changed, new scooter must be `AVAILABLE`; booking keeps old scooter release + new scooter reserve in one transaction
- If `hireOptionId` changed, accepts `hire_option_id` or code and recalculates duration/end time/price
- If `plannedStartAt` changed, end time recalculated from hire option duration

Response `data`:
- same schema as `GET /api/v1/bookings/{bookingId}`

### 6.5 POST `/api/v1/bookings/{bookingId}/cancel`

Header:
- `Authorization: Bearer <access_token>`

Request body is optional. If provided:

```json
{
  "reason": "change of plan"
}
```

Behavior:
- Only booking owner can cancel
- Only `CONFIRMED` bookings can transition to `CANCELLED`
- Cancelling releases scooter from `RESERVED` to `AVAILABLE`
- If already `CANCELLED`, API returns cancelled state directly

Response `data`:

```json
{
  "bookingId": "BKG-1234abcd",
  "status": "CANCELLED",
  "cancelledAt": "2026-03-14T13:40:00"
}
```

## 7. Scooter API (Status Filtered ID Query)

### 7.1 GET `/api/v1/scooters/ids`

Query params:
- `status` required, values:
  - `AVAILABLE`
  - `RESERVED`
  - `IN_USE`
  - `MAINTENANCE`
  - `UNAVAILABLE`

Response `data`:

```json
{
  "status": "AVAILABLE",
  "scooterIds": ["SCO-0001", "SCO-0002"]
}
```

## 8. State Transitions (Current Implementation)

### 8.1 Booking

| From | Endpoint | To |
|---|---|---|
| `CONFIRMED` | `PATCH /bookings/{bookingId}` | `CONFIRMED` |
| `CONFIRMED` | `POST /bookings/{bookingId}/cancel` | `CANCELLED` |

### 8.2 Scooter

| From | Trigger | To |
|---|---|---|
| `AVAILABLE` | `POST /bookings` | `RESERVED` |
| `RESERVED` | `PATCH /bookings/{bookingId}` with scooter changed | `AVAILABLE` |
| `AVAILABLE` | `PATCH /bookings/{bookingId}` with scooter changed | `RESERVED` |
| `RESERVED` | `POST /bookings/{bookingId}/cancel` | `AVAILABLE` |

## 9. Error Codes (Currently Used)

| Code | HTTP | Meaning |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Request payload validation failed |
| `AUTH_INVALID_CREDENTIALS` | 401 | Invalid login/token/user |
| `AUTH_TOKEN_EXPIRED` | 401 | Access token expired |
| `AUTH_FORBIDDEN` | 401/403 | Missing token or no permission |
| `RESOURCE_NOT_FOUND` | 404 | User/scooter/hire option/booking not found |
| `SCOOTER_NOT_AVAILABLE` | 409 | Scooter is not available for booking/update |
| `BOOKING_CONFLICT` | 409 | Booking state conflict during modify/cancel |
| `INTERNAL_ERROR` | 500 | Unhandled server error |

## 10. Database (Current Implementation)

Bootstrapped by Spring SQL init:
- `schema.sql`: creates tables `users`, `hire_options`, `scooters`, `bookings`
- `data.sql`: seeds hire options (`H1/H4/D1/W1`) and scooters (`SCO-0001`..`SCO-0005`)

Relevant config:
- `spring.sql.init.mode=always`
- `spring.datasource.*` from environment/profile
