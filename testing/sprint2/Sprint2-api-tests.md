# Urbanova Sprint 2 – API Automated Test Report

| Project | Urbanova Electric Scooter Rental System |
|---|---|
| Phase | Sprint 2 |
| Test Type | API Automated Testing |
| Tools | Postman + Newman + htmlextra |
| Collection | Urbanova Sprint 2 |
| Environment | Urbanova Local |
| Execution Time | 2026/04/11 18:04:55 |
| Raw Report | `testing/reports/sprint2-api-report.html` |

## 1. Execution Overview

| Metric | Value |
|---|---|
| Total Requests | 15 |
| Failed Requests | 0 |
| Total Assertions | 19 |
| Passed Assertions | **18** |
| Failed Assertions | **1** |
| Skipped Tests | 0 |
| Total Duration | 1.66 s |
| Average Response Time | 57 ms |
| **Pass Rate** | **94.7%** |

## 2. Test Scope

This automated test run covered 15 core endpoints across five modules:

| Module | Requests | Passed / Total Assertions |
|---|---|---|
| 00 Health Check | 1 | 2 / 2 |
| 01 Authentication (register / login / current user) | 5 | 6 / 6 |
| 02 Public Hire & Scooter Queries | 3 | 4 / 4 |
| 03 Admin APIs (Manager-only) | 3 | 3 / 3 |
| 04 Booking Flow (create / cancel / regression) | 3 | 3 / 4 |

## 3. Detailed Results

### 00 Health Check ✅

| Case | Endpoint | Expected | Actual | Result |
|---|---|---|---|---|
| TC-API-01 | GET /health | 200 + status=UP | 200 + status=UP | ✅ |

### 01 Authentication ✅

| Case | Endpoint | Expected | Actual | Result |
|---|---|---|---|---|
| Register | POST /auth/register | 200 / 400 | 200 | ✅ |
| Login as Customer | POST /auth/login | 200 + token | 200 + token saved | ✅ |
| Login as Manager | POST /auth/login | 200 + token | 200 + token saved | ✅ |
| TC-API-12 | GET /users/me (with token) | 200 | 200 | ✅ |
| TC-API-13 | GET /users/me (no token) | 401 | 401 | ✅ |

### 02 Public Hire & Scooter ✅

| Case | Endpoint | Expected | Actual | Result |
|---|---|---|---|---|
| TC-API-15 | GET /hire-options | 200 + array | 200 | ✅ |
| TC-API-17 | POST /pricing/quotes (H1) | 200 + finalPrice=3.00 | 200 + 3.00 | ✅ |
| TC-API-19 | POST /pricing/quotes (INVALID) | 404 | 404 | ✅ |

### 03 Admin APIs ✅

| Case | Endpoint | Expected | Actual | Result |
|---|---|---|---|---|
| TC-API-31 | GET /admin/scooter-types (Manager) | 200 | 200 | ✅ |
| TC-API-32 | GET /admin/scooter-types (Customer) | 403 | 403 | ✅ |
| TC-API-49 | GET /admin/scooters (Manager) | 200 | 200 | ✅ |

Role-based access control is working correctly: Customer tokens are properly rejected when accessing admin endpoints.

### 04 Booking Flow ⚠️

| Case | Endpoint | Expected | Actual | Result |
|---|---|---|---|---|
| TC-API-21 | POST /bookings | 200 + bookingId | 200 | ✅ |
| TC-API-26 | POST /bookings/{id}/cancel | 200 | 200 | ✅ |
| **TC-API-30** | POST /bookings/{id}/cancel (repeat cancel) | **403** | **200** | ❌ |

## 4. Defect Log

### BUG-001 (Regression – Sprint 1 carry-over, still unfixed)

| Field | Detail |
|---|---|
| Bug ID | BUG-001 |
| Module | Booking – Cancel |
| Test Case | TC-API-30 |
| Severity | Medium |
| Status | **Open (carried over from Sprint 1, still unfixed in Sprint 2)** |
| Description | Calling the cancel endpoint on a booking that already has status `CANCELLED` does not perform any idempotency check. The backend still returns HTTP 200 and re-sets the status to `CANCELLED`. |
| Expected | HTTP 403 (or 409 Conflict) with an error message indicating the booking has already been cancelled and the operation is not allowed. |
| Actual | HTTP 200 returned, assertion failed. |
| Reproduction Steps | 1) Create a booking; 2) Call the cancel endpoint (succeeds); 3) Call the cancel endpoint again with the same bookingId; 4) Observe HTTP 200 instead of 403. |
| Impact | Missing state-machine validation may cause inconsistent UI display, redundant audit logs, and incorrect downstream business logic such as refunds or billing being triggered more than once. |
| Suggested Fix | Add a status guard at the entry of `BookingService.cancelBooking`: if the current status is already `CANCELLED` or `COMPLETED`, throw a business exception that maps to HTTP 403/409. |

## 5. Comparison with Sprint 1

| Item | Sprint 1 | Sprint 2 |
|---|---|---|
| Test approach | Manual click-through in Postman | Newman one-line CLI execution |
| Report generation | Hand-written markdown tables | htmlextra auto-generated HTML |
| Test cases | 30 (manual) | 15 automated + manual cases |
| Single-run duration | ~30 minutes | **1.66 seconds** |
| Repeatability | Re-click everything | Re-run with one command |
| BUG-001 status | Open | **Still unfixed** |

After introducing Newman, regression efficiency improved dramatically. Critical endpoints can now be verified immediately after any backend code change.

## 6. Conclusion

1. **18 of 19 assertions passed (94.7% pass rate).** All HTTP requests were dispatched successfully and the backend service is stable.
2. **Authentication, role-based access control, public queries, admin endpoints, and booking creation/cancellation** are all working as expected.
3. The **only failed case (TC-API-30)** is a regression test for the Sprint 1 defect BUG-001. This run **confirms the bug is still present in Sprint 2** and should be prioritised for the next iteration.
4. The automation pipeline (Postman → Export → Newman → htmlextra HTML report) is fully working and will serve as the standard regression workflow for upcoming sprints.


**Report Date:** 2026/04/11
**Tester:** Na Cao
