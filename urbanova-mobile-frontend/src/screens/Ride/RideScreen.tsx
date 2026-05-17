import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  Animated,
  Dimensions,
  FlatList,
  Modal,
  PanResponder,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
  NativeSyntheticEvent,
  NativeScrollEvent,
} from 'react-native';
import { CameraView, useCameraPermissions, BarcodeScanningResult } from 'expo-camera';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';

import { useVehicles, mapPointToVehicle } from '@hooks/useVehicles';
import { useCurrentLocation } from '@hooks/useCurrentLocation';
import { useRideStore, VehicleFilter } from '@store/useRideStore';
import { colors, radii } from '@theme/index';
import FilterChip from '@components/FilterChip';
import VehicleCard from '@components/VehicleCard';
import PrimaryButton from '@components/PrimaryButton';
import { BookingService, HireOptionService, PriceQuote, ScooterService, UserLocationService } from '@services/api';
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
const NEARBY_RADIUS_KM = 5;
const SHEET_HEIGHT = SCREEN_HEIGHT * 0.85;
const SHEET_SNAP_POINTS = {
  expanded: SCREEN_HEIGHT * 0.12,
  collapsed: SCREEN_HEIGHT * 0.55,
};
const SHEET_BOTTOM_VISIBLE_PADDING = SHEET_SNAP_POINTS.expanded + 12;

