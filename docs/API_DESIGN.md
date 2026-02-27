# Urbanova API Design (v1)

Last updated: 2026-02-24

## 1. Purpose and Scope

This document defines a full REST API design for Urbanova, covering customer, staff, and manager interfaces for the full backlog (ID 1-25).

This design assumes:
- Backend: Spring Boot REST API with JSON payloads.
- Auth: JWT access token + refresh token.
- Payments: simulated card payments (no real payment gateway needed in coursework scope).
- Architecture: single backend service now, internal modularization for future split.

## 2. Global API Conventions

### 2.1 Base URL and Versioning

- Base URL: `/api/v1`
- Versioning strategy: URI versioning.
- Breaking changes: new major path (`/api/v2`).

### 2.2 Content Type

- Request: `Content-Type: application/json`
- Response: `Content-Type: application/json`

### 2.3 Common Headers

- `Authorization: Bearer <access_token>`
- `Idempotency-Key: <uuid>` for create/payment/cancel/extend actions.
- `If-Match: <etag>` for manager updates to mutable resources.
- `X-Request-Id: <uuid>` for tracing.

### 2.4 Roles

| Role | Description |
|---|---|
| `CUSTOMER` | Registered renter |
| `STAFF` | Staff operator (can create guest bookings) |
| `MANAGER` | Admin/operations and analytics |
| `SYSTEM` | Internal async jobs/events |

### 2.5 Standard Response Envelope

```json
{
  "success": true,
  "data": {},
  "meta": {
    "requestId": "2b7a9ebd-8b8b-4d25-916d-b38b8b0987a1",
    "timestamp": "2026-02-24T11:00:00Z"
  }
}
```

```json
{
  "success": false,
  "error": {
    "code": "BOOKING_CONFLICT",
    "message": "Scooter is no longer available",
    "details": {
      "scooterId": "SCO-0008"
    }
  },
  "meta": {
    "requestId": "c9b5d6c4-9c5c-4f87-8ca2-24d7f6bf35d1",
    "timestamp": "2026-02-24T11:00:00Z"
  }
}
```

### 2.6 Pagination and Sorting

- Query params: `page`, `pageSize`, `sortBy`, `sortOrder`.
- Default page size: `20`.
- Max page size: `100`.

### 2.7 Date and Currency

- Date-time format: ISO-8601 UTC.
- Currency: `GBP`.

## 3. Core Resource Models

## 3.1 User

| Field | Type | Notes |
|---|---|---|
| `userId` | string | UUID |
| `email` | string | unique |
| `fullName` | string | |
| `phone` | string | optional |
| `role` | enum | CUSTOMER/STAFF/MANAGER |
| `discountCategory` | enum | NONE/STUDENT/SENIOR |
| `accountStatus` | enum | ACTIVE/SUSPENDED/DELETED |
| `createdAt` | datetime | |

## 3.2 PaymentMethod

| Field | Type | Notes |
|---|---|---|
| `paymentMethodId` | string | UUID |
| `userId` | string | |
| `brand` | string | VISA/MASTERCARD |
| `last4` | string | tokenized only |
| `expiryMonth` | int | |
| `expiryYear` | int | |
| `isDefault` | boolean | |
| `status` | enum | ACTIVE/EXPIRED/REMOVED |

## 3.3 HireOption

| Field | Type | Notes |
|---|---|---|
| `hireOptionId` | string | UUID |
| `code` | enum | H1/H4/D1/W1 |
| `durationMinutes` | int | 60/240/1440/10080 |
| `basePrice` | number | decimal |
| `active` | boolean | manager controlled |

## 3.4 DiscountRule

| Field | Type | Notes |
|---|---|---|
| `discountRuleId` | string | UUID |
| `type` | enum | FREQUENT_USER/STUDENT/SENIOR |
| `thresholdHoursPerWeek` | number | for frequent users |
| `percentage` | number | e.g. 15.0 |
| `active` | boolean | manager controlled |

