import { useQuery } from '@tanstack/react-query';
import { ScooterService, ScooterMapPoint } from '@services/api';

export const useVehicles = () => {
  const query = useQuery({
    queryKey: ['vehicles'],
    queryFn: ScooterService.getMapPoints,
  });

  return {
    ...query,
    vehicles: query.data ?? [],
  };
};

export const mapPointToVehicle = (point: ScooterMapPoint) => ({
  id: point.scooterId,
  name: `Urbanova ${point.scooterId.slice(-4)}`,
  type: 'scooter' as const,
  battery: point.batteryPercent,
  distance: 0,
  lat: point.lat,
  lng: point.lng,
  pricePerMin: 1.99,
  status: mapStatus(point.status),
  zoneId: point.zone ?? undefined,
  image: `https://source.unsplash.com/collection/190727/200x200?sig=${point.scooterId.slice(-3)}`,
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
    case 'in_ride':
    case 'in-ride':
      return 'in-ride';
    default:
      return 'available';
  }
};
