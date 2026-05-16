<<<<<<< HEAD
import React, { useEffect, useRef, useState } from 'react';
import MapView from 'react-native-maps';
import { ActivityIndicator, Platform, Pressable, StyleSheet, Text, View } from 'react-native';
=======
import React, { useEffect, useMemo, useState } from 'react';
import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';
import { MapView, Overlay } from 'react-native-baidu-map-yzg-wu';
>>>>>>> origin/feature/backend-sprint4-improving
import { Vehicle } from '@models/index';
import { colors, radii } from '@theme/index';
import { initializeBaiduMap } from '@services/baiduMap';
import { Coordinate, toBaiduCoordinate } from '@utils/baiduCoordinates';

const { Marker } = Overlay;
const BaiduMarker = Marker as any;
const BaiduMarkerIcon = (Overlay as any).MarkerIcon as React.ComponentType<any>;

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

<<<<<<< HEAD
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
=======
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
  const [hasAdoptedResolvedLocation, setHasAdoptedResolvedLocation] = useState(false);

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

  useEffect(() => {
    const shouldAdoptResolvedLocation = !!userLocation && !hasAdoptedResolvedLocation;
    if (shouldAdoptResolvedLocation) {
      setMapCenter(targetCenter);
      setMapZoom(targetZoom);
      setHasAdoptedResolvedLocation(true);
    }
  }, [hasAdoptedResolvedLocation, targetCenter, targetZoom, userLocation]);

  const getMarkerColor = (vehicle: Vehicle, isSelected?: boolean) => {
    if (isSelected) return colors.lime;
    if (vehicle.status === 'available') return colors.success;
    if (vehicle.battery < 20) return colors.warning;
    return colors.textSecondary;
  };

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
          onMapStatusChange={(event: {
            target?: { latitude: number; longitude: number; zoom?: number };
          }) => {
            if (event?.target) {
              setMapCenter({
                latitude: event.target.latitude,
                longitude: event.target.longitude,
              });
              if (typeof event.target.zoom === 'number') {
                setMapZoom(event.target.zoom);
              }
            }
          }}
        >
          {displayVehicles.map((vehicle) => (
            <BaiduMarker
              key={vehicle.id}
              location={vehicle.baiduLocation}
              onClick={() => onSelectVehicle?.(vehicle.id)}
            >
              <BaiduMarkerIcon
                style={[
                  styles.marker,
                  { borderColor: getMarkerColor(vehicle, selectedVehicleId === vehicle.id) },
                  selectedVehicleId === vehicle.id && styles.markerSelected,
                ]}
              >
                <View style={styles.markerInner}>
                  <Text style={styles.markerText}>{vehicle.battery}%</Text>
                </View>
              </BaiduMarkerIcon>
            </BaiduMarker>
          ))}
        </MapView>
      ) : (
        <View style={styles.mapFallback} />
      )}
>>>>>>> origin/feature/backend-sprint4-improving
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
  marker: {
    borderRadius: 18,
    paddingHorizontal: 2,
    paddingVertical: 2,
    borderWidth: 2,
  },
  markerInner: {
    backgroundColor: colors.ink,
    borderRadius: 16,
    paddingHorizontal: 10,
    paddingVertical: 6,
    minWidth: 48,
    alignItems: 'center',
  },
  markerText: {
    color: colors.textPrimary,
    fontWeight: '700',
    fontSize: 12,
  },
  markerSelected: {
    transform: [{ scale: 1.08 }],
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
