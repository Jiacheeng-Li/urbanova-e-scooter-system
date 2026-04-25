import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  Animated,
  Dimensions,
  FlatList,
  PanResponder,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
  NativeSyntheticEvent,
  NativeScrollEvent,
} from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';

import { useVehicles, mapPointToVehicle } from '@hooks/useVehicles';
import { useCurrentLocation } from '@hooks/useCurrentLocation';
import { useRideStore, VehicleFilter } from '@store/useRideStore';
import { colors, radii } from '@theme/index';
import FilterChip from '@components/FilterChip';
import VehicleCard from '@components/VehicleCard';
import PrimaryButton from '@components/PrimaryButton';
import { BookingService } from '@services/api';
import { RootStackParamList } from '@models/index';
import { usePasses } from '@hooks/usePasses';
import FleetMap from '@components/FleetMap';
import { formatCurrency } from '@utils/format';

const filters: { label: string; value: VehicleFilter }[] = [
  { label: 'All', value: 'all' },
  { label: 'Scooters', value: 'scooter' },
  { label: 'Bikes', value: 'bike' },
  { label: 'Mopeds', value: 'moped' },
];

const SCREEN_HEIGHT = Dimensions.get('window').height;
const SHEET_HEIGHT = SCREEN_HEIGHT * 0.85;
const SHEET_SNAP_POINTS = {
  expanded: SCREEN_HEIGHT * 0.12,
  collapsed: SCREEN_HEIGHT * 0.55,
};

