import { useEffect, useState } from 'react';
import { getCurrentLocation, LocationResult } from '@services/location';

export const useCurrentLocation = () => {
  const [location, setLocation] = useState<LocationResult | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let mounted = true;
    const fetchLocation = async () => {
      try {
        const coords = await getCurrentLocation();
        if (mounted) {
          setLocation(coords);
        }
      } catch (error) {
        console.warn('Location error', error);
      } finally {
        if (mounted) {
          setIsLoading(false);
        }
      }
    };

    fetchLocation();

    return () => {
      mounted = false;
    };
  }, []);

  return { location, isLoading };
};
