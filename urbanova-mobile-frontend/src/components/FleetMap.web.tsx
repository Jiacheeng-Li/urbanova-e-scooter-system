import React from 'react';
import { Platform, StyleSheet, Text, View } from 'react-native';
import { Vehicle } from '@models/index';
import { colors } from '@theme/colors';

interface Props {
  vehicles: Vehicle[];
  initialRegion: {
    latitude: number;
    longitude: number;
    latitudeDelta: number;
    longitudeDelta: number;
  };
  selectedVehicleId?: string | null;
  onSelectVehicle?: (vehicleId: string) => void;
}

const FleetMap: React.FC<Props> = ({ vehicles, initialRegion, onSelectVehicle }) => {
  const lat = initialRegion.latitude;
  const lng = initialRegion.longitude;
  const bbox = `${(lng - 0.01).toFixed(4)}%2C${(lat - 0.01).toFixed(4)}%2C${(lng + 0.01).toFixed(4)}%2C${(lat + 0.01).toFixed(4)}`;
  const url = `https://www.openstreetmap.org/export/embed.html?bbox=${bbox}&layer=mapnik&marker=${lat}%2C${lng}`;

  return (
    <View style={styles.wrapper}>
      {React.createElement('iframe', {
        src: url,
        style: iframeStyle,
        title: 'Fleet map',
      })}
      <View style={styles.overlay}>
        <Text style={styles.overlayTitle}>Live map preview</Text>
        <Text style={styles.overlaySubtitle}>Open the Expo Go app on a device to see the interactive map.</Text>
        {vehicles.slice(0, 3).map((vehicle) => (
          <Text
            key={vehicle.id}
            style={styles.vehicleLine}
            onPress={() => onSelectVehicle?.(vehicle.id)}
          >
            - {vehicle.name} ({vehicle.battery}% battery)
          </Text>
        ))}
      </View>
    </View>
  );
};

const iframeStyle: React.CSSProperties = {
  border: 'none',
  width: '100%',
  height: '100%',
};

const styles = StyleSheet.create({
  wrapper: {
    ...StyleSheet.absoluteFillObject,
    borderRadius: 24,
    overflow: 'hidden',
  },
  overlay: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    backgroundColor: 'rgba(5,9,5,0.85)',
    padding: 16,
  },
  overlayTitle: {
    color: colors.textPrimary,
    fontSize: 16,
    fontWeight: '600',
  },
  overlaySubtitle: {
    color: colors.textSecondary,
    marginTop: 6,
    marginBottom: 10,
  },
  vehicleLine: {
    color: colors.textPrimary,
    marginTop: 4,
    cursor: 'pointer',
  } as any,
});

export default FleetMap;
