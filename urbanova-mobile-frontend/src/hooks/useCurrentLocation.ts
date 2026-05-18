import { useEffect, useState } from 'react';
import { getCurrentLocation, LocationResult, watchBestLocation } from '@services/location';

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

    const subscriptionPromise = watchBestLocation(
      (coords) => {
        if (mounted) {
          setLocation(coords);
        }
      },
      (error) => {
        console.warn('Location watch error', error);
      }
    );

    return () => {
      mounted = false;
      subscriptionPromise.then((subscription) => {
        subscription?.remove();
      });
    };
  }, []);

  return { location, isLoading };
};
