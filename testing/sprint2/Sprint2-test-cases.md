# Urbanova – Electric Scooter Rental System
## Sprint 2 Functional Test Case Document

| Project | Urbanova – Electric Scooter Rental System |
|---------|------------------------------------------|
| Version | V2.0 |
| Scope | Web Frontend (Admin pages) + Mobile Frontend (All pages) |
| Test Type | Functional Testing (Manual) |
| Date | 2026/04/11 |

---

## 1. Sprint 1 Regression Tests

| ID | Scenario | Precondition | Steps | Expected Result | Actual Result |
|----|----------|-------------|-------|-----------------|---------------|
| REG-01 | BUG-001: Wrong password/username causes page refresh with no error | Registered account exists | Enter incorrect password, click Login | Shows "Invalid email or password", stays on login page | Pass |
| REG-02 | BUG-002: Logged-in user can access Register page | Already logged in | Enter Register page URL in address bar | Auto-redirects to home page, shows "You are already logged in" | Pass |
| REG-03 | BUG-003: No booking record + page navigation broken after booking | Logged in with booking | Complete a booking, go to My Bookings | Booking record is displayed, page navigation works normally | No booking record + page navigation broken — bug not fixed |

---

## 2. Web Frontend – Admin Management Pages

**Objective:** Verify that admin users can manage scooters, scooter types, and hire options. Non-admin users must not have access.

| ID | Scenario | Precondition | Steps | Expected Result | Actual Result |
|----|----------|-------------|-------|-----------------|---------------|
| TC-WA01 | Admin menu visible after admin login | Admin account | Log in as admin | Navigation bar shows Admin entry | Pass |
| TC-WA02 | Admin menu hidden for regular users | Customer account | Log in as customer | Navigation bar has no Admin entry | Pass |
| TC-WA03 | Customer accesses /admin via URL | Customer account | Enter admin page URL in address bar | Redirects or shows 403 | Customer can access /admin directly via URL |
| TC-WA04 | Scooter list displays correctly | Admin logged in | Navigate to scooter management page | All scooters and their statuses are displayed | Pass |
| TC-WA05 | Add new scooter | Admin logged in | Fill in the form, submit | List refreshes, new scooter appears | Pass |
| TC-WA06 | Edit scooter information | Admin logged in | Modify fields and save | Information updated successfully | Cannot update status via "Edit" — must use separate "Change Status" action |
| TC-WA07 | Delete scooter | Admin logged in | Click delete and confirm | Scooter removed from list | No delete operation available |
| TC-WA08 | Manage hire options (add/edit/delete) | Admin logged in | Perform CRUD on Hire Options | Operations succeed, frontend syncs | Can add, but edit only supports disabling (not re-enabling), no delete option |

---

## 3. Mobile – Login / Register (Auth)

**Objective:** Verify mobile login and registration flows are complete with proper error handling.

| ID | Scenario | Precondition | Steps | Expected Result | Actual Result |
|----|----------|-------------|-------|-----------------|---------------|
| TC-MA01 | Successful login | Registered account | Enter correct credentials, tap Login | Redirects to home page | Pass |
| TC-MA02 | Wrong password | Registered account | Enter incorrect password | Shows error message, stays on login page | Pass |
| TC-MA03 | Submit with empty fields | None | Leave fields empty, tap Submit | Shows required field prompts | Pass |
| TC-MA04 | Successful registration | No account | Fill in valid info, tap Register | Registration succeeds, redirects to login or auto-login | Pass |
| TC-MA05 | Duplicate email registration | Email already registered | Use an already registered email | Shows "email already exists" prompt | Pass |
| TC-MA06 | Password format validation | None | Enter a password that doesn't meet requirements | Shows password requirement prompt | Pass |

---

## 4. Mobile – Ride

**Objective:** Verify users can view available scooters, complete bookings, and start/end rides.

| ID | Scenario | Precondition | Steps | Expected Result | Actual Result |
|----|----------|-------------|-------|-----------------|---------------|
| TC-MR01 | View available scooters | Logged in | Open Ride page | Shows available scooter list/map | Map stuck on loading |
| TC-MR02 | Select scooter and book | Logged in | Select scooter, choose plan, confirm | Booking succeeds, confirmation displayed | Cannot book at this time |
| TC-MR03 | Start ride | Booking exists | Tap Start Ride | Ride timer begins | Not tested — blocked by TC-MR02 |
| TC-MR04 | End ride | Ride in progress | Tap End Ride | Shows fare summary, ride ends | Not tested — blocked by TC-MR03 |
| TC-MR05 | Access Ride without login | Not logged in | Attempt to open Ride page | Redirects to login page | Not tested |

---

## 5. Mobile – Trip History (Trips)

**Objective:** Verify users can view past trip records and details.

| ID | Scenario | Precondition | Steps | Expected Result | Actual Result |
|----|----------|-------------|-------|-----------------|---------------|
| TC-MT01 | View trip list | Logged in, has past trips | Open Trips page | All past trips displayed | Pass |
| TC-MT02 | View trip details | Logged in | Tap on a trip | Shows details (time, fare, route, etc.) | Route cannot be displayed at this time |
| TC-MT03 | No trip records | Logged in, no trips | Open Trips page | Shows empty state prompt | Pass |

---

## 6. Mobile – Wallet

**Objective:** Verify wallet balance display, top-up, and transaction history.

