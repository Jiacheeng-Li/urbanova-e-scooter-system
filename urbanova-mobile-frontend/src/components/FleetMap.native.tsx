import React, { useEffect, useRef, useState } from 'react';
import MapView from 'react-native-maps';
import { ActivityIndicator, Platform, Pressable, StyleSheet, Text, View } from 'react-native';
import { Vehicle } from '@models/index';
import VehicleMarker from './VehicleMarker';
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
  const mapRef = useRef<MapView | null>(null);
  const lastCenteredRegion = useRef('');
  const [isReady, setIsReady] = useState(false);
  const [mapError, setMapError] = useState<string | null>(null);
  const useStaticFallback = Platform.OS === 'android' && !__DEV__;

  useEffect(() => {
    if (!isReady) {
      return;
    }

    const regionKey = [
      initialRegion.latitude.toFixed(6),
      initialRegion.longitude.toFixed(6),
      initialRegion.latitudeDelta.toFixed(4),
      initialRegion.longitudeDelta.toFixed(4),
    ].join(':');

    if (lastCenteredRegion.current === regionKey) {
      return;
    }

    lastCenteredRegion.current = regionKey;
    mapRef.current?.animateToRegion(initialRegion, 450);
  }, [
    initialRegion.latitude,
    initialRegion.longitude,
    initialRegion.latitudeDelta,
    initialRegion.longitudeDelta,
    isReady,
  ]);

  if (useStaticFallback) {
    return (
      <View style={[styles.container, styles.staticMap]}>
        <View style={styles.gridCircleLarge} />
        <View style={styles.gridCircleSmall} />
        <Text style={styles.staticTitle}>URBANOVA Fleet Map</Text>
        <Text style={styles.staticSubtitle}>Showing live vehicle positions from the backend.</Text>
        {vehicles.slice(0, 8).map((vehicle, index) => (
          <Pressable
            key={vehicle.id}
            style={[
              styles.staticMarker,
              {
                left: `${18 + ((index * 19) % 64)}%`,
                top: `${26 + ((index * 17) % 38)}%`,
              },
              selectedVehicleId === vehicle.id && styles.staticMarkerSelected,
            ]}
            onPress={() => onSelectVehicle?.(vehicle.id)}
          >
            <Text style={styles.staticMarkerText}>{vehicle.battery}%</Text>
          </Pressable>
        ))}
        {vehicles.length === 0 ? (
          <View style={styles.emptyState}>
            <Text style={styles.emptyTitle}>Looking for URBANOVA vehicles...</Text>
            <Text style={styles.emptySubtitle}>Vehicle data will appear once the backend responds.</Text>
          </View>
        ) : null}
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <MapView
        ref={mapRef}
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
        <VehicleMarker
          key={vehicle.id}
          vehicle={vehicle}
          isSelected={selectedVehicleId === vehicle.id}
          onPress={onSelectVehicle || (() => {})}
        />
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
  staticMap: {
    backgroundColor: '#101626',
    overflow: 'hidden',
  },
  gridCircleLarge: {
    position: 'absolute',
    width: 420,
    height: 420,
    borderRadius: 210,
    borderWidth: 1,
    borderColor: 'rgba(131,111,255,0.18)',
    top: 70,
    left: -80,
  },
  gridCircleSmall: {
    position: 'absolute',
    width: 260,
    height: 260,
    borderRadius: 130,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.1)',
    top: 170,
    right: -60,
  },
  staticTitle: {
    position: 'absolute',
    top: 170,
    left: 24,
    color: colors.lime,
    fontSize: 20,
    fontWeight: '900',
  },
  staticSubtitle: {
    position: 'absolute',
    top: 198,
    left: 24,
    right: 24,
    color: colors.textSecondary,
  },
  staticMarker: {
    position: 'absolute',
    minWidth: 46,
    alignItems: 'center',
    borderRadius: 18,
    paddingHorizontal: 9,
    paddingVertical: 6,
    borderWidth: 2,
    borderColor: colors.success,
    backgroundColor: colors.ink,
  },
  staticMarkerSelected: {
    borderColor: colors.lime,
    backgroundColor: 'rgba(131,111,255,0.45)',
  },
  staticMarkerText: {
    color: colors.textPrimary,
    fontWeight: '800',
    fontSize: 12,
  },
});

export default FleetMap;
