export type VehicleType = 'scooter' | 'bike' | 'moped';
export type VehicleStatus = 'available' | 'low-battery' | 'reserved' | 'in-ride';

export interface Vehicle {
  id: string;
  name: string;
  type: VehicleType;
  modelCode?: string;
  modelName?: string;
  battery: number;
  distance: number;
  lat: number;
  lng: number;
  pricePerMin: number;
  status: VehicleStatus;
  isFavorite?: boolean;
  zoneId?: string;
  image?: string | number;
  hardwareRevision?: string;
}

export interface RideZone {
  id: string;
  name: string;
  type: 'slow' | 'no-parking' | 'boost';
  speedLimit: number;
  color: string;
  vertices: { latitude: number; longitude: number }[];
}

export interface Trip {
  id: string;
  bookingRef: string;
  scooterId: string;
  status: string;
  startAt: string | null;
  endAt: string | null;
  priceFinal: number;
  updatedAt: string;
}

export interface Pass {
  id: string;
  name: string;
  price: number;
  durationMinutes: number;
  currency: string;
  highlight?: string;
}

export interface WalletTransaction {
  id: string;
  title: string;
  description: string;
  date: string;
  amount: number;
  type: 'credit' | 'debit';
}

export type RootStackParamList = {
  Login: undefined;
  Register: undefined;
  Main: undefined;
  RideDetail: { bookingId: string };
};

export type MainTabParamList = {
  Ride: undefined;
  Trips: undefined;
  Wallet: undefined;
  Profile: undefined;
};
