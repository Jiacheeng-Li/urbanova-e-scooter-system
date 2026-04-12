# Urbanova – Electric Scooter Rental System
## API Test Document (Postman)

| Version | V1.0 |
|---------|------|
| Base URL | http://localhost:8080/api/v1 |
| Tool | Postman |
| Endpoints Tested | 7 |
| Date | 2026/03/18 |

---

## API Test Details

### 1. Health Check

```
GET http://localhost:8080/api/v1/health
```

Run this first to confirm the backend is running. A response of `status: UP` means all other tests can proceed.

| ID | Request | Expected | Actual | Status |
|----|---------|----------|--------|--------|
| TC-API-01 | Send with no parameters | HTTP 200, `status: UP` | `{"success": true, "data": {"status": "UP"}, "error": null}` | ✅ Pass |

---

### 2. User Registration

```
POST http://localhost:8080/api/v1/auth/register
```

Registers a new user. Username and email must be unique in the database — duplicate registration returns an error.

**Request Body (raw → JSON):**
```json
{
  "email": "testuser02@example.com",
  "password": "Test@1234",
  "fullName": "Test User"
}
```

| ID | Request | Expected | Actual |
|----|---------|----------|--------|
| TC-API-02 | Valid email, password, and fullName | HTTP 200, returns token and user info | ✅ Token returned, registration successful |
| TC-API-03 | Already registered email | HTTP 400, email already exists | ✅ `VALIDATION_ERROR: Email already registered` |
| TC-API-04 | Invalid email format (e.g. abc123) | HTTP 400, invalid email format | ✅ `email format is invalid` |
| TC-API-05 | Missing `password` field | HTTP 400, missing required field | ✅ `password is required` |
| TC-API-06 | Empty JSON: `{}` | HTTP 400, all required fields missing | ✅ password, fullName, email all reported as required |

