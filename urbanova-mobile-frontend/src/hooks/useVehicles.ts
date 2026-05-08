// useVehicles.ts
import { useQuery } from '@tanstack/react-query';
import { LocationService, NearbyScooterPoint } from '@services/api';
import { getVehicleModelImage } from '@data/vehicleImages';
import { useCurrentLocation } from './useCurrentLocation';

export const useVehicles = () => {
  const { location } = useCurrentLocation();
  
  const query = useQuery({
    queryKey: ['nearby-vehicles', location?.latitude, location?.longitude],
    queryFn: async () => {
      if (!location) {
        return [];
      }
      const response = await LocationService.getNearbyScooters(
        location.latitude,
        location.longitude,
        5  // 5公里半径
      );
      return response.data;
    },
    enabled: !!location,  // 只有获取到位置后才请求
  });

  return {
    ...query,
    vehicles: query.data ?? [],
  };
};

// 保留原有的转换函数
export const mapPointToVehicle = (point: NearbyScooterPoint) => ({
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
    case 'in-ride':
      return 'in-ride';
    default:
      return 'available';
  }
};