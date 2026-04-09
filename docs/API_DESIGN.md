# Urbanova API Design (Implemented v1)

Last updated: 2026-04-09

## 1. Purpose and Scope

This document describes the **currently implemented** backend APIs in this repository.

Implemented backlog/new scope:
- ID 1: User registration & login
- ID 4: View hire options and cost
- ID 5: Book an e-scooter
- ID 12: Cancel booking
- Booking query and booking modify APIs
- ID 16: Manager hire option and scooter management APIs
- ID 18: Scooter map location API
- Scooter type catalog with static image URLs

Not yet implemented from the previous full design draft (examples): refresh/logout/password reset, payments, confirmations, staff APIs, analytics, issue workflow, and discount rules.

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
- Manager endpoints require authenticated user role `MANAGER`

### 2.4 Standard Response Envelope

All endpoints return a unified envelope:

```json
{
  "success": true,
  "data": {},
  "meta": {
    "requestId": "uuid",
    "timestamp": "2026-04-09T05:00:00Z"
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
    "timestamp": "2026-04-09T05:00:00Z"
  }
}
```

### 2.5 Date/Time and Currency

- Date-time fields are serialized from backend `LocalDateTime`
- Currency used in pricing responses: `GBP`

### 2.6 Static Images

- Scooter type images are served by Spring Boot static resources
- Current URL pattern: `/images/scooter-types/{slug}.png`

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
| GET | `/scooters/map-points` | Public | Query tracked scooter map locations with scooter type info |
| GET | `/scooter-types` | Public | List active scooter types |
| GET | `/scooter-types/{typeCode}` | Public | Get scooter type detail |
| GET | `/admin/scooter-types` | Bearer token (`MANAGER`) | Manager view of all scooter types |
| POST | `/admin/scooter-types` | Bearer token (`MANAGER`) | Create scooter type |
| PATCH | `/admin/scooter-types/{typeCode}` | Bearer token (`MANAGER`) | Update scooter type metadata |
| DELETE | `/admin/scooter-types/{typeCode}` | Bearer token (`MANAGER`) | Disable scooter type |
| GET | `/admin/hire-options` | Bearer token (`MANAGER`) | Manager view of all hire options |
| POST | `/admin/hire-options` | Bearer token (`MANAGER`) | Create hire option |
| PATCH | `/admin/hire-options/{hireOptionId}` | Bearer token (`MANAGER`) | Update hire option duration/price |
| DELETE | `/admin/hire-options/{hireOptionId}` | Bearer token (`MANAGER`) | Disable hire option |
| GET | `/admin/scooters` | Bearer token (`MANAGER`) | Manager inventory view |
| POST | `/admin/scooters` | Bearer token (`MANAGER`) | Add scooter |
| PATCH | `/admin/scooters/{scooterId}` | Bearer token (`MANAGER`) | Update scooter details |
| PATCH | `/admin/scooters/{scooterId}/status` | Bearer token (`MANAGER`) | Override scooter status |
| POST | `/admin/scooters/bulk-status` | Bearer token (`MANAGER`) | Bulk update scooter statuses |
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
    "createdAt": "2026-04-09T13:00:00"
  }
}
```

## 5. Hire Option APIs

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

### 5.3 GET `/api/v1/admin/hire-options`

Header:
- `Authorization: Bearer <manager_access_token>`

Behavior:
- Returns all hire options, including inactive ones

Response `data` item:

```json
{
  "hireOptionId": "HIRE-H1",
  "code": "H1",
  "durationMinutes": 60,
  "basePrice": 3.0,
  "active": true,
  "createdAt": "2026-04-09T13:00:00",
  "updatedAt": "2026-04-09T13:00:00"
}
```

### 5.4 POST `/api/v1/admin/hire-options`

Header:
- `Authorization: Bearer <manager_access_token>`

Request:

```json
{
  "code": "H2",
  "durationMinutes": 120,
  "basePrice": 6.0
}
```

Behavior:
- `code` is normalized to uppercase
- `hireOptionId` is generated as `HIRE-{CODE}`
- duplicate `code` or generated `hireOptionId` is rejected

### 5.5 PATCH `/api/v1/admin/hire-options/{hireOptionId}`

Header:
- `Authorization: Bearer <manager_access_token>`

Request:

```json
{
  "durationMinutes": 180,
  "basePrice": 8.5
}
```

Behavior:
- At least one field required
- Only `durationMinutes` and `basePrice` are mutable

### 5.6 DELETE `/api/v1/admin/hire-options/{hireOptionId}`

Header:
- `Authorization: Bearer <manager_access_token>`

Behavior:
- Soft disable only
- Returns the disabled hire option payload with `active=false`

## 6. Booking APIs (Create / Query / Modify / Cancel)

### 6.1 POST `/api/v1/bookings`

Header:
- `Authorization: Bearer <access_token>`

Request:

```json
{
  "scooterId": "SCO-0001",
  "hireOptionId": "HIRE-H1",
  "plannedStartAt": "2026-04-09T13:30:00"
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
  "startAt": "2026-04-09T13:30:00",
  "endAt": "2026-04-09T14:30:00",
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

### 6.3 GET `/api/v1/bookings/{bookingId}`

Header:
- `Authorization: Bearer <access_token>`

Behavior:
- Only booking owner can query detail

### 6.4 PATCH `/api/v1/bookings/{bookingId}`

Header:
- `Authorization: Bearer <access_token>`

Request (at least one field required):

```json
{
  "scooterId": "SCO-0002",
  "hireOptionId": "H4",
  "plannedStartAt": "2026-04-09T15:00:00"
}
```

### 6.5 POST `/api/v1/bookings/{bookingId}/cancel`

Header:
- `Authorization: Bearer <access_token>`

Request body is optional. If provided:

```json
{
  "reason": "change of plan"
}
```

## 7. Scooter Type APIs

### 7.1 GET `/api/v1/scooter-types`

Behavior:
- Public endpoint
- Returns active scooter types only

Response `data`:

```json
[
  {
    "typeCode": "ANDROMEDA",
    "displayName": "ANDROMEDA",
    "imageUrl": "/images/scooter-types/andromeda.png",
    "description": "High-performance urban scooter.",
    "active": true
  }
]
```

### 7.2 GET `/api/v1/scooter-types/{typeCode}`

Behavior:
- Public endpoint
- `typeCode` is case-insensitive and normalized to uppercase

Response `data`:

```json
{
  "typeCode": "GALAXY_SEAT",
  "displayName": "GALAXY Seat",
  "imageUrl": "/images/scooter-types/galaxy-seat.png",
  "description": "Comfort-focused seated scooter.",
  "active": true
}
```

### 7.3 GET `/api/v1/admin/scooter-types`

Header:
- `Authorization: Bearer <manager_access_token>`

Behavior:
- Returns all scooter types, including inactive ones

Response `data`:

```json
[
  {
    "typeCode": "ANDROMEDA",
    "displayName": "ANDROMEDA",
    "imageUrl": "/images/scooter-types/andromeda.png",
    "description": "High-performance urban scooter.",
    "active": true,
    "createdAt": "2026-04-10T13:00:00",
    "updatedAt": "2026-04-10T13:00:00"
  }
]
```

### 7.4 POST `/api/v1/admin/scooter-types`

Header:
- `Authorization: Bearer <manager_access_token>`

Request:

```json
{
  "typeCode": "SOLAR_X",
  "displayName": "SOLAR X",
  "imageUrl": "/images/scooter-types/solar-x.png",
  "description": "Compact city scooter."
}
```

Behavior:
- `typeCode` is normalized to uppercase
- duplicate `typeCode` is rejected
- image file management is external to this endpoint; the API stores metadata only

### 7.5 PATCH `/api/v1/admin/scooter-types/{typeCode}`

Header:
- `Authorization: Bearer <manager_access_token>`

Request:

```json
{
  "displayName": "GALAXY Seat Pro",
  "imageUrl": "/images/scooter-types/galaxy-seat-pro.png",
  "description": "Updated comfort-focused seated scooter."
}
```

Behavior:
- At least one field required
- Mutable fields: `displayName`, `imageUrl`, `description`

### 7.6 DELETE `/api/v1/admin/scooter-types/{typeCode}`

Header:
- `Authorization: Bearer <manager_access_token>`

Behavior:
- Soft disable only
- Existing scooters retain their `typeCode`
- Public `/scooter-types` no longer returns the disabled type

## 8. Scooter Public APIs

### 8.1 GET `/api/v1/scooters/ids`

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

### 8.2 GET `/api/v1/scooters/map-points`

Behavior:
- Public endpoint
- Returns scooters with non-null `lat` and `lng`
- Ordered by `scooterId`

Response `data`:

```json
[
  {
    "scooterId": "SCO-0001",
    "typeCode": "ANDROMEDA",
    "typeDisplayName": "ANDROMEDA",
    "typeImageUrl": "/images/scooter-types/andromeda.png",
    "status": "AVAILABLE",
    "batteryPercent": 92,
    "lat": 51.5074,
    "lng": -0.1278,
    "zone": "ZONE-A"
  }
]
```

## 9. Manager Scooter Management APIs

### 9.1 GET `/api/v1/admin/scooters`

Header:
- `Authorization: Bearer <manager_access_token>`

Query params:
- `status` optional, values:
  - `AVAILABLE`
  - `RESERVED`
  - `IN_USE`
  - `MAINTENANCE`
  - `UNAVAILABLE`

Response `data` item:

```json
{
  "scooterId": "SCO-0001",
  "typeCode": "ANDROMEDA",
  "typeDisplayName": "ANDROMEDA",
  "typeImageUrl": "/images/scooter-types/andromeda.png",
  "status": "AVAILABLE",
  "batteryPercent": 92,
  "lat": 51.5074,
  "lng": -0.1278,
  "zone": "ZONE-A",
  "version": 0,
  "createdAt": "2026-04-09T13:00:00",
  "updatedAt": "2026-04-09T13:00:00"
}
```

### 9.2 POST `/api/v1/admin/scooters`

Header:
- `Authorization: Bearer <manager_access_token>`

Request:

```json
{
  "scooterId": "SCO-0100",
  "typeCode": "ANDROMEDA",
  "status": "AVAILABLE",
  "batteryPercent": 100,
  "lat": 51.501,
  "lng": -0.141,
  "zone": "ZONE-D"
}
```

Behavior:
- `scooterId` is normalized to uppercase
- `typeCode` must match an active scooter type
- `status` defaults to `AVAILABLE` if omitted
- `batteryPercent` defaults to `100` if omitted
- `lat` and `lng` must be provided together

### 9.3 PATCH `/api/v1/admin/scooters/{scooterId}`

Header:
- `Authorization: Bearer <manager_access_token>`

Request:

```json
{
  "typeCode": "ORION_ULTRA",
  "batteryPercent": 84,
  "lat": 51.502,
  "lng": -0.142,
  "zone": "ZONE-E"
}
```

Behavior:
- At least one field required
- Mutable fields: `typeCode`, `batteryPercent`, `lat`, `lng`, `zone`
- `typeCode`, if present, must match an active scooter type
- `lat` and `lng` must remain a complete pair after update
- Successful update increments scooter `version`

### 9.4 PATCH `/api/v1/admin/scooters/{scooterId}/status`

Header:
- `Authorization: Bearer <manager_access_token>`

Request:

```json
{
  "status": "MAINTENANCE"
}
```

Behavior:
- Successful update increments scooter `version`

### 9.5 POST `/api/v1/admin/scooters/bulk-status`

Header:
- `Authorization: Bearer <manager_access_token>`

Request:

```json
{
  "scooterIds": ["SCO-0001", "SCO-0002"],
  "status": "MAINTENANCE"
}
```

Behavior:
- All scooter IDs must exist, otherwise request fails with `RESOURCE_NOT_FOUND`
- Successful update increments each scooter `version`

Response `data`:

```json
{
  "status": "MAINTENANCE",
  "updatedCount": 2,
  "scooterIds": ["SCO-0001", "SCO-0002"]
}
```

## 10. State Transitions (Current Implementation)

### 10.1 Booking

| From | Endpoint | To |
|---|---|---|
| `CONFIRMED` | `PATCH /bookings/{bookingId}` | `CONFIRMED` |
| `CONFIRMED` | `POST /bookings/{bookingId}/cancel` | `CANCELLED` |

### 10.2 Scooter

| From | Trigger | To |
|---|---|---|
| `AVAILABLE` | `POST /bookings` | `RESERVED` |
| `RESERVED` | `PATCH /bookings/{bookingId}` with scooter changed | `AVAILABLE` |
| `AVAILABLE` | `PATCH /bookings/{bookingId}` with scooter changed | `RESERVED` |
| `RESERVED` | `POST /bookings/{bookingId}/cancel` | `AVAILABLE` |
| any | `PATCH /admin/scooters/{scooterId}/status` | manager-selected status |
| any | `POST /admin/scooters/bulk-status` | manager-selected status |

## 11. Error Codes (Currently Used)

| Code | HTTP | Meaning |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Request payload validation failed |
| `AUTH_INVALID_CREDENTIALS` | 401 | Invalid login/token/user |
| `AUTH_TOKEN_EXPIRED` | 401 | Access token expired |
| `AUTH_FORBIDDEN` | 401/403 | Missing token or no permission |
| `RESOURCE_NOT_FOUND` | 404 | User/scooter/hire option/booking/scooter type not found |
| `SCOOTER_NOT_AVAILABLE` | 409 | Scooter is not available for booking/update |
| `BOOKING_CONFLICT` | 409 | Booking state conflict during modify/cancel |
| `INTERNAL_ERROR` | 500 | Unhandled server error |

## 12. Database (Current Implementation)

Current tables used by the backend:
- `users`
- `hire_options`
- `scooter_types`
- `scooters`
- `bookings`

Current scooter type design:
- `scooter_types.type_code` is the stable type identifier
- `scooters.type_code` references `scooter_types.type_code`
- images are stored as static files, while `scooter_types.image_url` stores the public path
- manager scooter type APIs update metadata in `scooter_types`

Current seeded scooter types:
- `ANDROMEDA`
- `GALAXY_SEAT`
- `LUNAR_LITE`
- `NEBULA_FAMILY`
- `ORION_ULTRA`
