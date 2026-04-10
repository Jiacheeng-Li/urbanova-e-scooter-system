import { useQuery } from '@tanstack/react-query';
import { rideZones } from '@data/zones';
import { RideZone } from '@models/index';

const simulateLatency = async <T>(payload: T, ms = 350): Promise<T> =>
  new Promise((resolve) => setTimeout(() => resolve(payload), ms));

export const useRideZones = () => {
  const query = useQuery({
    queryKey: ['ride-zones'],
    queryFn: () => simulateLatency<RideZone[]>(rideZones),
  });

  return {
    ...query,
    zones: query.data ?? [],
  };
};