const RideScreen = () => {
  const { vehicles, isLoading, error } = useVehicles();
  const { location } = useCurrentLocation();
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const { filter, setFilter, selectedVehicle, setSelectedVehicle } = useRideStore();
  const { passes, isLoading: passesLoading } = usePasses();
  const [selectedHireOption, setSelectedHireOption] = useState<string | null>(null);
  const [submittingId, setSubmittingId] = useState<string | null>(null);
  const [sheetState, setSheetState] = useState<'collapsed' | 'expanded'>('collapsed');
  const sheetOffset = useRef(SHEET_SNAP_POINTS.collapsed);
  const translateY = useRef(new Animated.Value(SHEET_SNAP_POINTS.collapsed)).current;
  const listAtTop = useRef(true);

  const animateSheet = (nextState: 'collapsed' | 'expanded') => {
    const toValue = SHEET_SNAP_POINTS[nextState];
    Animated.spring(translateY, {
      toValue,
      useNativeDriver: true,
      tension: 120,
      friction: 18,
    }).start(() => {
      sheetOffset.current = toValue;
      setSheetState(nextState);
    });
  };

  useEffect(() => {
    if (selectedVehicle) {
      animateSheet('expanded');
    }
  }, [selectedVehicle]);

  const panResponder = useRef(
    PanResponder.create({
      onMoveShouldSetPanResponder: (_, gestureState) => {
        const verticalMove = Math.abs(gestureState.dy);
        const horizontalMove = Math.abs(gestureState.dx);
        if (verticalMove < 8 || verticalMove < horizontalMove) {
          return false;
        }
        if (!listAtTop.current && sheetState === 'expanded' && gestureState.dy > 0) {
          return false;
        }
        return true;
      },
      onPanResponderGrant: () => {
        translateY.stopAnimation((value?: number) => {
          if (typeof value === 'number') {
            sheetOffset.current = value;
          }
        });
      },
      onPanResponderMove: (_, gestureState) => {
        const next = Math.min(
          Math.max(SHEET_SNAP_POINTS.expanded, sheetOffset.current + gestureState.dy),
          SHEET_SNAP_POINTS.collapsed
        );
        translateY.setValue(next);
      },
      onPanResponderRelease: (_, gestureState) => {
        const shouldExpand = gestureState.dy < -60 || gestureState.vy < -0.3;
        animateSheet(shouldExpand ? 'expanded' : 'collapsed');
      },
    })
  ).current;

  const handleToggleSheet = () => {
    animateSheet(sheetState === 'collapsed' ? 'expanded' : 'collapsed');
  };

  const handleVehicleListScroll = (event: NativeSyntheticEvent<NativeScrollEvent>) => {
    listAtTop.current = event.nativeEvent.contentOffset.y <= 0;
  };

  useEffect(() => {
    if (!selectedHireOption && passes.length > 0) {
      setSelectedHireOption(passes[0].id);
    }
  }, [passes, selectedHireOption]);

  const displayVehicles = useMemo(() => {
    return vehicles.map((point) => {
      const mapped = mapPointToVehicle(point);
      if (location) {
        mapped.distance = getDistanceKm(location.latitude, location.longitude, mapped.lat, mapped.lng);
      }
      return mapped;
    });
  }, [vehicles, location]);

  const filteredVehicles = useMemo(() => {
    if (filter === 'all') return displayVehicles;
    return displayVehicles.filter((vehicle) => vehicle.type === filter);
  }, [filter, displayVehicles]);

  const handleVehiclePress = (vehicle: any) => {
    setSelectedVehicle(vehicle);
  };

  const handleReserve = async (vehicle: any) => {
    if (!selectedHireOption) {
      Alert.alert('Select hire option', 'Please pick a hire option before reserving.');
      return;
    }
    try {
      setSubmittingId(vehicle.id);
      const booking = await BookingService.create({
        scooterId: vehicle.id,
        hireOptionId: selectedHireOption,
      });
      navigation.navigate('RideDetail', { bookingId: booking.bookingId });
      setSelectedVehicle(null);
    } catch (err: any) {
      const message = err?.response?.data?.error?.message || 'Unable to reserve the vehicle. Please try again.';
      Alert.alert('Reservation failed', message);
    } finally {
      setSubmittingId(null);
    }
  };

  const initialRegion = {
    latitude: location?.latitude ?? 37.7749,
    longitude: location?.longitude ?? -122.4194,
    latitudeDelta: 0.02,
    longitudeDelta: 0.02,
  };

  const handleMapSelect = (vehicleId: string) => {
    const nextVehicle = filteredVehicles.find((v) => v.id === vehicleId);
    if (nextVehicle) {
      handleVehiclePress(nextVehicle);
    }
  };

  return (
    <View style={styles.container}>
      <FleetMap
        vehicles={filteredVehicles}
        initialRegion={initialRegion}
        selectedVehicleId={selectedVehicle?.id}
        onSelectVehicle={handleMapSelect}
      />
      <View style={styles.overlay}>
        <View style={styles.overlayCard}>
          <Text style={styles.heading}>Good day, rider</Text>
          <Text style={styles.subheading}>Find URBANOVA vehicles near {location ? 'you' : 'San Francisco'}.</Text>
        </View>
        <FlatList
          horizontal
          data={filters}
          keyExtractor={(item) => item.value}
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={{ paddingVertical: 16 }}
          renderItem={({ item }) => (
            <FilterChip label={item.label} isActive={filter === item.value} onPress={() => setFilter(item.value)} />
          )}
        />
      </View>
      <Animated.View
        style={[styles.bottomSheet, { transform: [{ translateY }] }]}
        {...panResponder.panHandlers}
      >
        <Pressable style={styles.sheetHandle} onPress={handleToggleSheet}>
          <View style={styles.sheetHandleBar} />
        </Pressable>
        <Text style={styles.sheetTitle}>
          {isLoading ? 'Loading nearby vehicles...' : `Vehicles nearby (${filteredVehicles.length})`}
        </Text>
        {isLoading && <ActivityIndicator color={colors.lime} style={styles.loader} />}
        {error && <Text style={styles.errorText}>Unable to fetch vehicles. Pull to refresh and try again.</Text>}
        {passes.length > 0 && (
          <View style={styles.planSection}>
            <Text style={styles.sectionLabel}>Hire options</Text>
            <ScrollView horizontal showsHorizontalScrollIndicator={false}>
              {passes.map((plan) => (
                <Pressable
                  key={plan.id}
                  onPress={() => setSelectedHireOption(plan.id)}
                  style={[styles.planChip, selectedHireOption === plan.id && styles.planChipActive]}
                >
                  <Text style={styles.planName}>{plan.name}</Text>
                  <Text style={styles.planPrice}>{formatCurrency(plan.price)}</Text>
                  <Text style={styles.planHint}>{Math.round(plan.durationMinutes / 60)} hours</Text>
                </Pressable>
              ))}
            </ScrollView>
          </View>
        )}
        {passesLoading && <ActivityIndicator color={colors.textSecondary} style={styles.loader} />}
        <View style={styles.vehicleList}>
          <FlatList
            data={filteredVehicles}
            keyExtractor={(item) => item.id}
            renderItem={({ item }) => (
              <VehicleCard
                vehicle={item}
                onPress={handleVehiclePress}
                onReserve={handleReserve}
                isSelected={selectedVehicle?.id === item.id}
              />
            )}
            showsVerticalScrollIndicator={true}
            contentContainerStyle={{ paddingBottom: 16 }}
            onScroll={handleVehicleListScroll}
            scrollEventThrottle={16}
            ListEmptyComponent={!isLoading ? <Text style={styles.emptyText}>No vehicles available yet.</Text> : null}
          />
        </View>
        <PrimaryButton
          label={submittingId ? 'Reserving...' : 'Reserve now'}
          onPress={() => selectedVehicle && handleReserve(selectedVehicle)}
          disabled={!selectedVehicle || !!submittingId}
        />
        <Text style={styles.caption}>Tap a vehicle to view details or reserve it instantly.</Text>
      </Animated.View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.ink,
  },
  overlay: {
    position: 'absolute',
    top: 0,
    width: '100%',
    paddingTop: 40,
    paddingHorizontal: 20,
  },
  overlayCard: {
    backgroundColor: 'rgba(131,111,255,0.92)',
    borderRadius: radii.lg,
    paddingVertical: 14,
    paddingHorizontal: 16,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.2)',
    shadowColor: '#000',
    shadowOpacity: 0.2,
    shadowRadius: 8,
    shadowOffset: { width: 0, height: 4 },
    elevation: 4,
  },
  heading: {
    fontSize: 24,
    fontWeight: '700',
    color: '#FFFFFF',
  },
  subheading: {
    color: 'rgba(255,255,255,0.9)',
    marginTop: 4,
  },
  bottomSheet: {
    position: 'absolute',
    bottom: 0,
    width: '100%',
    height: SHEET_HEIGHT,
    paddingBottom: 32,
    paddingHorizontal: 20,
    paddingTop: 12,
    borderTopLeftRadius: 32,
    borderTopRightRadius: 32,
    backgroundColor: colors.graphite,
  },
  sheetHandle: {
    alignSelf: 'center',
    paddingVertical: 12,
  },
  sheetHandleBar: {
    width: 52,
    height: 4,
    borderRadius: 2,
    backgroundColor: 'rgba(255,255,255,0.35)',
  },
  sheetTitle: {
    color: colors.textPrimary,
    fontSize: 18,
    fontWeight: '700',
  },
  caption: {
    color: colors.textMuted,
    textAlign: 'center',
    marginTop: 12,
  },
  vehicleList: {
    flex: 1,
    marginTop: 12,
    marginBottom: 12,
  },
  loader: {
    marginTop: 16,
  },
  errorText: {
    color: '#ff6b6b',
    textAlign: 'center',
    marginTop: 12,
  },
  emptyText: {
    color: colors.textMuted,
    textAlign: 'center',
    marginTop: 24,
    fontSize: 16,
  },
  planSection: {
    marginTop: 16,
    marginBottom: 12,
  },
  sectionLabel: {
    color: colors.textSecondary,
    marginBottom: 8,
  },
  planChip: {
    padding: 14,
    borderRadius: radii.md,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.12)',
    marginRight: 12,
    backgroundColor: 'rgba(255,255,255,0.04)',
  },
  planChipActive: {
    borderColor: colors.lime,
    backgroundColor: 'rgba(131,111,255,0.15)',
  },
  planName: {
    color: colors.textPrimary,
    fontWeight: '600',
  },
  planPrice: {
    color: colors.lime,
    marginTop: 4,
    fontSize: 16,
    fontWeight: '700',
  },
  planHint: {
    color: colors.textMuted,
    marginTop: 2,
    fontSize: 12,
  },
});

export default RideScreen;

const getDistanceKm = (lat1: number, lon1: number, lat2: number, lon2: number) => {
  const R = 6371; // km
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return Number((R * c).toFixed(2));
};
