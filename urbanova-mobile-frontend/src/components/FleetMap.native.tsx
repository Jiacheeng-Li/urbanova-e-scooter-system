import React, { useEffect, useMemo, useState } from 'react';
import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';
import { MapView, Overlay } from 'react-native-baidu-map-yzg-wu';

import { Vehicle } from '@models/index';
import { colors, radii } from '@theme/index';
import { initializeBaiduMap } from '@services/baiduMap';
import { Coordinate, toBaiduCoordinate } from '@utils/baiduCoordinates';

const { Marker } = Overlay;
const BaiduMarker = Marker as any;

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
  userLocation?: Coordinate | null;
}

const longitudeDeltaToZoom = (longitudeDelta: number) => {
  const safeDelta = Math.max(longitudeDelta, 0.0005);
  const zoom = Math.log2(360 / safeDelta);
  return Math.min(20, Math.max(5, Math.round(zoom)));
};

const FleetMap: React.FC<Props> = ({
  vehicles,
  initialRegion,
  selectedVehicleId,
  onSelectVehicle,
  userLocation,
}) => {
  const [isReady, setIsReady] = useState(false);
  const [mapError, setMapError] = useState<string | null>(null);

  const targetCenter = useMemo(
    () =>
      toBaiduCoordinate({
        latitude: initialRegion.latitude,
        longitude: initialRegion.longitude,
      }),
    [initialRegion.latitude, initialRegion.longitude]
  );
  const targetZoom = useMemo(
    () => longitudeDeltaToZoom(initialRegion.longitudeDelta),
    [initialRegion.longitudeDelta]
  );

  const [mapCenter, setMapCenter] = useState<Coordinate>(targetCenter);
  const [mapZoom, setMapZoom] = useState(targetZoom);
  const [hasCenteredOnFleet, setHasCenteredOnFleet] = useState(false);

  useEffect(() => {
    const result = initializeBaiduMap();
    if (!result.ready && result.error) {
      setMapError(result.error);
    }
  }, []);

  const baiduUserLocation = useMemo(
    () => (userLocation ? toBaiduCoordinate(userLocation) : null),
    [userLocation]
  );
  const displayVehicles = useMemo(
    () =>
      vehicles.map((vehicle) => ({
        ...vehicle,
        baiduLocation: toBaiduCoordinate({
          latitude: vehicle.lat,
          longitude: vehicle.lng,
        }),
      })),
    [vehicles]
  );

  const handleMarkerClick = useMemo(
    () => (event: { title?: string; position?: { latitude?: number; longitude?: number } } | null | undefined) => {
      const markerVehicleId = event?.title?.split('|')[0];
      const matchedByTitle = markerVehicleId
        ? displayVehicles.find((vehicle) => vehicle.id === markerVehicleId)
        : undefined;
      if (matchedByTitle) {
        onSelectVehicle?.(matchedByTitle.id);
        return;
      }

      const latitude = event?.position?.latitude;
      const longitude = event?.position?.longitude;
      if (typeof latitude !== 'number' || typeof longitude !== 'number') {
        return;
      }
      const matched = displayVehicles.find((vehicle) => {
        const latDiff = Math.abs(vehicle.baiduLocation.latitude - latitude);
        const lngDiff = Math.abs(vehicle.baiduLocation.longitude - longitude);
        return latDiff < 0.0001 && lngDiff < 0.0001;
      });
      if (matched) {
        onSelectVehicle?.(matched.id);
      }
    },
    [displayVehicles, onSelectVehicle]
  );

  useEffect(() => {
    if (vehicles.length > 0 && !hasCenteredOnFleet) {
      setMapCenter(targetCenter);
      setMapZoom(targetZoom);
      setHasCenteredOnFleet(true);
    }
  }, [hasCenteredOnFleet, targetCenter, targetZoom, vehicles.length]);

  return (
    <View style={styles.container}>
      {!mapError ? (
        <MapView
          style={styles.map}
          center={mapCenter}
          zoom={mapZoom}
          mapType={1}
          showsUserLocation={!!baiduUserLocation}
          locationData={baiduUserLocation || undefined}
          scrollGesturesEnabled
          zoomGesturesEnabled
          zoomControlsVisible={false}
          onMapLoaded={() => setIsReady(true)}
          onMarkerClick={handleMarkerClick}
        >
          {displayVehicles.map((vehicle) => (
            <BaiduMarker
              key={vehicle.id}
              location={vehicle.baiduLocation}
              title={`${vehicle.id}|${vehicle.name} - ${vehicle.status}`}
              pinColor={selectedVehicleId === vehicle.id ? 'purple' : 'green'}
              alpha={selectedVehicleId === vehicle.id ? 1 : 0.92}
              onClick={() => onSelectVehicle?.(vehicle.id)}
            />
          ))}
        </MapView>
      ) : (
        <View style={styles.mapFallback} />
      )}
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
  mapFallback: {
    flex: 1,
    backgroundColor: colors.ink,
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
