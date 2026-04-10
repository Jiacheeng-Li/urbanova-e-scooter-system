import { Vehicle, VehicleType } from '@models/index';

const baseLocation = {
  lat: 37.7749,
  lng: -122.4194,
};

const vehicleImage = (type: VehicleType) => {
  switch (type) {
    case 'bike':
      return 'https://images.unsplash.com/photo-1485965120184-e220f721d03e?auto=format&fit=crop&w=600&q=60';
    case 'moped':
      return 'https://images.unsplash.com/photo-1502877338535-766e1452684a?auto=format&fit=crop&w=600&q=60';
    default:
      return 'https://images.unsplash.com/photo-1485965120184-e220f721d03e?auto=format&fit=crop&w=600&q=60';
  }
};

export const vehicles: Vehicle[] = [
  {
    id: 'URB-9380',
    name: 'Urbanova S 9380',
    type: 'scooter',
    battery: 78,
    distance: 0.2,
    lat: baseLocation.lat + 0.002,
    lng: baseLocation.lng - 0.002,
    pricePerMin: 0.39,
    status: 'available',
    isFavorite: true,
    hardwareRevision: 'S4.2',
    image: vehicleImage('scooter'),
  },
  {
    id: 'URB-5584',
    name: 'Urbanova S 5584',
    type: 'scooter',
    battery: 42,
    distance: 0.5,
    lat: baseLocation.lat - 0.003,
    lng: baseLocation.lng + 0.0015,
    pricePerMin: 0.39,
    status: 'low-battery',
    hardwareRevision: 'S4.2',
    image: vehicleImage('scooter'),
  },
  {
    id: 'URB-2041',
    name: 'Urbanova E 2041',
    type: 'bike',
    battery: 61,
    distance: 0.7,
    lat: baseLocation.lat + 0.004,
    lng: baseLocation.lng + 0.003,
    pricePerMin: 0.35,
    status: 'available',
    hardwareRevision: 'E3.1',
    image: vehicleImage('bike'),
  },
  {
    id: 'URB-8300',
    name: 'Urbanova M 8300',
    type: 'moped',
    battery: 91,
    distance: 1.2,
    lat: baseLocation.lat - 0.006,
    lng: baseLocation.lng - 0.003,
    pricePerMin: 0.49,
    status: 'reserved',
    hardwareRevision: 'M2.0',
    image: vehicleImage('moped'),
  },
];