## 3.5 Scooter

| Field | Type | Notes |
|---|---|---|
| `scooterId` | string | human readable ID |
| `status` | enum | AVAILABLE/RESERVED/IN_USE/MAINTENANCE/UNAVAILABLE |
| `batteryPercent` | int | 0-100 |
| `lat` | number | |
| `lng` | number | |
| `zone` | string | optional |
| `version` | int | for optimistic locking |

## 3.6 Booking

| Field | Type | Notes |
|---|---|---|
| `bookingId` | string | UUID |
| `bookingRef` | string | human reference |
| `customerType` | enum | REGISTERED/GUEST |
| `userId` | string | nullable for guest |
| `guestInfo` | object | name/email/phone for staff bookings |
| `scooterId` | string | |
| `hireOptionId` | string | |
| `startAt` | datetime | planned/actual |
| `endAt` | datetime | planned/actual |
| `status` | enum | PENDING_PAYMENT/CONFIRMED/ACTIVE/COMPLETED/CANCELLED/EXPIRED |
| `priceBreakdown` | object | base, discount, final |
| `paymentStatus` | enum | UNPAID/PAID/PARTIAL/REFUNDED |
| `createdByRole` | enum | CUSTOMER/STAFF |

## 3.7 Payment

| Field | Type | Notes |
|---|---|---|
| `paymentId` | string | UUID |
| `bookingId` | string | |
| `amount` | number | decimal |
| `method` | enum | SAVED_CARD/ONE_TIME_CARD |
| `status` | enum | INITIATED/SUCCEEDED/FAILED/REFUNDED |
| `simulatedOutcome` | enum | SUCCESS/FAILURE |
| `createdAt` | datetime | |

## 3.8 Issue

| Field | Type | Notes |
|---|---|---|
| `issueId` | string | UUID |
| `reporterUserId` | string | |
| `bookingId` | string | optional |
| `scooterId` | string | optional |
| `title` | string | short feedback/fault summary |
| `description` | string | detail |
| `priority` | enum | LOW/HIGH/CRITICAL |
| `status` | enum | OPEN/IN_REVIEW/RESOLVED/CLOSED |
| `managerFeedback` | string | nullable |

## 4. Endpoint Catalog

## 4.1 Authentication and Account APIs

| Method | Path | Role | Purpose | Backlog |
|---|---|---|---|---|
| POST | `/auth/register` | Public | Create customer account | ID1 |
| POST | `/auth/login` | Public | Login and issue tokens | ID1 |
| POST | `/auth/refresh` | Public | Refresh access token | ID1 |
| POST | `/auth/logout` | Authenticated | Invalidate current session | ID1 |
| POST | `/auth/password/forgot` | Public | Start reset flow | ID1 |
| POST | `/auth/password/reset` | Public | Complete reset flow | ID1 |
| GET | `/users/me` | CUSTOMER/STAFF/MANAGER | Current profile | ID1 |
| PATCH | `/users/me` | CUSTOMER/STAFF/MANAGER | Update profile fields | ID1 |
| GET | `/users/me/usage-summary` | CUSTOMER | Personal usage/cost summary | Product brief |
| GET | `/admin/users` | MANAGER | List customers/staff | Manager requirement |
| GET | `/admin/users/{userId}` | MANAGER | User detail | Manager requirement |
| PATCH | `/admin/users/{userId}/status` | MANAGER | Suspend/reactivate user | ID3 |
| GET | `/admin/users/{userId}/bookings` | MANAGER | User booking history | Manager requirement |

## 4.2 Payment Method APIs

| Method | Path | Role | Purpose | Backlog |
|---|---|---|---|---|
| GET | `/payment-methods` | CUSTOMER | List saved cards | ID2 |
| POST | `/payment-methods` | CUSTOMER | Save tokenized card | ID2 |
| PATCH | `/payment-methods/{paymentMethodId}` | CUSTOMER | Update expiry/label/default | ID2 |
| DELETE | `/payment-methods/{paymentMethodId}` | CUSTOMER | Remove saved card | ID2 |
| POST | `/payment-methods/{paymentMethodId}/default` | CUSTOMER | Set default card | ID2 |

