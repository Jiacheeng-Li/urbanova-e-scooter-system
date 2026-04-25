# URBANOVA - Expo React Native Client

URBANOVA mobile app built with Expo + React Native and integrated with the Sprint 3 backend in `../urbanova`.

## Tech stack

- Expo SDK 54 (React Native 0.81, React 19)
- React Navigation (native stack + bottom tabs)
- React Query + Axios
- Zustand for auth/session state

## Key Sprint 3 flows implemented

1. Booking lifecycle
- Update booking (`PATCH /api/v1/bookings/{id}`)
- Start / end / extend booking (`/start`, `/end`, `/extend`)
- Timeline display (`GET /api/v1/bookings/{id}/timeline`)
- Return-zone validation before ending ride (mobile-side geo check against no-parking zones)
- Return fault reporting (`POST /api/v1/issues`)

2. Payment + confirmation + status
- Simulated payment in booking detail (`POST /api/v1/bookings/{id}/payments`)
- Payment records shown in app (`GET /api/v1/bookings/{id}/payments`)
- Confirmation display + resend (`GET /bookings/{id}/confirmation`, `POST /resend`)
- Booking/payment status updated and visible in Ride detail + Trips

3. Card binding + account security
- Card binding during registration (optional)
- Card management in Wallet (`GET/POST/PATCH/DELETE /api/v1/payment-methods`)
- Default card switching (`POST /payment-methods/{id}/default`)
- Profile update + usage summary (`PATCH /users/me`, `GET /users/me/usage-summary`)
- Forgot/reset password flow in Security Center

## Security behavior

- Access token and refresh token are stored in AsyncStorage keys `accessToken` and `refreshToken`.
- On 401 responses, local session is cleared automatically.
- Logout calls backend logout and clears local session.
- Card UI only displays masked card info (`brand + last4`), never full card number.
- Sensitive operations include client-side validation:
  - password strength and confirm match
  - card number length and expiry format
  - profile field validation (required full name, phone format)
- Destructive card operations require explicit user confirmation.

## Running locally

1. Start backend:
```powershell
cd ..\urbanova
mvn spring-boot:run
```

2. Start mobile app:
```powershell
cd ..\urbanova-mobile-frontend
npm install
npm run start
```

3. Configure API base URL in `app.json -> expo.extra.apiBaseUrl` for emulator/device LAN.

## Notes

- Currency display is GBP.
- Return-zone validation currently uses mobile-side zone polygons (`src/data/zones.ts`) because backend does not expose dedicated return-zone validation endpoint yet.

## Vehicle image assets

Ride screen vehicle cards now load local model images from `assets/vehicle-models/`.

Replace these files with your own PNG assets (keep file names unchanged):

- `assets/vehicle-models/andromeda.png`
- `assets/vehicle-models/galaxy-seat.png`
- `assets/vehicle-models/lunar-lite.png`
- `assets/vehicle-models/nebula-family.png`
- `assets/vehicle-models/orion-ultra.png`
- `assets/vehicle-models/urbanova-default.png`

Type-to-image mapping is defined in `src/data/vehicleImages.ts`.
