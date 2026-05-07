// src/components/VehicleMarker.tsx
import React from 'react';
import { View, Text, StyleSheet, Pressable } from 'react-native';
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
  const getMarkerColor = () => {
    if (isSelected) return colors.lime;
    if (vehicle.status === 'available') return colors.success;
    if (vehicle.battery < 20) return colors.warning;
    return colors.textSecondary;
  };

  return (
    <Marker
      coordinate={{ latitude: vehicle.lat, longitude: vehicle.lng }}
      onPress={() => onPress(vehicle.id)}
      tracksViewChanges={false}
    >
      <Pressable style={[styles.marker, { borderColor: getMarkerColor() }, isSelected && styles.selected]}>
        <Text style={styles.batteryText}>{vehicle.battery}%</Text>
      </Pressable>
    </Marker>
  );
};

const styles = StyleSheet.create({
  marker: {
    backgroundColor: colors.ink,
    borderRadius: 16,
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderWidth: 2,
    minWidth: 44,
    alignItems: 'center',
  },
  selected: {
    transform: [{ scale: 1.1 }],
  },
  batteryText: {
    color: colors.textPrimary,
    fontSize: 12,
    fontWeight: 'bold',
  },
});

export default VehicleMarker;