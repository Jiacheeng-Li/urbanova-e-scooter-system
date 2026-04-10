import { useQuery } from '@tanstack/react-query';
import { BookingService } from '@services/api';
import { Trip } from '@models/index';

export const useTrips = () => {
  const query = useQuery({
    queryKey: ['bookings'],
    queryFn: () => BookingService.list(),
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
