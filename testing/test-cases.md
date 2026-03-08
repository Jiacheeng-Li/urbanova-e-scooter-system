# Urbanova – Electric Scooter Rental System
## Functional Test Case Document

| Project | Urbanova – Electric Scooter Rental System |
|---------|------------------------------------------|
| Version | V1.0 |
| Scope | Completed frontend pages: Login, Register, Hire Options, Booking |
| Test Type | Functional Testing (Manual) |
| Date | 2026/03/08 |

---

## 1. Login Tests

| ID | Scenario | Precondition | Steps | Expected Result | Actual Result |
|----|----------|-------------|-------|-----------------|---------------|
| TC-L01 | Successful login | Registered account exists | Enter correct username and password, click Login | Redirects to home page, user info displayed | Pass |
| TC-L02 | Wrong password | Registered account exists | Enter correct username with incorrect password, click Login | Page shows "Incorrect password", stays on login page | Page does not show error — refreshes instead |
| TC-L03 | Non-existent username | None | Enter a username that doesn't exist with any password, click Login | Page shows "User not found" or "Invalid credentials" | Page does not show error — refreshes instead |
| TC-L04 | Empty username | None | Leave username blank, enter a password, click Login | Field shows "Please enter your username", no request sent | Pass |
| TC-L05 | Empty password | None | Enter a username, leave password blank, click Login | Field shows "Please enter your password", no request sent | Pass |
| TC-L06 | Both fields empty | None | Leave both username and password blank, click Login | Both fields show required error, no request sent | Pass |
| TC-L07 | Navigate to register while logged in | Already logged in | Directly enter the register page URL in address bar | Auto-redirects to home page, re-registration not allowed | Register page is accessible via direct URL |

---

## 2. Registration Tests

| ID | Scenario | Precondition | Steps | Expected Result | Actual Result |
|----|----------|-------------|-------|-----------------|---------------|
| TC-R01 | Successful registration | No existing account | Fill in valid username, email, and password, click Register | Registration succeeds, redirects to login or logs in automatically | Pass |
| TC-R02 | Invalid email format | None | Enter a malformed email (e.g. abc123), click Register | Shows "Please enter a valid email address" | Pass |
| TC-R03 | Password too short | None | Enter a password shorter than the minimum length (8 chars), click Register | Shows password length requirement error | Pass |
| TC-R04 | Passwords do not match | None | Enter different values in password and confirm password fields, click Register | Shows "Passwords do not match" | Pass |
| TC-R05 | Username already taken | Same username exists in DB | Enter an existing username with other valid fields, click Register | Shows "Username is already taken" | Duplicate username is accepted |
| TC-R06 | Required fields empty | None | Leave any required field blank, click Register | Affected field shows required error, no request sent | Pass |
| TC-R07 | Login after registration | Registration completed | Log in with the newly registered credentials | Login succeeds, enters the system | Pass |

---

## 3. Hire Options Tests

| ID | Scenario | Precondition | Steps | Expected Result | Actual Result |
|----|----------|-------------|-------|-----------------|---------------|
| TC-H01 | Page loads correctly | Not logged in | Open the Hire Options page | 4 plans shown: 1 Hour, 4 Hours, 1 Day, 7 Days — with correct prices | Pass |
| TC-H02 | Prices are correct | None | Check the 4 plan cards on the page | 1h=£3.00, 4h=£12.00, 1d=£20.00, 7d=£60.00 | Pass |
| TC-H03 | Plan codes displayed correctly | None | Check the label on the top-left of each card | H1, H4, D1, W1 respectively | Pass |
| TC-H04 | "View Details" button (logged in) | Logged in | Click "View Details" on any plan | Detailed information for that plan is shown | Pass |
| TC-H05 | Responsive layout | None | Resize browser window to different widths | Cards adapt to the layout, no content overflow | Pass |
| TC-H06 | Navbar when not logged in | Not logged in | Check the top navigation bar | "Login" and "Register" buttons are visible | Pass |
| TC-H07 | Navbar after login | Logged in | Check the top navigation bar | Username is shown, Login/Register buttons are hidden | Pass |

---

## 4. Booking Tests

**Objective:** Verify that a user can complete the full booking flow, including selecting a scooter, confirming details, submitting, and managing the booking.

| ID | Scenario | Precondition | Steps | Expected Result | Actual Result |
|----|----------|-------------|-------|-----------------|---------------|
| TC-B01 | Complete a booking successfully | Logged in | Select an available scooter, confirm time and plan, submit | Booking succeeds, confirmation or booking ID is displayed | Pass |
| TC-B02 | Submit without selecting a scooter | Logged in | Do not select a scooter, click Submit directly | Shows "Please select a scooter", submission blocked | Pass |
| TC-B03 | Redirect to My Bookings after booking | Logged in | Complete a booking successfully | The new booking appears in the My Bookings page | My Bookings page is unavailable — error: `[plugin:vite:vue] Invalid end tag.` |

---

## 5. Cross-Page General Tests

**Objective:** Verify route access control, logout behaviour, and common browser interactions.

| ID | Scenario | Precondition | Steps | Expected Result | Actual Result |
|----|----------|-------------|-------|-----------------|---------------|
| TC-X01 | Navigation works after login | Logged in | Click each item in the navigation bar | All menu items navigate correctly, no 404 pages | My Bookings page fails to load |
| TC-X02 | Logout works | Logged in | Click the logout button | Login state is cleared, redirected to home or login page | Pass |
| TC-X03 | Access protected page after logout | Just logged out | Enter a protected page URL in the address bar | Automatically redirects to login page, content not shown | Does not redirect automatically |
| TC-X04 | Login state persists on refresh | Logged in | Press F5 to refresh any page | Still logged in after refresh, no redirect to login | Pass |
| TC-X05 | Browser back button | During normal usage | Click the browser back button | Returns to the previous page with correct content | Pass |
| TC-X06 | Loading state during page transition | Normal network | Switch between pages quickly | No blank screen or errors, loading indicators work correctly | Pass |

---

## 6. Test Summary

### 6.1 Defect Log

| Bug ID | Module | Description | Severity | Status |
|--------|--------|-------------|----------|--------|
| BUG-001 | Login / Register | Incorrect username or password causes page to refresh with no error message shown | Critical | Open |
| BUG-002 | Login / Register | Logged-in users can access the Register page directly via URL | Minor | Open |
| BUG-003 | My Bookings | Page is completely unavailable | Critical | Open |
| BUG-004 | Login / Register | After logout, navigating directly to a protected URL does not redirect to the login page | Moderate | Open |

### 6.2 Overall Assessment

Core functionality is working as expected. 4 issues were identified, including 2 critical defects. All bugs have been reported to the development team for resolution.

---

Document Date: 2026/03/08