const RideScreen = () => {
  const { location } = useCurrentLocation();
  const { vehicles, isLoading, error } = useVehicles({
    lat: location?.latitude,
    lng: location?.longitude,
    radiusKm: NEARBY_RADIUS_KM,
  });
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const { filter, setFilter, selectedVehicle, setSelectedVehicle, plannedStartAt } = useRideStore();
  const { passes, isLoading: passesLoading } = usePasses();
  const [selectedHireOption, setSelectedHireOption] = useState<string | null>(null);
  const [selectedQuote, setSelectedQuote] = useState<PriceQuote | null>(null);
  const [submittingId, setSubmittingId] = useState<string | null>(null);
  const [qrModalVisible, setQrModalVisible] = useState(false);
  const [qrLoading, setQrLoading] = useState(false);
  const [qrCameraEnabled, setQrCameraEnabled] = useState(false);
  const [qrScanned, setQrScanned] = useState(false);
  const [cameraPermission, requestCameraPermission] = useCameraPermissions();
  const [sheetState, setSheetState] = useState<'collapsed' | 'expanded'>('collapsed');
  const sheetOffset = useRef(SHEET_SNAP_POINTS.collapsed);
  const translateY = useRef(new Animated.Value(SHEET_SNAP_POINTS.collapsed)).current;
  const listAtTop = useRef(true);
  const vehicleListRef = useRef<FlatList<any>>(null);
  const quoteRequestRef = useRef('');

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

  useEffect(() => {
    if (!location) {
      return;
    }
    UserLocationService.updateLocation({
      lat: location.latitude,
      lng: location.longitude,
      source: 'CLIENT_GPS',
    }).catch(() => {
      // Location sync is best-effort; ride discovery still works without it.
    });
  }, [location]);

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

  const handleVehicleDetails = (vehicle: any) => {
    navigation.navigate('VehicleDetail', { vehicleId: vehicle.id });
  };

  useEffect(() => {
    const selectedPlan = passes.find((plan) => plan.id === selectedHireOption);
    if (!selectedVehicle || !selectedPlan?.code) {
      quoteRequestRef.current = '';
      setSelectedQuote(null);
      return;
    }

    const quoteKey = `${selectedVehicle.id}:${selectedPlan.code}`;
    if (quoteRequestRef.current === quoteKey) {
      return;
    }
    quoteRequestRef.current = quoteKey;
    let isMounted = true;
    HireOptionService.quote({
      scooterId: selectedVehicle.id,
      hireOptionCode: selectedPlan.code,
    })
      .then((quote) => {
        if (isMounted) {
          setSelectedQuote(quote);
        }
      })
      .catch(() => {
        if (isMounted) {
          setSelectedQuote(null);
        }
      })

    return () => {
      isMounted = false;
    };
  }, [passes, selectedHireOption, selectedVehicle?.id]);

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
        plannedStartAt: plannedStartAt.trim() || undefined,
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

  const handleResolveQr = async (payload: string) => {
    const payloadToResolve = payload.trim();
    if (!payloadToResolve) {
      Alert.alert('QR payload required', 'Scan a valid URBANOVA scooter QR code.');
      return;
    }
    setQrLoading(true);
    try {
      const result = await ScooterService.resolveQrPayload(payloadToResolve);
      const scooterId =
        result.scooter?.scooterId ||
        result.scooterId ||
        (typeof result.scooter === 'object' ? String((result.scooter as any)?.scooterId || '') : '');
      const matched = displayVehicles.find((vehicle) => vehicle.id === scooterId);
      if (matched) {
        setSelectedVehicle(matched);
      }
      setQrModalVisible(false);
      if (result.canBook === false) {
        Alert.alert('Vehicle found but unavailable', result.reason || 'This vehicle cannot be booked right now.');
      } else {
        Alert.alert('Vehicle selected', scooterId ? `Scooter ${scooterId} is ready to reserve.` : 'QR resolved successfully.');
      }
    } catch (err: any) {
      Alert.alert('QR resolve failed', err?.response?.data?.error?.message || 'Unable to resolve this QR payload.');
    } finally {
      setQrLoading(false);
    }
  };

  const handleStartCameraScan = async () => {
    const permission = cameraPermission?.granted ? cameraPermission : await requestCameraPermission();
    if (!permission.granted) {
      Alert.alert('Camera permission needed', 'Allow camera access to scan scooter QR codes.');
      return;
    }
    setQrScanned(false);
    setQrCameraEnabled(true);
  };

  const handleBarcodeScanned = (result: BarcodeScanningResult) => {
    if (qrScanned) {
      return;
    }
    setQrScanned(true);
    setQrCameraEnabled(false);
    handleResolveQr(result.data);
  };

  const mapCenterVehicle = displayVehicles[0];
  const initialRegion = {
    latitude: mapCenterVehicle?.lat ?? location?.latitude ?? 30.764633,
    longitude: mapCenterVehicle?.lng ?? location?.longitude ?? 103.983826,
    latitudeDelta: 0.02,
    longitudeDelta: 0.02,
  };

  const handleMapSelect = (vehicleId: string) => {
    const nextVehicle = filteredVehicles.find((v) => v.id === vehicleId);
    if (nextVehicle) {
      setSelectedVehicle(nextVehicle);
      animateSheet('expanded');
      const vehicleIndex = filteredVehicles.findIndex((vehicle) => vehicle.id === nextVehicle.id);
      if (vehicleIndex >= 0) {
        requestAnimationFrame(() => {
          vehicleListRef.current?.scrollToIndex({ index: vehicleIndex, animated: true, viewPosition: 0.08 });
        });
      }
    }
  };

  return (
    <View style={styles.container}>
      <FleetMap
        vehicles={filteredVehicles}
        initialRegion={initialRegion}
        userLocation={location}
        selectedVehicleId={selectedVehicle?.id}
        onSelectVehicle={handleMapSelect}
      />
      <View style={styles.overlay}>
        <View style={styles.topControls}>
          <View style={styles.brandRow}>
            <Text style={styles.brandName}>URBANOVA</Text>
            <Pressable style={styles.scanButton} onPress={() => setQrModalVisible(true)}>
              <Text style={styles.scanButtonText}>Scan QR</Text>
            </Pressable>
          </View>
          <View style={styles.filterBar}>
            <FlatList
              horizontal
              data={filters}
              keyExtractor={(item) => item.value}
              showsHorizontalScrollIndicator={false}
              contentContainerStyle={styles.filterContent}
              renderItem={({ item }) => (
                <FilterChip label={item.label} isActive={filter === item.value} onPress={() => setFilter(item.value)} />
              )}
            />
          </View>
        </View>
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
                  {selectedVehicle && selectedHireOption === plan.id && selectedQuote?.appliedDiscounts?.length ? (
                    <View style={styles.discountPriceRow}>
                      <Text style={styles.planOriginalPrice}>{formatCurrency(Number(selectedQuote.basePrice || plan.price))}</Text>
                      <Text style={styles.planPrice}>{formatCurrency(Number(selectedQuote.finalPrice || plan.price))}</Text>
                    </View>
                  ) : (
                    <Text style={styles.planPrice}>{formatCurrency(plan.price)}</Text>
                  )}
                  <Text style={styles.planHint}>{Math.round(plan.durationMinutes / 60)} hours</Text>
                </Pressable>
              ))}
            </ScrollView>
          </View>
        )}
        {passesLoading && <ActivityIndicator color={colors.textSecondary} style={styles.loader} />}
        <View style={styles.vehicleList}>
          <FlatList
            ref={vehicleListRef}
            data={filteredVehicles}
            keyExtractor={(item) => item.id}
            renderItem={({ item }) => (
              <VehicleCard
                vehicle={item}
                onPress={handleVehiclePress}
                onReserve={handleReserve}
                onDetails={handleVehicleDetails}
                isSelected={selectedVehicle?.id === item.id}
                showReserveButton={false}
              />
            )}
            showsVerticalScrollIndicator={true}
            contentContainerStyle={{ paddingBottom: 16 }}
            onScroll={handleVehicleListScroll}
            scrollEventThrottle={16}
            onScrollToIndexFailed={({ index }) => {
              setTimeout(() => {
                vehicleListRef.current?.scrollToIndex({ index, animated: true, viewPosition: 0.08 });
              }, 250);
            }}
            ListEmptyComponent={!isLoading ? <Text style={styles.emptyText}>No vehicles available yet.</Text> : null}
          />
        </View>
        <PrimaryButton
          label={submittingId ? 'Reserving...' : 'Reserve now'}
          onPress={() => selectedVehicle && handleReserve(selectedVehicle)}
          disabled={!selectedVehicle || !!submittingId}
        />
      </Animated.View>

      <Modal visible={qrModalVisible} transparent animationType="fade" onRequestClose={() => setQrModalVisible(false)}>
        <View style={styles.modalBackdrop}>
          <View style={styles.modalCard}>
            <Text style={styles.modalTitle}>Scan scooter QR</Text>
            <Text style={styles.modalHint}>
              Scan the scooter QR code. URBANOVA will ask the backend whether the vehicle can be booked.
            </Text>
            {qrCameraEnabled ? (
              <View style={styles.cameraBox}>
                <CameraView
                  style={styles.cameraView}
                  facing="back"
                  barcodeScannerSettings={{ barcodeTypes: ['qr'] }}
                  onBarcodeScanned={handleBarcodeScanned}
                />
                <View style={styles.scanFrame} />
              </View>
            ) : null}
            <PrimaryButton
              label={qrCameraEnabled ? 'Scanning...' : 'Open camera scanner'}
              onPress={handleStartCameraScan}
              disabled={qrCameraEnabled}
              style={{ marginBottom: 10 }}
            />
            <PrimaryButton label="Close" onPress={() => setQrModalVisible(false)} style={{ marginTop: 10 }} />
          </View>
        </View>
      </Modal>
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
    paddingTop: 58,
    paddingHorizontal: 16,
  },
  topControls: {
    gap: 10,
  },
  brandRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    borderRadius: radii.lg,
    backgroundColor: 'rgba(10,15,18,0.72)',
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.12)',
    paddingHorizontal: 14,
    paddingVertical: 10,
  },
  brandName: {
    color: colors.lime,
    fontSize: 20,
    fontWeight: '900',
    letterSpacing: 1,
  },
  filterBar: {
    borderRadius: radii.lg,
    backgroundColor: 'rgba(10,15,18,0.76)',
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.14)',
    paddingHorizontal: 10,
    paddingVertical: 8,
  },
  filterContent: {
    paddingRight: 8,
  },
  scanButton: {
    borderRadius: radii.md,
    backgroundColor: 'rgba(131,111,255,0.92)',
    paddingHorizontal: 14,
    paddingVertical: 10,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.2)',
  },
  scanButtonText: {
    color: '#FFFFFF',
    fontWeight: '700',
  },
  bottomSheet: {
    position: 'absolute',
    bottom: 0,
    width: '100%',
    height: SHEET_HEIGHT,
    paddingBottom: SHEET_BOTTOM_VISIBLE_PADDING,
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
  discountPriceRow: {
    marginTop: 4,
  },
  planOriginalPrice: {
    color: colors.textMuted,
    textDecorationLine: 'line-through',
    fontSize: 12,
  },
  modalBackdrop: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.65)',
    justifyContent: 'center',
    paddingHorizontal: 24,
  },
  modalCard: {
    backgroundColor: colors.graphite,
    borderRadius: radii.lg,
    padding: 20,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.12)',
  },
  modalTitle: {
    color: colors.textPrimary,
    fontSize: 20,
    fontWeight: '800',
  },
  modalHint: {
    color: colors.textSecondary,
    marginTop: 8,
    marginBottom: 14,
  },
  cameraBox: {
    height: 260,
    borderRadius: radii.lg,
    overflow: 'hidden',
    marginBottom: 12,
    borderWidth: 1,
    borderColor: 'rgba(131,111,255,0.45)',
  },
  cameraView: {
    flex: 1,
  },
  scanFrame: {
    position: 'absolute',
    left: '18%',
    right: '18%',
    top: '22%',
    bottom: '22%',
    borderWidth: 2,
    borderColor: colors.lime,
    borderRadius: radii.lg,
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
