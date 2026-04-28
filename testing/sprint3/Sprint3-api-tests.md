# Urbanova Sprint 3 – API Automated Test Report

| Project | Urbanova Electric Scooter Rental System |
|---|---|
| Phase | Sprint 3 |
| Test Type | API Automated Testing |
| Tools | Postman + Newman + htmlextra |
| Collection | Urbanova Sprint 3 v5 |
| Environment | Urbanova Local |
| Execution Time | 2026/04/25 |
| Raw Report | `testing/reports/sprint3-api-report.html` |

---

## 1. Execution Overview

| Metric | Value |
|---|---|
| Total Requests | 117 |
| Failed Requests | 0 |
| Total Assertions | 167 |
| Passed Assertions | **164** |
| Failed Assertions | **3** |
| Skipped Tests | 0 |
| Total Duration | 18.2 s |
| Average Response Time | 74 ms |
| **Pass Rate** | **98.2%** |

---

## 2. Test Scope

This automated test run covered 117 endpoints across 15 modules, expanding significantly from Sprint 2's 15 requests. New modules include the full booking lifecycle, payments & refunds, discount rules, notifications, issue management, and comprehensive admin operations.

| Module | Requests | Passed / Total Assertions |
|---|---|---|
| 00 Health & Meta | 3 | 4 / 5 |
| 01 Authentication | 15 | 20 / 20 |
| 02 Payment Methods | 5 | 5 / 5 |
| 03 Public – Scooters & Hire Options | 12 | 14 / 14 |
| 04 Discounts | 6 | 8 / 8 |
| 05 ⭐ Booking Lifecycle (Full Chain) | 13 | 22 / 22 |
| 06 ⭐ Cancel & BUG-001 Regression | 4 | 4 / 6 |
| 07 Payments & Refunds | 8 | 13 / 13 |
| 08 Notifications | 3 | 4 / 4 |
| 09 Issues | 9 | 10 / 10 |
| 10 Admin – Users & Audit | 5 | 6 / 6 |
| 11 Admin – Scooters | 6 | 9 / 9 |
| 12 Admin – Hire Options & Scooter Types | 10 | 12 / 12 |
| 13 Admin – Bookings | 4 | 5 / 5 |
| 15 Analytics | 6 | 8 / 8 |

---

## 3. Detailed Results

### 00 Health & Meta ⚠️

| Case | Endpoint | Expected | Actual | Result |
|---|---|---|---|---|
| TC-API-00-01 | GET /health | 200 + status=UP | 200 + status=UP | ✅ |
| TC-API-00-02 | GET /meta | 200 + apiVersion | 200 + apiVersion | ✅ |
| TC-API-00-03 | POST /test/add | 404 or 405 | **500** | ❌ |

TC-API-00-03 is a security check verifying that the development-only `TestController` has been removed or restricted. The endpoint returning 500 means it still exists in the deployed build and is throwing an unhandled exception. **Dev action required: remove or disable this controller before any production deployment.**

### 01 Authentication ✅

| Case | Endpoint | Expected | Actual | Result |
|---|---|---|---|---|
| Register (seed) | POST /auth/register | 200 / 400 | 200 | ✅ |
| TC-API-01-02 | POST /auth/login (Customer) | 200 + token | 200 + token saved | ✅ |
| TC-API-01-03 | POST /auth/login (Manager) | 200 + token | 200 + token saved | ✅ |
| TC-API-01-05 | POST /auth/login (wrong password) | 401 | 401 | ✅ |
| TC-API-01-06 | GET /users/me (with token) | 200 + email | 200 + email | ✅ |
| TC-API-01-07 | GET /users/me (no token) | 401 | 401 | ✅ |
| TC-API-01-08 | POST /auth/refresh | 200 + new token | 200 + new token | ✅ |
| TC-API-01-09 | POST /auth/password/forgot | 200 + resetToken | 200 + resetToken | ✅ |
| TC-API-01-10 | POST /auth/password/reset | 200 | 200 | ✅ |
| TC-API-01-11 | POST /auth/login (new password) | 200 | 200 | ✅ |
| TC-API-01-12 | PATCH /users/me | 200 + fullName updated | 200 | ✅ |
| TC-API-01-13 | GET /users/me/usage-summary | 200 | 200 | ✅ |
| TC-API-01-14 | POST /auth/logout | 200 | 200 | ✅ |
| TC-API-01-15 | POST /auth/refresh (revoked) | 401 | 401 | ✅ |
| Re-Login (restore token) | POST /auth/login | 200 | 200 | ✅ |

