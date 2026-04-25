# Urbanova API Design

Last updated: 2026-04-25

## 1. Scope

This document describes the backend APIs that are actually implemented in the current repository.

Covered backlog:
- ID 1: user account and login
- ID 2: saved card details
- ID 3: account security and admin audit support
- ID 4: hire options and price quote
- ID 5: booking creation
- ID 6: simulated card payment
- ID 7: booking confirmation and notification resend
- ID 8: booking confirmation storage and on-demand display
- ID 9: staff bookings for unregistered users
- ID 10: scooter availability updates
- ID 11: booking extension
- ID 12: booking cancellation
- ID 13: issue / fault submission
- ID 14: issue prioritization and resolution
- ID 15: high-priority issue queue
- ID 16: manager scooter / hire / type management
- ID 17: scooter list and availability display
- ID 18: scooter map points
- ID 19: weekly revenue by hire option
- ID 20: combined daily revenue in one week
- ID 21: weekly revenue chart payload
- ID 22: discounts for frequent / student / senior
- ID 23: multi-client support through transactional reservation and optimistic scooter updates

Notes:
- ID 24 and ID 25 are frontend concerns. No dedicated backend endpoints are required for them.
- Extra retained endpoints also exist: `/scooters/ids`, `/scooter-types/*`, `/admin/scooter-types/*`, `/bookings/{id}` patch update.

## 2. Global Conventions

### 2.1 Base URL

- Base URL: `/api/v1`

### 2.2 Content Type

- Request: `Content-Type: application/json`
- Response: `Content-Type: application/json`

### 2.3 Authentication

- Protected endpoints use `Authorization: Bearer <access_token>`
- Access token: JWT
- Refresh token: opaque token stored in `auth_sessions`
- Current default access token TTL: 15 minutes
- Current default refresh token TTL: 14 days

### 2.4 Roles

| Role | Meaning |
|---|---|
| `CUSTOMER` | registered end user |
| `STAFF` | staff operator |
| `MANAGER` | manager / admin |

### 2.5 Standard Envelope

Success:

```json
{
  "success": true,
  "data": {},
  "meta": {
    "requestId": "uuid",
    "timestamp": "2026-04-25T07:00:00Z"
  }
}
```

Failure:

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "details": {}
  },
  "meta": {
    "requestId": "uuid",
    "timestamp": "2026-04-25T07:00:00Z"
  }
}
```

### 2.6 Date and Currency

- Datetime values are serialized from backend `LocalDateTime`
- Currency used by pricing and analytics is `GBP`

## 3. Implemented Endpoint Catalog

### 3.1 Authentication and User Account

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/auth/register` | Public | Register customer and issue tokens |
| POST | `/auth/login` | Public | Login and issue tokens |
| POST | `/auth/refresh` | Public | Rotate refresh token and issue a new access token |
| POST | `/auth/logout` | Bearer | Revoke current refresh session or all active sessions |
| POST | `/auth/password/forgot` | Public | Create password reset token |
| POST | `/auth/password/reset` | Public | Reset password with reset token |
| GET | `/users/me` | Bearer | Current user profile |
| PATCH | `/users/me` | Bearer | Update current user profile |
| GET | `/users/me/usage-summary` | Bearer | Booking and spend summary for current user |
| GET | `/admin/users` | MANAGER | List users |
| GET | `/admin/users/{userId}` | MANAGER | User detail |
| PATCH | `/admin/users/{userId}/status` | MANAGER | Update account status |
| GET | `/admin/users/{userId}/bookings` | MANAGER | User booking history |
| GET | `/admin/audit-logs` | MANAGER | Manager audit log view |

### 3.2 Payment Methods

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/payment-methods` | Bearer | List current user's saved cards |
| POST | `/payment-methods` | Bearer | Save tokenized card metadata |
| PATCH | `/payment-methods/{paymentMethodId}` | Bearer | Update expiry / label / default |
| DELETE | `/payment-methods/{paymentMethodId}` | Bearer | Mark payment method as removed |
| POST | `/payment-methods/{paymentMethodId}/default` | Bearer | Set default card |

### 3.3 Pricing, Hire Options, Discounts

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/hire-options` | Public | Active hire options |
| POST | `/pricing/quotes` | Public, optional Bearer | Price quote with discount preview when token is provided |
| GET | `/discounts/eligibility` | Bearer | Current user discount eligibility |
| GET | `/admin/hire-options` | MANAGER | List all hire options |
| POST | `/admin/hire-options` | MANAGER | Create hire option |
| PATCH | `/admin/hire-options/{hireOptionId}` | MANAGER | Update hire option |
| DELETE | `/admin/hire-options/{hireOptionId}` | MANAGER | Disable hire option |
| GET | `/admin/discount-rules` | MANAGER | List discount rules |
| POST | `/admin/discount-rules` | MANAGER | Create discount rule |
| PATCH | `/admin/discount-rules/{discountRuleId}` | MANAGER | Update discount rule |

