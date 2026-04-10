import { create } from 'zustand';
import { Vehicle } from '@models/index';

export type RideMode = 'explore' | 'reserved' | 'active';
export type VehicleFilter = 'all' | 'scooter' | 'bike' | 'moped';

interface RideState {
  selectedVehicle: Vehicle | null;
  filter: VehicleFilter;
  rideMode: RideMode;
  reserveVehicle: (vehicle: Vehicle) => void;
  setSelectedVehicle: (vehicle: Vehicle | null) => void;
  setFilter: (filter: VehicleFilter) => void;
  setRideMode: (mode: RideMode) => void;
}

export const useRideStore = create<RideState>((set) => ({
  selectedVehicle: null,
  filter: 'all',
  rideMode: 'explore',
  reserveVehicle: (vehicle) =>
    set({
      selectedVehicle: vehicle,
      rideMode: 'reserved',
    }),
  setSelectedVehicle: (selectedVehicle) => set({ selectedVehicle }),
  setFilter: (filter) => set({ filter }),
  setRideMode: (rideMode) => set({ rideMode }),
}));