<details>
<summary>TC-API-02 Full Response</summary>

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresInSeconds": 900,
    "user": {
      "userId": "73aedf50-8046-47a8-bab8-4725cd21e362",
      "email": "testuser02@example.com",
      "fullName": "Test User",
      "phone": null,
      "role": "CUSTOMER",
      "discountCategory": "NONE",
      "accountStatus": "ACTIVE",
      "createdAt": "2026-03-08T21:15:10.6179306"
    }
  },
  "error": null
}
```
</details>

---

### 3. User Login

```
POST http://localhost:8080/api/v1/auth/login
```

Returns a token on success. Copy the token into the Postman environment variable so it is reused automatically in subsequent requests.

**Request Body (raw → JSON):**
```json
{
  "email": "testuser02@example.com",
  "password": "Test@1234"
}
```

| ID | Request | Expected | Actual |
|----|---------|----------|--------|
| TC-API-07 | Correct email and password | HTTP 200, returns token | ✅ Token returned, login successful |
| TC-API-08 | Wrong password | HTTP 400, invalid credentials | ✅ `AUTH_INVALID_CREDENTIALS: Invalid email or password` |
| TC-API-09 | Non-existent email | HTTP 400, user not found | ✅ `Invalid email or password` |
| TC-API-10 | Empty email field | HTTP 400, field required | ✅ `Invalid email or password` |
| TC-API-11 | Empty JSON: `{}` | HTTP 400, validation failed | ✅ password and email both reported as required |

---

### 4. Get Current User Info 🔒

```
GET http://localhost:8080/api/v1/users/me
```

Requires a valid token in the Authorization header. Returns 401 if the token is missing or invalid.

**Headers:**

| Key | Value |
|-----|-------|
| Authorization | Bearer {{token}} |

| ID | Request | Expected | Actual |
|----|---------|----------|--------|
| TC-API-12 | Valid Bearer token in headers | HTTP 200, returns user info | ✅ User details returned correctly |
| TC-API-13 | No Authorization header | HTTP 401, unauthorised | ✅ `AUTH_FORBIDDEN: Missing Bearer token` |
| TC-API-14 | Invalid token (e.g. Bearer abc) | HTTP 401, invalid token | ✅ `AUTH_INVALID_CREDENTIALS: Invalid access token` |

---

### 5. List Hire Options

```
GET http://localhost:8080/api/v1/hire-options
```

Returns all available rental plans. Corresponds to the 4 cards shown on the frontend Hire Options page. **No login required.**

| ID | Request | Expected | Actual |
|----|---------|----------|--------|
| TC-API-15 | GET with no parameters | HTTP 200, returns 4 plans with correct prices | ✅ H1, H4, D1, W1 returned with correct pricing |

<details>
<summary>TC-API-15 Full Response</summary>

```json
{
  "success": true,
  "data": [
    { "hireOptionId": "HIRE-H1", "code": "H1", "durationMinutes": 60,    "basePrice": 3.00,  "active": true },
    { "hireOptionId": "HIRE-H4", "code": "H4", "durationMinutes": 240,   "basePrice": 12.00, "active": true },
    { "hireOptionId": "HIRE-D1", "code": "D1", "durationMinutes": 1440,  "basePrice": 20.00, "active": true },
    { "hireOptionId": "HIRE-W1", "code": "W1", "durationMinutes": 10080, "basePrice": 60.00, "active": true }
  ],
  "error": null
}
```
</details>

---

### 6. Get Price Quote

```
POST http://localhost:8080/api/v1/pricing/quotes
```

Calculates the actual charge for a given plan code, including any applicable discounts.

**Request Body (raw → JSON):**
```json
{
  "hireOptionCode": "H1"
}
```

| ID | Request | Expected | Actual |
|----|---------|----------|--------|
| TC-API-17 | `{"hireOptionCode": "H1"}` | Returns price £3.00 | ✅ `finalPrice: 3.00, currency: GBP` |
| TC-API-18 | `{"hireOptionCode": "W1"}` | Returns price £60.00 | ✅ `finalPrice: 60.00, currency: GBP` |
| TC-API-19 | `{"hireOptionCode": "INVALID"}` | HTTP 404, plan not found | ✅ `RESOURCE_NOT_FOUND: Hire option not found` |
| TC-API-20 | Missing `hireOptionCode` field | HTTP 400, required field missing | ✅ `hireOptionCode is required` |

---

### 7. Scooter List (Supplementary Test)

```
GET http://localhost:8080/api/v1/scooters/ids?status=AVAILABLE
```

> ⚠️ This test was added to verify whether a scooter-related endpoint exists in the backend.

| Request | Expected | Actual |
|---------|----------|--------|
| Valid Bearer token in headers | HTTP 200, returns available scooters | ✅ |

---

### 8. Create Booking 🔒

```
POST http://localhost:8080/api/v1/bookings
```

Requires login (Bearer token in headers). Creates a booking record based on the selected plan and scooter.

**Request Body (raw → JSON):**
```json
{
  "scooterId": "SCO-0001",
  "hireOptionId": "H1"
}
```

| ID | Request | Expected | Actual |
|----|---------|----------|--------|
| TC-API-21 | Valid token, valid plan code and scooter ID | HTTP 200, returns booking details and bookingId | ✅ |
| TC-API-22 | No Authorization Header | HTTP 401, unauthorized | ✅ |
| TC-API-23 | Fill in the hireOptionCode with a non-existent value | HTTP 404, indicating that the package does not exist | ✅ |
| TC-API-24 | The scooterId field is missing | HTTP 400，bad request | ✅ |
| TC-API-25 | Fill in the "scooterId" for non-existent scooters | HTTP 404 indicates that the scooter does not exist | ✅ |

---

### 9. Cancel Booking

```
POST http://localhost:8080/api/v1/bookings/{bookingId}/cancel
```

Requires login (Bearer token in headers). Cancel a booking record.

| ID | Request | Expected | Actual |
|----|---------|----------|--------|
| TC-API-26 | Valid token, valid booking ID | HTTP 200, Change status to CANCELLED | ✅ |
| TC-API-27 | Valid token, invalid booking ID | HTTP 404, Reservation does not exist | ✅ |
| TC-API-28 | No Authorization Header | HTTP 401, unauthorized | ✅ |
| TC-API-29 | Try to cancel someone else's reservation (using different account tokens) | HTTP 403, forbidden | ✅ |
| TC-API-30 | Cancel the cancelled reservation again | HTTP 403, forbidden | ❌ HTTP 200, Change status to CANCELLED |

---

## Test Summary

| Endpoint | Cases | Passed | Failed / Blocked |
|----------|-------|--------|-----------------|
| Health Check | 1 | 1 | 0 |
| User Registration | 5 | 5 | 0 |
| User Login | 5 | 5 | 0 |
| Get Current User | 3 | 3 | 0 |
| List Hire Options | 1 | 1 | 0 |
| Price Quote | 4 | 4 | 0 |
| Create Booking | 5 | 5 | 0 |
| Cancel Booking | 5 | 4 | 1 |
| **Total** | **30** | **29** | **1** |

**Conclusion:** All implemented endpoints respond correctly. Authentication, input validation, and error response formats all behave as expected except the repeated cancel booking test.

---

Document Date: 2026/03/18