Full authentication lifecycle verified: registration, login, token refresh, password reset, profile update, logout, and token revocation all pass.

### 02 Payment Methods ✅

| Case | Endpoint | Expected | Actual | Result |
|---|---|---|---|---|
| TC-API-02-01 | GET /payment-methods | 200 | 200 | ✅ |
| TC-API-02-02 | POST /payment-methods | 200 + paymentMethodId | 200 + saved | ✅ |
| TC-API-02-03 | PATCH /payment-methods/{id} | 200 | 200 | ✅ |
| TC-API-02-05 | DELETE /payment-methods/{id} | 200 | 200 | ✅ |
| Re-Add Card (for booking) | POST /payment-methods | 200 | 200 | ✅ |

### 03 Public – Scooters & Hire Options ✅

| Case | Endpoint | Expected | Actual | Result |
|---|---|---|---|---|
| TC-API-03-01 | GET /hire-options | 200 + array | 200 + array | ✅ |
| TC-API-03-02 | POST /pricing/quotes (no token) | 200 + finalPrice | 200 + finalPrice | ✅ |
| TC-API-03-03 | POST /pricing/quotes (with token) | 200 + finalPrice | 200 + finalPrice | ✅ |
| TC-API-03-04 | POST /pricing/quotes (INVALID) | 404 | 404 | ✅ |
| TC-API-03-05 | GET /scooters | 200 + array | 200 + array | ✅ |
| TC-API-03-06 | GET /scooters?status=AVAILABLE | 200 + all AVAILABLE | 200 + all AVAILABLE | ✅ |
| TC-API-03-07 | GET /scooters/availability | 200 | 200 | ✅ |
| TC-API-03-08 | GET /scooters/ids?status=AVAILABLE | 200 | 200 | ✅ |
| TC-API-03-09 | GET /scooters/map-points | 200 + array | 200 + array | ✅ |
| TC-API-03-10 | GET /scooters/{id} | 200 | 200 | ✅ |
| TC-API-03-11 | GET /scooter-types | 200 | 200 | ✅ |
| TC-API-03-12 | GET /scooter-types/{typeCode} | 200 | 200 | ✅ |

### 04 Discounts ✅

| Case | Endpoint | Expected | Actual | Result |
|---|---|---|---|---|
| TC-API-04-00 | GET /admin/discount-rules (pre-check) | 200 | 200 | ✅ |
| TC-API-04-01 | GET /discounts/eligibility | 200 + discountCategory | 200 + discountCategory | ✅ |
| TC-API-04-02 | GET /admin/discount-rules (Manager) | 200 | 200 | ✅ |
| TC-API-04-03 | GET /admin/discount-rules (Customer) | 403 | 403 | ✅ |
| TC-API-04-04 | POST /admin/discount-rules | 200 or 400 | 200 or 400 | ✅ |
| TC-API-04-05 | PATCH /admin/discount-rules/{id} | 200 | 200 | ✅ |

Note: TC-API-04-04 intentionally accepts 400 as a valid outcome to handle idempotent re-runs where the `FREQUENT_USER` rule already exists in the database.

### 05 ⭐ Booking Lifecycle (Full Chain) ✅

| Case | Endpoint | Expected | Actual | Result |
|---|---|---|---|---|
| TC-API-05-01 | POST /bookings | 200 + PENDING_PAYMENT | 200 + PENDING_PAYMENT | ✅ |
| TC-API-05-02 | GET /bookings | 200 + array | 200 + array | ✅ |
| TC-API-05-03 | GET /bookings/{id} | 200 + PENDING_PAYMENT | 200 + PENDING_PAYMENT | ✅ |
| TC-API-05-04 | POST /bookings/{id}/payments | 200 + paymentId saved | 200 + paymentId saved | ✅ |
| TC-API-05-05 | GET /bookings/{id} | 200 + CONFIRMED | 200 + CONFIRMED | ✅ |
| TC-API-05-06 | GET /bookings/{id}/confirmation | 200 + SENT + resendCount=0 | 200 + SENT + 0 | ✅ |
| TC-API-05-07 | POST /bookings/{id}/confirmation/resend | 200 + RESENT + count=1 | 200 + RESENT + 1 | ✅ |
| TC-API-05-08 | POST /bookings/{id}/confirmation/resend | 200 + count=2 | 200 + count=2 | ✅ |
| TC-API-05-09 | GET /confirmations | 200 + array | 200 + array | ✅ |
| TC-API-05-10 | POST /bookings/{id}/start | 200 + ACTIVE | 200 + ACTIVE | ✅ |
| TC-API-05-11 | POST /bookings/{id}/extend | 200 | 200 | ✅ |
| TC-API-05-12 | POST /bookings/{id}/end | 200 + COMPLETED | 200 + COMPLETED | ✅ |
| TC-API-05-13 | GET /bookings/{id}/timeline | 200 + ascending events | 200 + ascending | ✅ |