## 4.3 Pricing, Hire Option, Discount APIs

| Method | Path | Role | Purpose | Backlog |
|---|---|---|---|---|
| GET | `/hire-options` | Public | View hire options and base costs | ID4 |
| POST | `/pricing/quotes` | CUSTOMER/STAFF | Price quote with discount preview | ID4, ID22 |
| GET | `/discounts/eligibility` | CUSTOMER | Current user discount eligibility | ID22 |
| GET | `/admin/hire-options` | MANAGER | Manage hire option list | ID16 |
| POST | `/admin/hire-options` | MANAGER | Add hire option | ID16 |
| PATCH | `/admin/hire-options/{hireOptionId}` | MANAGER | Update cost/duration | ID16 |
| DELETE | `/admin/hire-options/{hireOptionId}` | MANAGER | Disable option | ID16 |
| GET | `/admin/discount-rules` | MANAGER | List discount rules | ID22 |
| POST | `/admin/discount-rules` | MANAGER | Create discount rule | ID22 |
| PATCH | `/admin/discount-rules/{discountRuleId}` | MANAGER | Update rule thresholds and rates | ID22 |

## 4.4 Scooter and Map APIs

| Method | Path | Role | Purpose | Backlog |
|---|---|---|---|---|
| GET | `/scooters` | Public | List scooters with availability and location | ID17 |
| GET | `/scooters/{scooterId}` | Public | Scooter details | ID17 |
| GET | `/scooters/map-points` | Public | Map data for all tracked scooters | ID18 |
| GET | `/scooters/availability` | Public | Availability summary counts | ID17 |
| GET | `/admin/scooters` | MANAGER | Full scooter inventory view | ID16 |
| POST | `/admin/scooters` | MANAGER | Add scooter | ID16 |
| PATCH | `/admin/scooters/{scooterId}` | MANAGER | Update scooter details | ID16 |
| PATCH | `/admin/scooters/{scooterId}/status` | MANAGER | Manual status override | ID10, ID16 |
| POST | `/admin/scooters/bulk-status` | MANAGER | Bulk maintenance/availability updates | ID16 |

## 4.5 Booking APIs

| Method | Path | Role | Purpose | Backlog |
|---|---|---|---|---|
| POST | `/bookings` | CUSTOMER | Create booking for registered user | ID5 |
| GET | `/bookings` | CUSTOMER | List own bookings | ID8 |
| GET | `/bookings/{bookingId}` | CUSTOMER | Booking detail | ID8 |
| POST | `/bookings/{bookingId}/start` | CUSTOMER | Start ongoing booking | Product brief |
| POST | `/bookings/{bookingId}/end` | CUSTOMER | End ongoing booking | Product brief |
| POST | `/bookings/{bookingId}/extend` | CUSTOMER | Extend fixed booking duration | ID11 |
| POST | `/bookings/{bookingId}/cancel` | CUSTOMER | Cancel booking | ID12 |
| GET | `/bookings/{bookingId}/timeline` | CUSTOMER | Booking events audit | ID8 |
| POST | `/staff/bookings/guest` | STAFF | Create booking for unregistered user | ID9 |
| GET | `/staff/bookings/guest/{bookingId}` | STAFF | Guest booking detail | ID9 |
| GET | `/admin/bookings` | MANAGER | All bookings with filters | Manager requirement |
| GET | `/admin/bookings/{bookingId}` | MANAGER | Booking detail and payment info | Manager requirement |
| PATCH | `/admin/bookings/{bookingId}/override` | MANAGER | Manager corrective action | Ops need |

## 4.6 Payment APIs

