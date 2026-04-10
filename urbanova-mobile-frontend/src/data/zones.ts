import { RideZone } from '@models/index';

export const rideZones: RideZone[] = [
  {
    id: 'slow-1',
    name: 'Market Street Slow Zone',
    type: 'slow',
    speedLimit: 10,
    color: '#FFC85755',
    vertices: [
      { latitude: 37.7765, longitude: -122.423 },
      { latitude: 37.7762, longitude: -122.418 },
      { latitude: 37.7734, longitude: -122.4185 },
      { latitude: 37.7737, longitude: -122.4235 },
    ],
  },
  {
    id: 'no-parking-1',
    name: 'Embarcadero No-Parking',
    type: 'no-parking',
    speedLimit: 0,
    color: '#F45B6944',
    vertices: [
      { latitude: 37.8005, longitude: -122.401 },
      { latitude: 37.7985, longitude: -122.392 },
      { latitude: 37.7955, longitude: -122.393 },
      { latitude: 37.7975, longitude: -122.402 },
    ],
  },
];
