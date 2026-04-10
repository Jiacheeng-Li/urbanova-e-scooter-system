import React, { useState } from 'react';
import MapView, { Marker } from 'react-native-maps';
import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';
import { Vehicle } from '@models/index';
import { colors, radii } from '@theme/index';

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

const FleetMap: React.FC<Props> = ({ vehicles, initialRegion, selectedVehicleId, onSelectVehicle }) => {
  const [isReady, setIsReady] = useState(false);
  const [mapError, setMapError] = useState<string | null>(null);

  return (
    <View style={styles.container}>
      <MapView
        style={styles.map}
        initialRegion={initialRegion}
        showsUserLocation
        showsMyLocationButton
        scrollEnabled
        pitchEnabled
        moveOnMarkerPress={false}
        loadingEnabled
        loadingIndicatorColor={colors.lime}
        onMapReady={() => setIsReady(true)}
        onError={(event: { nativeEvent?: { message?: string } }) =>
          setMapError(event?.nativeEvent?.message || 'Unable to render map')
        }
      >
        {vehicles.map((vehicle) => (
          <Marker
            key={vehicle.id}
            coordinate={{ latitude: vehicle.lat, longitude: vehicle.lng }}
            title={vehicle.name}
            onPress={() => onSelectVehicle?.(vehicle.id)}
          >
            <View style={[styles.marker, selectedVehicleId === vehicle.id && styles.markerSelected]}>
              <Text style={styles.markerText}>{vehicle.battery}%</Text>
            </View>
          </Marker>
        ))}
      </MapView>
      {!isReady && !mapError && (
        <View style={styles.statusOverlay}>
          <ActivityIndicator color={colors.textPrimary} />
          <Text style={styles.statusText}>Loading map...</Text>
        </View>
      )}
      {mapError && (
        <View style={styles.errorState}>
          <Text style={styles.errorTitle}>Map unavailable</Text>
          <Text style={styles.errorSubtitle}>We could not load the map. Check your connection or location settings.</Text>
        </View>
      )}
      {vehicles.length === 0 && !mapError && (
        <View style={styles.emptyState}>
          <Text style={styles.emptyTitle}>Looking for URBANOVA vehicles...</Text>
          <Text style={styles.emptySubtitle}>Make sure location permission is granted to display the map.</Text>
        </View>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    ...StyleSheet.absoluteFillObject,
  },
  map: {
    flex: 1,
  },
  marker: {
    backgroundColor: colors.ink,
    borderRadius: 16,
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderWidth: 2,
    borderColor: colors.lime,
  },
  markerSelected: {
    transform: [{ scale: 1.1 }],
    borderColor: colors.warning,
  },
  markerText: {
    color: colors.textPrimary,
    fontWeight: '700',
  },
  emptyState: {
    position: 'absolute',
    bottom: 40,
    left: 20,
    right: 20,
    padding: 16,
    borderRadius: radii.md,
    backgroundColor: 'rgba(5,9,5,0.85)',
  },
  emptyTitle: {
    color: colors.textPrimary,
    fontWeight: '600',
    fontSize: 16,
  },
  emptySubtitle: {
    color: colors.textSecondary,
    marginTop: 4,
    fontSize: 13,
  },
  statusOverlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'rgba(5,9,5,0.45)',
  },
  statusText: {
    marginTop: 8,
    color: colors.textPrimary,
  },
  errorState: {
    position: 'absolute',
    left: 20,
    right: 20,
    top: 80,
    padding: 16,
    borderRadius: radii.md,
    backgroundColor: 'rgba(5,9,5,0.9)',
  },
  errorTitle: {
    color: colors.textPrimary,
    fontWeight: '700',
    fontSize: 16,
  },
  errorSubtitle: {
    color: colors.textSecondary,
    marginTop: 6,
    fontSize: 13,
    lineHeight: 18,
  },
});

export default FleetMap;