| Method | Path | Role | Purpose | Backlog |
|---|---|---|---|---|
| POST | `/bookings/{bookingId}/payments` | CUSTOMER/STAFF | Initiate simulated payment | ID6 |
| GET | `/bookings/{bookingId}/payments` | CUSTOMER/STAFF/MANAGER | Payment list for booking | ID6 |
| GET | `/payments/{paymentId}` | CUSTOMER/STAFF/MANAGER | Payment detail | ID6 |
| POST | `/payments/{paymentId}/simulate-settlement` | SYSTEM | Apply simulated result | ID6 |
| POST | `/payments/{paymentId}/refund` | MANAGER/SYSTEM | Refund after cancellation rules | ID12 |

## 4.7 Confirmation and Notification APIs

| Method | Path | Role | Purpose | Backlog |
|---|---|---|---|---|
| GET | `/bookings/{bookingId}/confirmation` | CUSTOMER/STAFF | Fetch stored confirmation | ID8 |
| POST | `/bookings/{bookingId}/confirmation/resend` | CUSTOMER/STAFF | Resend confirmation email | ID7 |
| GET | `/confirmations` | CUSTOMER | List confirmation history | ID8 |
| GET | `/notifications` | CUSTOMER | User notifications/inbox | ID7 |
| PATCH | `/notifications/{notificationId}/read` | CUSTOMER | Mark notification read | ID7 |

## 4.8 Issue and Fault APIs

| Method | Path | Role | Purpose | Backlog |
|---|---|---|---|---|
| POST | `/issues` | CUSTOMER | Submit issue/fault feedback | ID13 |
| GET | `/issues` | CUSTOMER | List own issues | ID13 |
| GET | `/issues/{issueId}` | CUSTOMER | Issue detail and status | ID13 |
| POST | `/issues/{issueId}/comments` | CUSTOMER | Add follow-up comment | ID13 |
| GET | `/admin/issues` | MANAGER | Issue queue with filters | ID14 |
| PATCH | `/admin/issues/{issueId}/priority` | MANAGER | Prioritize/escalate issue | ID14 |
| PATCH | `/admin/issues/{issueId}/status` | MANAGER | Move issue in workflow | ID14 |
| POST | `/admin/issues/{issueId}/resolve` | MANAGER | Resolve with feedback | ID14 |
| GET | `/admin/issues/high-priority` | MANAGER | Dedicated high-priority list | ID15 |

## 4.9 Analytics and Revenue APIs

| Method | Path | Role | Purpose | Backlog |
|---|---|---|---|---|
| GET | `/admin/analytics/revenue/estimate` | MANAGER | Revenue estimate by date range | Product brief |
| GET | `/admin/analytics/revenue/weekly-by-hire-option` | MANAGER | Weekly income by 1h/4h/1d/1w | ID19 |
| GET | `/admin/analytics/revenue/daily-combined` | MANAGER | Combined daily income in one week | ID20 |
| GET | `/admin/analytics/revenue/weekly-chart` | MANAGER | Chart-friendly timeseries payload | ID21 |
| GET | `/admin/analytics/usage/frequent-users` | MANAGER | Users crossing frequent threshold | ID22 |

## 4.10 System and Operational APIs

| Method | Path | Role | Purpose | Backlog |
|---|---|---|---|---|
| GET | `/health` | Public | Service health check | Ops |
| GET | `/meta` | Public | Version/build metadata | Ops |
| GET | `/admin/audit-logs` | MANAGER | Admin action traceability | ID3 |

## 5. Critical Workflow API Examples

## 5.1 Price Quote

`POST /api/v1/pricing/quotes`

```json
{
  "scooterId": "SCO-0008",
  "hireOptionCode": "H4",
  "userContext": {
    "isAuthenticated": true
  }
}
```

```json
{
  "success": true,
  "data": {
    "basePrice": 12.0,
    "appliedDiscounts": [
      {
        "type": "STUDENT",
        "amount": 1.8
      }
    ],
    "finalPrice": 10.2,
    "currency": "GBP"
  }
}
```

## 5.2 Create Booking