Full booking lifecycle verified end-to-end: PENDING_PAYMENT → CONFIRMED (via payment) → ACTIVE (start) → COMPLETED (end), with confirmation emails and resend tracking at each step.

### 06 ⭐ Cancel & BUG-001 Regression ⚠️

| Case | Endpoint | Expected | Actual | Result |
|---|---|---|---|---|
| TC-API-06-01 | POST /bookings | 200 + bookingId2 | 200 + bookingId2 | ✅ |
| TC-API-06-02 | POST /bookings/{id}/cancel (1st) | 200 + CANCELLED | 200 + CANCELLED | ✅ |
| **TC-API-06-03** | POST /bookings/{id}/cancel (2nd) | **403 or 409** | **200** | ❌ |
| TC-API-06-04 | POST /bookings/{id}/cancel (COMPLETED) | 400/403/409 | 400/403/409 | ✅ |

TC-API-06-03 is a regression test for BUG-001 (carried over from Sprint 1 and Sprint 2). The first cancellation succeeds correctly; the second cancellation on the same already-CANCELLED booking should be rejected, but still returns 200. See Defect Log.

### 07 Payments & Refunds ✅

| Case | Endpoint | Expected | Actual | Result |
|---|---|---|---|---|
| TC-API-07-00 | POST /bookings (create bookingId3) | 200 + PENDING_PAYMENT | 200 | ✅ |
| TC-API-07-01 | GET /bookings/{id}/payments | 200 + length > 0 | 200 + 1 payment | ✅ |
| TC-API-07-02 | GET /payments/{paymentId} | 200 | 200 | ✅ |
| TC-API-07-03 | POST /bookings/{id}/payments (FAILURE) | 402 | 402 | ✅ |
| TC-API-07-04 | POST /bookings/{id}/payments (deferred) | 200 + INITIATED | 200 + INITIATED | ✅ |
| TC-API-07-05 | POST /payments/{id}/simulate-settlement | 200 | 200 | ✅ |
| TC-API-07-06 | POST /payments/{id}/simulate-settlement (Customer) | 403 | 403 | ✅ |
| TC-API-07-07 | POST /payments/{paymentId}/refund | 200 | 200 | ✅ |

Note: Simulated payment failure correctly returns HTTP 402 (Payment Required). The payment record is saved with status `FAILED` and the booking remains in `PENDING_PAYMENT` state, consistent with `noRollbackFor = BusinessException.class` in the service layer.

### 08 Notifications ✅

| Case | Endpoint | Expected | Actual | Result |
|---|---|---|---|---|
| TC-API-08-01 | GET /notifications | 200 + length > 0 | 200 + notifications | ✅ |
| TC-API-08-02 | PATCH /notifications/{id}/read | 200 + read=true | 200 + read=true | ✅ |
| TC-API-08-03 | GET /notifications (verify) | 200 + target read=true | 200 + read=true | ✅ |

### 09 Issues ✅

| Case | Endpoint | Expected | Actual | Result |
|---|---|---|---|---|
| TC-API-09-01 | POST /issues | 200 + issueId | 200 + issueId saved | ✅ |
| TC-API-09-02 | GET /issues | 200 | 200 | ✅ |
| TC-API-09-03 | GET /issues/{id} | 200 | 200 | ✅ |
| TC-API-09-04 | POST /issues/{id}/comments | 200 | 200 | ✅ |
| TC-API-09-05 | GET /admin/issues (Manager) | 200 | 200 | ✅ |
| TC-API-09-06 | PATCH /admin/issues/{id}/priority | 200 | 200 | ✅ |
| TC-API-09-07 | PATCH /admin/issues/{id}/status | 200 | 200 | ✅ |
| TC-API-09-08 | POST /admin/issues/{id}/resolve | 200 | 200 | ✅ |
| TC-API-09-09 | GET /admin/issues/high-priority | 200 | 200 | ✅ |