### 3.4 Scooters and Map

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/scooters` | Public | Scooter list with type, location and status |
| GET | `/scooters/{scooterId}` | Public | Scooter detail |
| GET | `/scooters/availability` | Public | Status counts summary |
| GET | `/scooters/ids` | Public | Scooter IDs filtered by status |
| GET | `/scooters/map-points` | Public | Map points for tracked scooters |
| GET | `/scooter-types` | Public | Active scooter types |
| GET | `/scooter-types/{typeCode}` | Public | Scooter type detail |
| GET | `/admin/scooters` | MANAGER | Admin scooter inventory |
| POST | `/admin/scooters` | MANAGER | Create scooter |
| PATCH | `/admin/scooters/{scooterId}` | MANAGER | Update scooter |
| PATCH | `/admin/scooters/{scooterId}/status` | MANAGER | Override scooter status |
| POST | `/admin/scooters/bulk-status` | MANAGER | Bulk update scooter statuses |
| GET | `/admin/scooter-types` | MANAGER | List scooter types |
| POST | `/admin/scooter-types` | MANAGER | Create scooter type |
| PATCH | `/admin/scooter-types/{typeCode}` | MANAGER | Update scooter type |
| DELETE | `/admin/scooter-types/{typeCode}` | MANAGER | Disable scooter type |

### 3.5 Bookings

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/bookings` | CUSTOMER | Create booking |
| GET | `/bookings` | CUSTOMER | List current user's bookings |
| GET | `/bookings/{bookingId}` | CUSTOMER | Booking detail |
| PATCH | `/bookings/{bookingId}` | CUSTOMER | Modify booking before settlement / start |
| POST | `/bookings/{bookingId}/start` | CUSTOMER | Start confirmed booking |
| POST | `/bookings/{bookingId}/end` | CUSTOMER | End active booking |
| POST | `/bookings/{bookingId}/extend` | CUSTOMER | Extend booking |
| POST | `/bookings/{bookingId}/cancel` | CUSTOMER | Cancel booking |
| GET | `/bookings/{bookingId}/timeline` | CUSTOMER | Booking event timeline |
| POST | `/staff/bookings/guest` | STAFF | Create guest booking |
| GET | `/staff/bookings/guest/{bookingId}` | STAFF/MANAGER | Guest booking detail |
| GET | `/admin/bookings` | MANAGER | Filtered booking list |
| GET | `/admin/bookings/{bookingId}` | MANAGER | Booking detail with payments and timeline |
| PATCH | `/admin/bookings/{bookingId}/override` | MANAGER | Manager override of booking fields |

### 3.6 Payments, Confirmations, Notifications

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/bookings/{bookingId}/payments` | CUSTOMER/STAFF/MANAGER | Create simulated payment |
| GET | `/bookings/{bookingId}/payments` | CUSTOMER/STAFF/MANAGER | Payment list for booking |
| GET | `/payments/{paymentId}` | CUSTOMER/STAFF/MANAGER | Payment detail |
| POST | `/payments/{paymentId}/simulate-settlement` | MANAGER | Settle a deferred payment |
| POST | `/payments/{paymentId}/refund` | MANAGER | Refund payment partially or fully |
| GET | `/bookings/{bookingId}/confirmation` | Bearer | Latest booking confirmation |
| POST | `/bookings/{bookingId}/confirmation/resend` | Bearer | Resend booking confirmation |
| GET | `/confirmations` | Bearer | Current user's confirmations |
| GET | `/notifications` | Bearer | Current user's notifications |
| PATCH | `/notifications/{notificationId}/read` | Bearer | Mark notification as read |

### 3.7 Issues

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/issues` | Bearer | Create issue |
| GET | `/issues` | Bearer | List current user's issues |
| GET | `/issues/{issueId}` | Bearer | Issue detail |
| POST | `/issues/{issueId}/comments` | Bearer | Add issue comment |
| GET | `/admin/issues` | MANAGER | Admin issue queue |
| PATCH | `/admin/issues/{issueId}/priority` | MANAGER | Update issue priority |
| PATCH | `/admin/issues/{issueId}/status` | MANAGER | Update issue status |
| POST | `/admin/issues/{issueId}/resolve` | MANAGER | Resolve issue |
| GET | `/admin/issues/high-priority` | MANAGER | High / critical issue queue |