`POST /api/v1/bookings`

Headers:
- `Authorization: Bearer <token>`
- `Idempotency-Key: 0be77e99-c2a5-4f61-a444-706f2d8e1db9`

```json
{
  "scooterId": "SCO-0008",
  "hireOptionId": "HIRE-4H",
  "plannedStartAt": "2026-02-24T12:00:00Z",
  "paymentPreference": {
    "type": "SAVED_CARD",
    "paymentMethodId": "PM-123"
  }
}
```

```json
{
  "success": true,
  "data": {
    "bookingId": "BKG-9e7100f0",
    "status": "PENDING_PAYMENT",
    "scooterStatusSnapshot": "RESERVED",
    "priceBreakdown": {
      "base": 12.0,
      "discount": 1.8,
      "final": 10.2
    }
  }
}
```

## 5.3 Extend Booking

`POST /api/v1/bookings/{bookingId}/extend`

```json
{
  "additionalHireOptionCode": "H1"
}
```

```json
{
  "success": true,
  "data": {
    "bookingId": "BKG-9e7100f0",
    "oldEndAt": "2026-02-24T16:00:00Z",
    "newEndAt": "2026-02-24T17:00:00Z",
    "additionalCharge": 3.0
  }
}
```

## 5.4 Report Issue

`POST /api/v1/issues`

```json
{
  "bookingId": "BKG-9e7100f0",
  "scooterId": "SCO-0008",
  "title": "Brake feels weak",
  "description": "Braking distance increased in last 10 minutes"
}
```

```json
{
  "success": true,
  "data": {
    "issueId": "ISS-11d0a77f",
    "priority": "LOW",
    "status": "OPEN"
  }
}
```

## 6. State and Transition Rules

## 6.1 Booking State Machine

| From | Endpoint | To | Rule |
|---|---|---|---|
| `PENDING_PAYMENT` | `POST /bookings/{id}/payments` success | `CONFIRMED` | payment required |
| `CONFIRMED` | `POST /bookings/{id}/start` | `ACTIVE` | start time reached or manager override |
| `ACTIVE` | `POST /bookings/{id}/end` | `COMPLETED` | scooter released to AVAILABLE |
| `CONFIRMED` | `POST /bookings/{id}/cancel` | `CANCELLED` | cancellation policy applied |
| `ACTIVE` | `POST /bookings/{id}/extend` | `ACTIVE` | extension and extra charge |
| `PENDING_PAYMENT` | timeout job | `EXPIRED` | reservation released |

## 6.2 Scooter State Machine

| From | Trigger | To |
|---|---|---|
| `AVAILABLE` | booking created | `RESERVED` |
| `RESERVED` | booking start | `IN_USE` |
| `IN_USE` | booking end | `AVAILABLE` |
| any | manager maintenance action | `MAINTENANCE` |
| any | manager disable action | `UNAVAILABLE` |

## 6.3 Issue State Machine

| From | Endpoint | To |
|---|---|---|
| `OPEN` | `PATCH /admin/issues/{id}/status` | `IN_REVIEW` |
| `IN_REVIEW` | `PATCH /admin/issues/{id}/priority` high | `IN_REVIEW` |
| `IN_REVIEW` | `POST /admin/issues/{id}/resolve` | `RESOLVED` |
| `RESOLVED` | `PATCH /admin/issues/{id}/status` | `CLOSED` |

## 7. Error Code Set

| Code | HTTP | Meaning |
|---|---|---|
| `AUTH_INVALID_CREDENTIALS` | 401 | login failure |
| `AUTH_TOKEN_EXPIRED` | 401 | access token expired |
| `AUTH_FORBIDDEN` | 403 | role not allowed |
| `VALIDATION_ERROR` | 400 | payload format invalid |
| `RESOURCE_NOT_FOUND` | 404 | missing resource |
| `BOOKING_CONFLICT` | 409 | booking race conflict |
| `SCOOTER_NOT_AVAILABLE` | 409 | scooter unavailable |
| `PAYMENT_FAILED` | 402 | simulated payment failed |
| `IDEMPOTENCY_KEY_REUSED` | 409 | mismatched idempotent replay |
| `PRECONDITION_FAILED` | 412 | etag mismatch |
| `RATE_LIMITED` | 429 | too many requests |
| `INTERNAL_ERROR` | 500 | unhandled server error |

