import { useQuery } from '@tanstack/react-query';
import { BookingService } from '@services/api';
import { Trip } from '@models/index';
import { useAuthStore } from '@store/useAuthStore';

export const useTrips = () => {
  const userId = useAuthStore((state) => state.user?.userId);
  const query = useQuery({
    queryKey: ['bookings', userId ?? 'guest'],
    queryFn: () => BookingService.list(),
    enabled: !!userId,
  });

  const trips: Trip[] = (query.data ?? []).map((booking) => ({
    id: booking.bookingId,
    bookingRef: booking.bookingRef,
    scooterId: booking.scooterId,
    status: booking.status,
    startAt: booking.startAt,
    endAt: booking.endAt,
    priceFinal: Number(booking.priceFinal || 0),
    updatedAt: booking.updatedAt,
  }));

  return {
    ...query,
    trips,
  };
};
