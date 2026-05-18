import * as Location from 'expo-location';

export interface LocationResult {
  latitude: number;
  longitude: number;
  accuracy: number;
}

const FALLBACK_LOCATION: LocationResult = {
  latitude: 30.764633,
  longitude: 103.983826,
  accuracy: 1000,
};

export const requestLocationPermission = async () => {
  const { status } = await Location.requestForegroundPermissionsAsync();
  return status === 'granted';
};

export const getCurrentLocation = async (): Promise<LocationResult> => {
  const granted = await requestLocationPermission();
  if (!granted) {
    return FALLBACK_LOCATION;
  }

  const servicesEnabled = await Location.hasServicesEnabledAsync();
  if (!servicesEnabled) {
    return FALLBACK_LOCATION;
  }

  const lastKnown = await Location.getLastKnownPositionAsync({
    maxAge: 2 * 60 * 1000,
    requiredAccuracy: 200,
  });

  if (lastKnown?.coords?.latitude && lastKnown?.coords?.longitude) {
    return {
      latitude: lastKnown.coords.latitude,
      longitude: lastKnown.coords.longitude,
      accuracy: lastKnown.coords.accuracy ?? 100,
    };
  }

  try {
    const location = await Location.getCurrentPositionAsync({
      accuracy: Location.Accuracy.High,
      mayShowUserSettingsDialog: true,
    });

    return {
      latitude: location.coords.latitude,
      longitude: location.coords.longitude,
      accuracy: location.coords.accuracy ?? 30,
    };
  } catch {
    return FALLBACK_LOCATION;
  }
};

export const watchBestLocation = (
  onUpdate: (location: LocationResult) => void,
  onError?: (error: unknown) => void
) =>
  Location.watchPositionAsync(
    {
      accuracy: Location.Accuracy.High,
      timeInterval: 3000,
      distanceInterval: 5,
    },
    (location) => {
      onUpdate({
        latitude: location.coords.latitude,
        longitude: location.coords.longitude,
        accuracy: location.coords.accuracy ?? 30,
      });
    }
  ).catch((error) => {
    onError?.(error);
    return null;
  });
