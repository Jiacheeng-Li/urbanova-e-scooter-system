import { useQuery } from '@tanstack/react-query';
import { ScooterService, ScooterMapPoint } from '@services/api';
import { getVehicleModelImage } from '@data/vehicleImages';

interface UseVehiclesOptions {
  lat?: number | null;
  lng?: number | null;
  radiusKm?: number;
}

export const useVehicles = ({ lat, lng, radiusKm = 5 }: UseVehiclesOptions = {}) => {
  const hasLocation = typeof lat === 'number' && typeof lng === 'number';

  const query = useQuery({
    queryKey: ['vehicles', hasLocation ? 'nearby' : 'map-points', lat, lng, radiusKm],
    queryFn: async () => {
      if (!hasLocation) {
        return ScooterService.getMapPoints();
      }
      try {
        const nearbyVehicles = await ScooterService.getNearby(lat, lng, radiusKm);
        if (nearbyVehicles.length > 0) {
          return nearbyVehicles;
        }
        console.warn('Nearby vehicle lookup returned no vehicles, falling back to map points.');
        return ScooterService.getMapPoints();
      } catch (error) {
        console.warn('Nearby vehicle lookup failed, falling back to map points.', error);
        return ScooterService.getMapPoints();
      }
    },
  });

  return {
    ...query,
    vehicles: query.data ?? [],
  };
};

export const mapPointToVehicle = (point: ScooterMapPoint) => ({
  id: point.scooterId,
  name: `URBANOVA ${point.scooterId.slice(-4)}`,
  type: 'scooter' as const,
  modelCode: point.typeCode,
  modelName: point.typeDisplayName || point.typeCode || 'Standard',
  battery: point.batteryPercent,
  distance: point.distance ?? 0,
  lat: point.lat,
  lng: point.lng,
  pricePerMin: 1.99,
  status: mapStatus(point.status),
  zoneId: point.zone ?? undefined,
  image: getVehicleModelImage(point.typeCode),
});

const mapStatus = (apiStatus: string): 'available' | 'low-battery' | 'reserved' | 'in-ride' => {
  switch (apiStatus?.toLowerCase()) {
    case 'available':
      return 'available';
    case 'low_battery':
    case 'low-battery':
      return 'low-battery';
    case 'reserved':
      return 'reserved';
    case 'in_use':
    case 'in_ride':
    case 'in-ride':
      return 'in-ride';
    default:
      return 'available';
  }
};
