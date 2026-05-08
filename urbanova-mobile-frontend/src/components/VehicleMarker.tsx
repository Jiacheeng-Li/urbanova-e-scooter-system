import React from 'react';
import { Marker } from 'react-native-maps';
import { colors } from '@theme/colors';

interface VehicleMarkerProps {
  vehicle: {
    id: string;
    name: string;
    battery: number;
    status: string;
    lat: number;
    lng: number;
  };
  isSelected?: boolean;
  onPress: (vehicleId: string) => void;
}

const VehicleMarker: React.FC<VehicleMarkerProps> = ({ vehicle, isSelected, onPress }) => {
  const latitude = Number(vehicle.lat);
  const longitude = Number(vehicle.lng);

  if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
    return null;
  }

  const pinColor = isSelected
    ? colors.lime
    : vehicle.status === 'available'
      ? colors.success
      : vehicle.battery < 20
        ? colors.warning
        : colors.textSecondary;

  return (
    <Marker
      coordinate={{ latitude, longitude }}
      title={vehicle.name}
      description={`${vehicle.battery}% battery`}
      pinColor={pinColor}
      onPress={() => onPress(vehicle.id)}
    />
  );
};

export default VehicleMarker;