### 3.8 Analytics and Ops

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/admin/analytics/revenue/estimate` | MANAGER | Revenue estimate in date range |
| GET | `/admin/analytics/revenue/weekly-by-hire-option` | MANAGER | Weekly totals grouped by hire option |
| GET | `/admin/analytics/revenue/daily-combined` | MANAGER | Daily revenue over one week |
| GET | `/admin/analytics/revenue/weekly-chart` | MANAGER | Chart-friendly revenue series |
| GET | `/admin/analytics/usage/frequent-users` | MANAGER | Frequent user list |
| GET | `/health` | Public | Health check |
| GET | `/meta` | Public | API metadata |

## 4. Request Notes by Domain

### 4.1 Auth

`POST /api/v1/auth/register`

```json
{
  "email": "user@example.com",
  "password": "Passw0rd!",
  "fullName": "Urbanova User",
  "phone": "12345678"
}
```

`POST /api/v1/auth/login`

```json
{
  "email": "user@example.com",
  "password": "Passw0rd!"
}
```

Successful auth response:

```json
{
  "sessionId": "SES-4D5D8E7A3B1C",
  "accessToken": "jwt",
  "refreshToken": "opaque-refresh-token",
  "tokenType": "Bearer",
  "expiresInSeconds": 900,
  "refreshExpiresInSeconds": 1209600,
  "user": {
    "userId": "uuid",
    "email": "user@example.com",
    "fullName": "Urbanova User",
    "phone": "12345678",
    "role": "CUSTOMER",
    "discountCategory": "NONE",
    "accountStatus": "ACTIVE",
    "createdAt": "2026-04-25T13:00:00"
  }
}
```

`POST /api/v1/auth/refresh`

```json
{
  "refreshToken": "opaque-refresh-token"
}
```

`POST /api/v1/auth/password/forgot`

Current implementation is coursework-oriented:
- it creates a reset token in `password_reset_tokens`
- it returns the token in the response body instead of sending a real email

`POST /api/v1/auth/password/reset`

```json
{
  "resetToken": "token-from-forgot-password",
  "newPassword": "NewPassw0rd!"
}
```

### 4.2 Payment Method

`POST /api/v1/payment-methods`

```json
{
  "brand": "VISA",
  "cardNumber": "4111111111111111",
  "expiryMonth": 12,
  "expiryYear": 2030,
  "label": "Personal card",
  "isDefault": true
}
```

### 4.3 Pricing and Discounts

`POST /api/v1/pricing/quotes`

```json
{
  "scooterId": "SCO-0001",
  "hireOptionCode": "H4"
}
```

Behavior:
- without bearer token: base price only
- with bearer token: applies currently eligible discount rules

`POST /api/v1/admin/discount-rules`

```json
{
  "type": "FREQUENT_USER",
  "thresholdHoursPerWeek": 8.0,
  "percentage": 15.0,
  "active": true
}
```

### 4.4 Booking

`POST /api/v1/bookings`

```json
{
  "scooterId": "SCO-0001",
  "hireOptionId": "HIRE-H4",
  "plannedStartAt": "2026-04-25T14:00:00"
}
```

Current behavior:
- booking is created as `PENDING_PAYMENT`
- scooter is moved from `AVAILABLE` to `RESERVED`
- booking price already includes eligible discounts

`PATCH /api/v1/bookings/{bookingId}`

Supported fields:
- `scooterId`
- `hireOptionId`
- `plannedStartAt`

Current behavior:
- the booking is recalculated
- payment state is reset to `UNPAID`
- booking status is reset to `PENDING_PAYMENT`

`POST /api/v1/bookings/{bookingId}/extend`

```json
{
  "additionalHireOptionCode": "H1"
}
```

`POST /api/v1/staff/bookings/guest`

```json
{
  "guestName": "Alex Guest",
  "guestEmail": "guest@example.com",
  "guestPhone": "10086",
  "scooterId": "SCO-0002",
  "hireOptionId": "HIRE-H1",
  "plannedStartAt": "2026-04-25T16:00:00"
}
```

`PATCH /api/v1/admin/bookings/{bookingId}/override`

Supported fields:
- `status`
- `paymentStatus`
- `scooterId`
- `cancelReason`

### 4.5 Payments

`POST /api/v1/bookings/{bookingId}/payments`

```json
{
  "method": "SAVED_CARD",
  "paymentMethodId": "PM-1234567890",
  "amount": 10.20,
  "deferSettlement": false,
  "simulatedOutcome": "SUCCESS"
}
```

Notes:
- `method` supports `SAVED_CARD` and `ONE_TIME_CARD`
- `deferSettlement=true` creates a payment in `INITIATED`
- if settlement is not deferred, the current default path immediately simulates success/failure
- a fully paid booking moves from `PENDING_PAYMENT` to `CONFIRMED`
- confirmation and notification records are created when a booking becomes fully paid

`POST /api/v1/payments/{paymentId}/refund`

```json
{
  "amount": 5.00
}
```

If `amount` is omitted, the implementation refunds the remaining refundable balance.

### 4.6 Issues

`POST /api/v1/issues`

```json
{
  "bookingId": "BKG-12345678",
  "scooterId": "SCO-0001",
  "title": "Brake feels weak",
  "description": "Braking distance increased during the ride",
  "priority": "LOW"
}
```

`POST /api/v1/issues/{issueId}/comments`

```json
{
  "message": "Additional detail or manager reply"
}
```

### 4.7 Analytics

Supported query params:
- `startDate=YYYY-MM-DD`
- `endDate=YYYY-MM-DD`

Current implementation uses recorded `payments` rows as revenue source.

## 5. State and Behavior Notes

### 5.1 Booking

Current implemented state flow:
- `PENDING_PAYMENT` -> payment success -> `CONFIRMED`
- `CONFIRMED` -> `/bookings/{id}/start` -> `ACTIVE`
- `ACTIVE` -> `/bookings/{id}/end` -> `COMPLETED`
- `PENDING_PAYMENT` or `CONFIRMED` -> `/bookings/{id}/cancel` -> `CANCELLED`

### 5.2 Scooter

Current implemented transitions:
- `AVAILABLE` -> booking created -> `RESERVED`
- `RESERVED` -> booking start -> `IN_USE`
- `IN_USE` -> booking end -> `AVAILABLE`
- manager endpoints can directly set `AVAILABLE`, `RESERVED`, `IN_USE`, `MAINTENANCE`, `UNAVAILABLE`

### 5.3 Issue

Current implemented values:
- priority: `LOW`, `HIGH`, `CRITICAL`
- status: `OPEN`, `IN_REVIEW`, `RESOLVED`, `CLOSED`

## 6. Security and Concurrency Notes

Implemented security / reliability measures:
- BCrypt password hashing
- JWT access token + stored refresh session rotation
- password reset tokens stored in database
- manager audit logs in `audit_logs`
- booking reservation uses conditional scooter status update
- scooter admin updates increment `version`
- payment, confirmation, notification, issue and booking event records are all persisted in MySQL

This is how the current backend addresses ID 3 and ID 23.

## 7. Backlog Coverage Matrix

| ID | Coverage in Current Backend |
|---|---|
| 1 | `/auth/*`, `/users/me`, `/users/me/usage-summary` |
| 2 | `/payment-methods/*` |
| 3 | JWT + refresh sessions + password reset + admin status update + `/admin/audit-logs` |
| 4 | `/hire-options`, `/pricing/quotes` |
| 5 | `/bookings` |
| 6 | `/bookings/{id}/payments`, `/payments/*` |
| 7 | `/bookings/{id}/confirmation/resend`, `/notifications/*` |
| 8 | `/bookings/{id}/confirmation`, `/confirmations`, `/bookings/{id}`, `/bookings/{id}/timeline` |
| 9 | `/staff/bookings/guest*` |
| 10 | booking lifecycle + `/admin/scooters/{id}/status` + `/admin/scooters/bulk-status` |
| 11 | `/bookings/{id}/extend` |
| 12 | `/bookings/{id}/cancel` |
| 13 | `/issues*` |
| 14 | `/admin/issues/*` |
| 15 | `/admin/issues/high-priority` |
| 16 | `/admin/hire-options/*`, `/admin/scooters/*`, `/admin/scooter-types/*` |
| 17 | `/scooters`, `/scooters/{id}`, `/scooters/availability` |
| 18 | `/scooters/map-points` |
| 19 | `/admin/analytics/revenue/weekly-by-hire-option` |
| 20 | `/admin/analytics/revenue/daily-combined` |
| 21 | `/admin/analytics/revenue/weekly-chart` |
| 22 | `/discounts/eligibility`, `/admin/discount-rules/*`, quote discount preview |
| 23 | transactional reservation, scooter versioning, stored audit/event records |
| 24 | frontend concern, no backend endpoint |
| 25 | frontend concern, no backend endpoint |