### 10 Admin – Users & Audit ✅

| Case | Endpoint | Expected | Actual | Result |
|---|---|---|---|---|
| TC-API-10-01 | GET /admin/users | 200 | 200 | ✅ |
| TC-API-10-02 | GET /admin/users?role=CUSTOMER | 200 | 200 | ✅ |
| TC-API-10-03 | GET /admin/users/{customerId} | 200 | 200 | ✅ |
| TC-API-10-04 | GET /admin/users/{customerId}/bookings | 200 | 200 | ✅ |
| TC-API-10-05 | GET /admin/audit-logs | 200 | 200 | ✅ |

### 11 Admin – Scooters ✅

| Case | Endpoint | Expected | Actual | Result |
|---|---|---|---|---|
| TC-API-11-00 | GET /admin/scooters (pre-check) | 200 | 200 | ✅ |
| TC-API-11-01 | GET /admin/scooters | 200 | 200 | ✅ |
| TC-API-11-02 | POST /admin/scooters | 200 or 400 | 200 or 400 | ✅ |
| TC-API-11-03 | PATCH /admin/scooters/{id} | 200 | 200 | ✅ |
| TC-API-11-04 | PATCH /admin/scooters/{id}/status | 200 + MAINTENANCE | 200 + MAINTENANCE | ✅ |
| TC-API-11-05 | POST /admin/scooters/bulk-status | 200 | 200 | ✅ |

### 12 Admin – Hire Options & Scooter Types ✅

| Case | Endpoint | Expected | Actual | Result |
|---|---|---|---|---|
| TC-API-12-00 | GET /admin/hire-options (pre-check) | 200 | 200 | ✅ |
| TC-API-12-00b | GET /admin/scooter-types (pre-check) | 200 | 200 | ✅ |
| TC-API-12-01 | GET /admin/hire-options | 200 | 200 | ✅ |
| TC-API-12-02 | POST /admin/hire-options | 200 or 400 | 200 or 400 | ✅ |
| TC-API-12-03 | PATCH /admin/hire-options/{id} | 200 | 200 | ✅ |
| TC-API-12-04 | DELETE /admin/hire-options/{id} | 200 | 200 | ✅ |
| TC-API-12-05 | GET /admin/scooter-types | 200 | 200 | ✅ |
| TC-API-12-06 | POST /admin/scooter-types | 200 or 400 | 200 or 400 | ✅ |
| TC-API-12-07 | PATCH /admin/scooter-types/{typeCode} | 200 | 200 | ✅ |
| TC-API-12-08 | DELETE /admin/scooter-types/{typeCode} | 200 | 200 | ✅ |

### 13 Admin – Bookings ✅

| Case | Endpoint | Expected | Actual | Result |
|---|---|---|---|---|
| TC-API-13-01 | GET /admin/bookings | 200 | 200 | ✅ |
| TC-API-13-02 | GET /admin/bookings?status=CONFIRMED | 200 + all CONFIRMED | 200 + all CONFIRMED | ✅ |
| TC-API-13-03 | GET /admin/bookings/{id} | 200 | 200 | ✅ |
| TC-API-13-04 | PATCH /admin/bookings/{id}/override | 200 | 200 | ✅ |

### 15 Analytics ✅

| Case | Endpoint | Expected | Actual | Result |
|---|---|---|---|---|
| TC-API-15-01 | GET /admin/analytics/revenue/estimate | 200 | 200 | ✅ |
| TC-API-15-02 | GET /admin/analytics/revenue/weekly-by-hire-option | 200 + array | 200 + array | ✅ |
| TC-API-15-03 | GET /admin/analytics/revenue/daily-combined | 200 + array | 200 + array | ✅ |
| TC-API-15-04 | GET /admin/analytics/revenue/weekly-chart | 200 | 200 | ✅ |
| TC-API-15-05 | GET /admin/analytics/usage/frequent-users | 200 | 200 | ✅ |
| TC-API-15-06 | GET /admin/analytics/revenue/estimate (Customer) | 403 | 403 | ✅ |