## 8. Security and Data Protection Requirements

- Password hashing with Argon2 or BCrypt.
- Access token TTL: 15 minutes.
- Refresh token TTL: 7-30 days with rotation.
- Card data storage: token + masked PAN only.
- PII encryption at rest for sensitive fields.
- Audit logs for manager actions and payment state changes.
- Login rate limiting and brute-force lockout.

These items directly support NF security expectation in ID3.

## 9. Concurrency and Reliability Rules

- Booking creation must be idempotent via `Idempotency-Key`.
- Payment initiation and refund must be idempotent.
- Scooter and hire-option manager updates should use optimistic locking (`If-Match` + `version`).
- Async events required:
- `booking.confirmed`
- `booking.cancelled`
- `payment.succeeded`
- `payment.failed`
- `issue.prioritized`
- Email sending and notification creation are consumer jobs on these events.

These rules support simultaneous multi-client usage in ID23.

## 10. Backlog-to-API Coverage Matrix

| Backlog ID | Requirement | API Coverage |
|---|---|---|
| 1 | User account and login | `/auth/*`, `/users/*` |
| 2 | Save card details | `/payment-methods/*` |
| 3 | Security for accounts | auth/security standards, audit logs |
| 4 | View hire options/costs | `/hire-options`, `/pricing/quotes` |
| 5 | Book scooter with ID and period | `/bookings` |
| 6 | Simulated card payment | `/bookings/{id}/payments`, `/payments/*` |
| 7 | Email confirmation | `/bookings/{id}/confirmation/resend` |
| 8 | Store and view confirmations | `/bookings/{id}/confirmation`, `/confirmations` |
| 9 | Staff bookings for unregistered | `/staff/bookings/guest` |
| 10 | Update scooter availability | booking transitions + `/admin/scooters/*/status` |
| 11 | Extend booking | `/bookings/{id}/extend` |
| 12 | Cancel booking | `/bookings/{id}/cancel` |
| 13 | Submit issue/fault feedback | `/issues` |
| 14 | Prioritize and resolve issues | `/admin/issues/*` |
| 15 | View high-priority issues | `/admin/issues/high-priority` |
| 16 | Configure scooter details/costs | `/admin/scooters/*`, `/admin/hire-options/*` |
| 17 | Scooter availability/location list | `/scooters`, `/scooters/availability` |
| 18 | Visual map locations | `/scooters/map-points` |
| 19 | Weekly income by hire option | `/admin/analytics/revenue/weekly-by-hire-option` |
| 20 | Combined daily income in week | `/admin/analytics/revenue/daily-combined` |
| 21 | Weekly income graph data | `/admin/analytics/revenue/weekly-chart` |
| 22 | Discounts for frequent/student/senior | `/discounts/eligibility`, `/admin/discount-rules/*` |
| 23 | Multi-client concurrency | idempotency + optimistic locking + conflict errors |
| 24 | Responsive UI | frontend concern, API provides mobile-friendly JSON |
| 25 | Accessibility | frontend concern, API provides clear semantic error messages |

## 11. Recommended Delivery Priority

| Phase | Must-have APIs |
|---|---|
| Sprint 1 | ID1, ID4, ID5, ID12 (`/auth/*`, `/hire-options`, `/bookings`, cancel) |
| Sprint 2 | Payments, confirmations, scooter/map, manager config (`ID6-10,16-18`) |
| Sprint 3 | Issues workflow, analytics, discounts (`ID11,13-15,19-22`) |
| Hardening | Security, concurrency, audit, performance (`ID3,23`) |