| ID | Scenario | Precondition | Steps | Expected Result | Actual Result |
|----|----------|-------------|-------|-----------------|---------------|
| TC-MW01 | View balance | Logged in | Open Wallet page | Current balance displayed | Pass |
| TC-MW02 | Top up | Logged in | Select/enter amount, confirm | Balance updated | Pass |
| TC-MW03 | Invalid top-up amount | Logged in | Enter negative number or zero | Shows error prompt | Pass |
| TC-MW04 | View transaction history | Logged in | Check spending/top-up records | Shows transaction list | No transaction history feature available |

---

## 7. Mobile – Profile

**Objective:** Verify users can view and edit personal information and log out.

| ID | Scenario | Precondition | Steps | Expected Result | Actual Result |
|----|----------|-------------|-------|-----------------|---------------|
| TC-MP01 | View personal info | Logged in | Open Profile page | Shows username, email, etc. | Pass |
| TC-MP02 | Edit personal info | Logged in | Modify name/phone, save | Updated successfully | No edit feature available |
| TC-MP03 | Log out | Logged in | Tap Log Out | Returns to login page, session cleared | Pass |

---

## 8. Mobile – General Tests

**Objective:** Verify common mobile interactions and cross-platform consistency.

| ID | Scenario | Precondition | Steps | Expected Result | Actual Result |
|----|----------|-------------|-------|-----------------|---------------|
| TC-MG01 | Bottom tab navigation | Logged in | Tap each bottom tab | Pages switch correctly, no blank screens | Pass |
| TC-MG02 | Network disconnection prompt | Logged in | Turn off network, attempt an action | Shows network error prompt | No offline error handling at this time |
| TC-MG03 | App background and resume | In use | Press Home, then return | State preserved, no crash | Pass |
| TC-MG04 | iOS and Android consistency | Both devices | Run core flows on both platforms | Behaviour is consistent | iOS not available — requires additional development |

---

## 9. Cross-Platform Consistency Tests

**Objective:** Verify data synchronisation between Web and Mobile for the same account.

| ID | Scenario | Precondition | Steps | Expected Result | Actual Result |
|----|----------|-------------|-------|-----------------|---------------|
| TC-CP01 | Web booking → visible on Mobile | Same account | Complete booking on Web, open Mobile | Mobile shows the booking | Pass |
| TC-CP02 | Mobile booking → visible on Web | Same account | Complete booking on Mobile, open Web | Web shows the booking | Web still cannot display the booking |
| TC-CP03 | Account info consistent across platforms | Same account | Check personal info on both platforms | Name, email, balance all match | Pass |
| TC-CP04 | Web cancel booking → Mobile status syncs | Same account | Cancel booking on Web, refresh Mobile | Mobile shows CANCELLED status | No cancel feature available on Web |
| TC-CP05 | Mobile top-up → Web balance updates | Same account | Top up on Mobile, refresh Web wallet | Balances match | Pass |

---

## 10. Test Summary

### 10.1 Defect Log

| Bug ID | Module | Description | Severity | Status |
|--------|--------|-------------|----------|--------|
| BUG-S2-001 | Sprint 1 Regression | BUG-003 not fixed: no booking record shown + page navigation broken after booking | Critical | Open |
| BUG-S2-002 | Web Admin | Customer can access /admin page directly via URL — no permission check | Critical | Open |
| BUG-S2-003 | Web Admin | Cannot update scooter status via Edit — must use separate "Change Status" action | Moderate | Open |
| BUG-S2-004 | Web Admin | No delete scooter functionality | Moderate | Open |
| BUG-S2-005 | Web Admin | Hire option can be disabled but not re-enabled; no delete option | Moderate | Open |
| BUG-S2-006 | Mobile Ride | Map stuck on loading — cannot view available scooters | Critical | Open |
| BUG-S2-007 | Mobile Ride | Booking flow not functional | Critical | Open |
| BUG-S2-008 | Mobile Trips | Trip detail does not display route | Minor | Open |
| BUG-S2-009 | Mobile Wallet | No transaction history feature | Moderate | Open |
| BUG-S2-010 | Mobile Profile | No edit personal info feature | Moderate | Open |
| BUG-S2-011 | Mobile General | No offline/network error handling | Minor | Open |
| BUG-S2-012 | Mobile General | iOS build not available — requires additional development | Critical | Open |
| BUG-S2-013 | Cross-Platform | Mobile booking not visible on Web | Critical | Open |
| BUG-S2-014 | Cross-Platform | No cancel booking feature on Web | Moderate | Open |

### 10.2 Overall Assessment

41 test cases in total. 38 executed, 3 blocked (TC-MR03, TC-MR04, TC-MR05 — dependent on Ride booking which is non-functional).

| Metric | Count |
|--------|-------|
| Total Cases | 41 |
| Passed | 24 |
| Failed | 14 |
| Blocked / Not Tested | 3 |
| **Pass Rate (executed)** | **63.2%** |

14 defects identified, including 6 critical issues. The primary blockers are:

1. **Mobile Ride module non-functional** — map loading failure blocks the entire ride flow (TC-MR01–MR05), making core mobile functionality unusable.
2. **Admin access control missing** — customers can access /admin via direct URL, posing a security risk.
3. **Cross-platform sync incomplete** — mobile bookings do not appear on the web, and web lacks a cancel booking feature.
4. **Sprint 1 BUG-003 still unresolved** — booking records and page navigation remain broken.

---

Document Date: 2026/04/11