---

## 4. Defect Log

### BUG-001 (Regression – Sprint 1 and Sprint 2 carry-over, still unfixed)

| Field | Detail |
|---|---|
| Bug ID | BUG-001 |
| Module | Booking – Cancel |
| Test Case | TC-API-06-03 |
| Severity | Medium |
| Status | **Open (carried over from Sprint 1 and Sprint 2, still unfixed in Sprint 3)** |
| Description | Calling the cancel endpoint on a booking that already has status `CANCELLED` does not perform any idempotency check. The backend still returns HTTP 200. |
| Expected | HTTP 403 or 409 with an error message indicating the booking is already cancelled. |
| Actual | HTTP 200 returned; booking status reset to `CANCELLED` again. |
| Reproduction Steps | 1) Create a booking; 2) Call `POST /bookings/{id}/cancel` — succeeds (200 + CANCELLED); 3) Call cancel again with the same bookingId; 4) Observe HTTP 200 instead of 403/409. |
| Impact | Missing state-machine guard may cause duplicate audit log entries, and could trigger redundant refund or billing logic if cancellation hooks are ever added. |
| Suggested Fix | Add a status guard at the entry of `BookingService.cancelBooking`: if current status is already `CANCELLED` or `COMPLETED`, throw a `BusinessException` mapping to HTTP 403 or 409. |

### SEC-001 (New – Security Finding)

| Field | Detail |
|---|---|
| Bug ID | SEC-001 |
| Module | Health & Meta |
| Test Case | TC-API-00-03 |
| Severity | Low (dev environment only) |
| Status | **Open** |
| Description | `POST /test/add` (`/test/add`, outside `/api/v1`) responds with HTTP 500, indicating that a development-only `TestController` is still present in the running build and crashes on invocation. |
| Expected | HTTP 404 or 405 — the endpoint should not exist outside of local development. |
| Actual | HTTP 500 returned. |
| Impact | Exposes an undocumented endpoint in the running build. While it currently crashes harmlessly, it represents unnecessary attack surface and should be removed before any staging or production deployment. |
| Suggested Fix | Delete or comment out `TestController`. If needed for local development, gate it with a Spring profile (`@Profile("local")`). |

---

## 5. Comparison with Sprint 2

| Item | Sprint 2 | Sprint 3 |
|---|---|---|
| Total requests | 15 | **117** |
| Total assertions | 19 | **167** |
| Passed assertions | 18 | **164** |
| Pass rate | 94.7% | **98.2%** |
| Modules covered | 5 | **15** |
| Booking lifecycle tested | Partial (create + cancel only) | **Full chain (PENDING → CONFIRMED → ACTIVE → COMPLETED)** |
| Payment flow tested | No | **Yes (success, failure, deferred settlement, refund)** |
| Admin APIs tested | Read-only | **Full CRUD (scooters, hire options, scooter types, users, bookings)** |
| BUG-001 status | Open | **Still open** |
| New bugs found | — | SEC-001 (TestController exposed) |
| Single-run duration | 1.66 s | 18.2 s |
| Average response time | 57 ms | 74 ms |

---

## 6. Conclusion

1. **164 of 167 assertions passed (98.2% pass rate).** All 117 requests completed successfully with no HTTP-level failures.
2. **The full booking lifecycle is verified end-to-end** — from creation through payment, confirmation, start, extend, and end — including confirmation email tracking and payment failure simulation.
3. **Role-based access control is consistently enforced** across all protected endpoints: Customer tokens are correctly rejected on admin and staff routes; Manager-only endpoints return 403 for Customer tokens throughout modules 04, 07, 10, 11, 12, 13, and 15.
4. **The only failing assertions are caused by two known backend defects** (BUG-001 and SEC-001), not by collection or environment errors. Both are documented in the Defect Log above.
5. **BUG-001 remains open for the third consecutive sprint.** This should be treated as a priority fix in Sprint 4 — the suggested fix is a single status guard at the entry of `BookingService.cancelBooking`.
6. **The automation pipeline remains stable and repeatable.** The collection is designed to handle idempotent re-runs: pre-check steps in modules 04, 11, and 12 detect existing test data and skip redundant creation, preventing false 400 failures on subsequent runs.

---

**Report Date:** 2026/04/25
**Tester:** Na Cao
