import { Trip } from '@models/index';

export const trips: Trip[] = [
  {
    id: 'trip-1',
    bookingRef: 'BK-2041',
    scooterId: 'URB-2041',
    status: 'COMPLETED',
    startAt: '2026-04-05T14:22:00Z',
    endAt: '2026-04-05T14:38:00Z',
    priceFinal: 5.3,
    updatedAt: '2026-04-05T14:38:10Z',
  },
  {
    id: 'trip-2',
    bookingRef: 'BK-8300',
    scooterId: 'URB-8300',
    status: 'CANCELLED',
    startAt: '2026-04-02T09:10:00Z',
    endAt: null,
    priceFinal: 0,
    updatedAt: '2026-04-02T09:15:00Z',
  },
  {
    id: 'trip-3',
    bookingRef: 'BK-9380',
    scooterId: 'URB-9380',
    status: 'IN_PROGRESS',
    startAt: '2026-04-07T03:18:00Z',
    endAt: null,
    priceFinal: 0,
    updatedAt: '2026-04-07T03:25:00Z',
  },
];
