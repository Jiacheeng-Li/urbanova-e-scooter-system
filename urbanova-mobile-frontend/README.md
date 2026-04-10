# URBANOVA – Expo React Native Client

URBANOVA is a shared micromobility client built with Expo + React Native. It now talks directly to the Urbanova Spring Boot backend (located at `../urbanova-e-scooter-system-feature-backend-sprint2/urbanova`) so that login, scooter discovery, hire options, and booking flows are backed by live APIs.

## Tech stack

- Expo SDK 54 (React Native 0.81, React 19)
- React Navigation (native stack + bottom tabs)
- React Query + Axios for API calls
- Zustand for client-side auth/session state
- React Native Maps, Expo Location, Linear Gradient, Blur, etc.

## Project structure

```
urbanova-mobile/
├── App.tsx              # Re-exports src/App
├── app.json             # Expo + native config (apiBaseUrl lives here)
├── babel.config.js      # Path aliases + Reanimated plugin
├── src/
│   ├── App.tsx          # Providers + root navigator
│   ├── navigation/      # AppNavigator, MainTabs, stack types
│   ├── screens/         # Auth, Ride, Trips, Wallet, Profile, RideDetail
│   ├── components/      # Buttons, cards, markers, layout helpers
│   ├── hooks/           # React Query hooks, Zustand helpers
│   ├── store/           # Auth + ride selections
│   ├── services/        # Axios API client (auth, scooters, bookings, hire)
│   ├── theme/           # Colors, spacing, typography tokens
│   ├── types/           # Shared domain/view-model interfaces
│   └── utils/           # Formatters (currency, time)
```

## Backend integration

1. **Run the backend**
   ```powershell
   cd ..\urbanova-e-scooter-system-feature-backend-sprint2\urbanova
   .\mvnw.cmd spring-boot:run
   ```
   By default the server listens on `http://localhost:8080` and connects to the MySQL instance defined in `src/main/resources/application.properties`.

2. **Configure the mobile API base URL**
   - `app.json -> expo.extra.apiBaseUrl` defaults to `http://10.0.2.2:8080` (Android emulator loopback).
   - For iOS simulator or web, change it to `http://127.0.0.1:8080`.
   - For a physical device, use your machine’s LAN IP, e.g. `http://192.168.1.20:8080`.
   - The Axios client automatically appends `/api/v1/...` paths and injects the JWT Bearer token once you log in.

3. **Available endpoints consumed by the app**
   - `POST /api/v1/auth/register` / `POST /api/v1/auth/login` – email + password auth returning JWT + user profile.
   - `GET /api/v1/users/me` – used to hydrate the profile tab on launch.
   - `GET /api/v1/scooters/map-points` – feeds the map/list of nearby vehicles.
   - `GET /api/v1/hire-options` – powers the hire-plan picker on the Ride + Wallet screens.
   - `POST /api/v1/bookings` / `GET /api/v1/bookings` / `GET /api/v1/bookings/{id}` – creating, listing, and viewing bookings (Trips tab + Ride detail).

4. **Auth flow**
   - Login & register screens now ask for email + password per backend contract (password ≥ 8 chars).
   - JWT tokens are stored in `AsyncStorage` and automatically injected into requests. Invalid/expired tokens clear local state and return to the login screen.

5. **Remaining mock data**
   - Wallet transactions still use `src/data/transactions.ts` because the backend has no finance API yet.
   - Ride zones overlays remain mocked (`src/data/zones.ts`). They can be swapped once the backend exposes geo-fence polygons.

## Running the mobile app

1. Install dependencies (already present, but run once after pulling):
   ```bash
   npm install
   ```
2. Start Metro / Expo:
   ```bash
   npm run start
   ```
3. Launch on your target:
   - Android emulator or device: `npm run android`
   - iOS simulator (macOS): `npm run ios`
   - Web preview for layout: `npm run web`

Expo Go or a custom dev client can scan the Metro QR to load URBANOVA. Ensure the backend is reachable from the device (adjust `apiBaseUrl` if necessary).

## Notes

- Path aliases (`@components`, `@screens`, `@models`, etc.) are configured in both `babel.config.js` and `tsconfig.json`.
- React Native Reanimated **must** remain the last Babel plugin.
- Location & maps permissions are already declared in `app.json`. For production you should add a real Google Maps API key inside the `react-native-maps` plugin section and configure HTTPS for the backend.
- To integrate additional backend modules, add strongly typed service methods under `src/services/api.ts` and consume them through hooks (preferred) so state and error handling stay centralized.
