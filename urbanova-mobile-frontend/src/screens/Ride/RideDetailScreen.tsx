import React, { useEffect, useMemo, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
  Image,
  Modal,
} from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { RootStackParamList } from '@models/index';
import {
  BookingService,
  ConfirmationService,
  IssueService,
  PaymentMethod,
  PaymentMethodService,
  PaymentService,
} from '@services/api';
import { usePasses } from '@hooks/usePasses';
import { useCurrentLocation } from '@hooks/useCurrentLocation';
import { useRideZones } from '@hooks/useRideZones';
import PrimaryButton from '@components/PrimaryButton';
import { colors, radii } from '@theme/index';
import { formatCurrency, formatDate } from '@utils/format';
import { validateReturnLocation } from '@utils/geo';
import { maskCard } from '@utils/security';
import ScooterFaultDiagram, { FaultPart } from '@components/ScooterFaultDiagram';

type Props = NativeStackScreenProps<RootStackParamList, 'RideDetail'>;

const statusTone: Record<string, string> = {
  PENDING_PAYMENT: '#FFB020',
  CONFIRMED: '#40C057',
  ACTIVE: '#4DABF7',
  COMPLETED: '#9CA3AF',
  CANCELLED: '#F45B69',
};

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
const PICKER_YEARS = Array.from({ length: 6 }, (_, index) => new Date().getFullYear() + index);

const getDayCount = (year: number, monthIndex: number) => new Date(year, monthIndex + 1, 0).getDate();

const roundUpToNextFiveMinutes = (date: Date) => {
  const rounded = new Date(date);
  rounded.setSeconds(0, 0);
  const remainder = rounded.getMinutes() % 5;
  if (remainder !== 0) {
    rounded.setMinutes(rounded.getMinutes() + (5 - remainder));
  }
  return rounded;
};

const formatPickerTimestamp = (value: string) => {
  if (!value) return 'Immediate start';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'Select start time';
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
};

const getApiErrorMessage = (error: any, fallback: string) => {
  const message = error?.response?.data?.error?.message || error?.message || fallback;
  const code = error?.response?.data?.error?.code;
  if (/availability|overlap|conflict|reserved|time|extend|booking/i.test(message)) {
    return `${message}\n\nThis usually means the selected time conflicts with another booking or the vehicle is not available for that window.`;
  }
  return code ? `${message} (${code})` : message;
};

const RideDetailScreen: React.FC<Props> = ({ route, navigation }) => {
  const { bookingId } = route.params;
  const queryClient = useQueryClient();
  const { passes } = usePasses();
  const { location } = useCurrentLocation();
  const { zones } = useRideZones();

  const [selectedHireOptionId, setSelectedHireOptionId] = useState('');
  const [extendOptionCode, setExtendOptionCode] = useState('');
  const [paymentMode, setPaymentMode] = useState<'SAVED_CARD' | 'ONE_TIME_CARD'>('SAVED_CARD');
  const [selectedPaymentMethodId, setSelectedPaymentMethodId] = useState('');
  const [faultTitle, setFaultTitle] = useState('Vehicle damage detected at return');
  const [faultDescription, setFaultDescription] = useState('');
  const [faultPart, setFaultPart] = useState<FaultPart>('brake');
  const [faultPhotos, setFaultPhotos] = useState<Array<{ uri: string; name?: string; type?: string }>>([]);
  const [plannedStartInput, setPlannedStartInput] = useState('');
  const [updatePickerVisible, setUpdatePickerVisible] = useState(false);
  const [pickerYear, setPickerYear] = useState(new Date().getFullYear());
  const [pickerMonth, setPickerMonth] = useState(new Date().getMonth());
  const [pickerDay, setPickerDay] = useState(new Date().getDate());
  const [pickerHour, setPickerHour] = useState(new Date().getHours());
  const [pickerMinute, setPickerMinute] = useState(roundUpToNextFiveMinutes(new Date()).getMinutes());
  const [cancelReason, setCancelReason] = useState('Plans changed');

  const bookingQuery = useQuery({
    queryKey: ['booking-detail', bookingId],
    queryFn: () => BookingService.getDetail(bookingId),
  });

  const paymentsQuery = useQuery({
    queryKey: ['booking-payments', bookingId],
    queryFn: () => PaymentService.listByBooking(bookingId),
  });

  const timelineQuery = useQuery({
    queryKey: ['booking-timeline', bookingId],
    queryFn: () => BookingService.timeline(bookingId),
  });

  const paymentMethodsQuery = useQuery({
    queryKey: ['payment-methods'],
    queryFn: PaymentMethodService.list,
  });

  const confirmationQuery = useQuery({
    queryKey: ['booking-confirmation', bookingId],
    queryFn: async () => {
      try {
        return await ConfirmationService.getForBooking(bookingId);
      } catch (error: any) {
        if (error?.response?.status === 404) {
          return null;
        }
        throw error;
      }
    },
    retry: false,
  });

  const activePaymentMethods = useMemo(
    () => (paymentMethodsQuery.data ?? []).filter((method) => method.status === 'ACTIVE'),
    [paymentMethodsQuery.data]
  );

  useEffect(() => {
    if (bookingQuery.data?.hireOptionId) {
      setSelectedHireOptionId((current) => current || bookingQuery.data?.hireOptionId || '');
    }
  }, [bookingQuery.data?.hireOptionId]);

  useEffect(() => {
    if (bookingQuery.data?.startAt) {
      setPlannedStartInput((current) => current || bookingQuery.data?.startAt || '');
    }
  }, [bookingQuery.data?.startAt]);

  useEffect(() => {
    if (!extendOptionCode && passes.length > 0) {
      setExtendOptionCode(passes[0].name);
    }
  }, [passes, extendOptionCode]);

  useEffect(() => {
    const defaultMethod = activePaymentMethods.find((method) => method.isDefault) || activePaymentMethods[0];
    if (!selectedPaymentMethodId && defaultMethod) {
      setSelectedPaymentMethodId(defaultMethod.paymentMethodId);
    }
  }, [activePaymentMethods, selectedPaymentMethodId]);

  const returnValidation = useMemo(
    () => validateReturnLocation(location?.latitude, location?.longitude, zones),
    [location?.latitude, location?.longitude, zones]
  );

  const refreshBookingRelatedData = async () => {
    await Promise.all([
      bookingQuery.refetch(),
      paymentsQuery.refetch(),
      timelineQuery.refetch(),
      confirmationQuery.refetch(),
      queryClient.invalidateQueries({ queryKey: ['bookings'] }),
    ]);
  };

  const updateBookingMutation = useMutation({
    mutationFn: () =>
      BookingService.update(bookingId, {
        hireOptionId: selectedHireOptionId || undefined,
        plannedStartAt: plannedStartInput.trim() || undefined,
      }),
    onSuccess: async () => {
      Alert.alert('Booking updated', 'Booking details were refreshed and payment status was reset as expected.');
      await refreshBookingRelatedData();
    },
    onError: (error: any) => {
      Alert.alert('Update failed', getApiErrorMessage(error, 'Unable to update booking.'));
    },
  });

  const cancelBookingMutation = useMutation({
    mutationFn: () => BookingService.cancel(bookingId, cancelReason.trim() || 'Cancelled from mobile app'),
    onSuccess: async () => {
      Alert.alert('Booking cancelled', 'Your booking status was updated.');
      await refreshBookingRelatedData();
    },
    onError: (error: any) => {
      Alert.alert('Cancel failed', getApiErrorMessage(error, 'Unable to cancel booking.'));
    },
  });

  const startBookingMutation = useMutation({
    mutationFn: () => BookingService.start(bookingId),
    onSuccess: async () => {
      Alert.alert('Ride started', 'Your booking is now active.');
      await refreshBookingRelatedData();
    },
    onError: (error: any) => {
      Alert.alert('Cannot start ride', getApiErrorMessage(error, 'Unable to start this booking.'));
    },
  });

  const endBookingMutation = useMutation({
    mutationFn: () => BookingService.end(bookingId),
    onSuccess: async () => {
      Alert.alert('Ride completed', 'Return was completed and booking status has been updated.');
      await refreshBookingRelatedData();
    },
    onError: (error: any) => {
      Alert.alert('Cannot end ride', getApiErrorMessage(error, 'Unable to end this booking.'));
    },
  });

  const extendBookingMutation = useMutation({
    mutationFn: () => BookingService.extend(bookingId, { additionalHireOptionCode: extendOptionCode }),
    onSuccess: async (result) => {
      Alert.alert('Ride extended', `New end time set. Additional charge: ${formatCurrency(Number(result.additionalCharge || 0))}`);
      await refreshBookingRelatedData();
    },
    onError: (error: any) => {
      Alert.alert('Extension failed', getApiErrorMessage(error, 'Unable to extend this booking.'));
    },
  });

  const payMutation = useMutation({
    mutationFn: () =>
      PaymentService.create(bookingId, {
        method: paymentMode,
        paymentMethodId: paymentMode === 'SAVED_CARD' ? selectedPaymentMethodId : undefined,
        simulatedOutcome: 'SUCCESS',
      }),
    onSuccess: async () => {
      Alert.alert('Payment success', 'Simulated payment completed and booking state was refreshed.');
      await refreshBookingRelatedData();
    },
    onError: (error: any) => {
      Alert.alert('Payment failed', error?.response?.data?.error?.message || 'Unable to process payment.');
    },
  });

  const resendConfirmationMutation = useMutation({
    mutationFn: () => ConfirmationService.resend(bookingId),
    onSuccess: async () => {
      Alert.alert('Confirmation sent', 'The confirmation email flow was re-triggered successfully.');
      await confirmationQuery.refetch();
    },
    onError: (error: any) => {
      Alert.alert('Resend failed', error?.response?.data?.error?.message || 'Unable to resend confirmation.');
    },
  });

  const issueMutation = useMutation({
    mutationFn: () =>
      IssueService.create({
        bookingId,
        scooterId: bookingQuery.data?.scooterId,
        issueType: 'FAULT_REPORT',
        title: faultTitle.trim(),
        description: `[${faultPart.toUpperCase()}] ${faultDescription.trim()}`,
      }),
    onSuccess: async (issue) => {
      if (faultPhotos.length > 0) {
        await IssueService.uploadPhotos(issue.issueId, faultPhotos);
      }
      setFaultDescription('');
      setFaultPhotos([]);
      Alert.alert(
        'Issue submitted',
        faultPhotos.length > 0
          ? 'Fault report and photos were submitted for follow-up.'
          : 'Return fault report was submitted for follow-up.'
      );
    },
    onError: (error: any) => {
      Alert.alert('Report failed', error?.response?.data?.error?.message || 'Unable to submit issue report.');
    },
  });

  const booking = bookingQuery.data;
  const dayOptions = Array.from({ length: getDayCount(pickerYear, pickerMonth) }, (_, index) => index + 1);
  const hourOptions = Array.from({ length: 24 }, (_, index) => index);
  const minuteOptions = Array.from({ length: 12 }, (_, index) => index * 5);

  if (bookingQuery.isLoading || !booking) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color={colors.lime} />
        <Text style={styles.loadingText}>Fetching booking details...</Text>
      </View>
    );
  }

  const isPendingPayment = booking.status === 'PENDING_PAYMENT' || booking.paymentStatus !== 'PAID';
  const canUpdateBooking = booking.status === 'PENDING_PAYMENT' || booking.status === 'CONFIRMED';
  const canStartBooking = booking.status === 'CONFIRMED';
  const canEndBooking = booking.status === 'ACTIVE';
  const canExtendBooking = booking.status === 'CONFIRMED' || booking.status === 'ACTIVE';

  const selectedPaymentMethod: PaymentMethod | undefined = activePaymentMethods.find(
    (item) => item.paymentMethodId === selectedPaymentMethodId
  );

  const openUpdateStartPicker = () => {
    const now = roundUpToNextFiveMinutes(new Date());
    const source = plannedStartInput ? new Date(plannedStartInput) : now;
    const selected = !Number.isNaN(source.getTime()) && source.getTime() >= Date.now() - 60_000 ? source : now;
    setPickerYear(selected.getFullYear());
    setPickerMonth(selected.getMonth());
    setPickerDay(selected.getDate());
    setPickerHour(selected.getHours());
    setPickerMinute(Math.round(selected.getMinutes() / 5) * 5 % 60);
    setUpdatePickerVisible(true);
  };

  const confirmUpdateStartPicker = () => {
    const selected = new Date(pickerYear, pickerMonth, pickerDay, pickerHour, pickerMinute, 0);
    if (selected.getTime() < Date.now() - 60_000) {
      Alert.alert('Invalid start time', 'Please choose the current time or a future time.');
      return;
    }
    setPlannedStartInput(selected.toISOString().slice(0, 16));
    setUpdatePickerVisible(false);
  };

  const handleEndRide = () => {
    if (!returnValidation.isValid) {
      Alert.alert(
        'Invalid return zone',
        `Current location is not valid for return (${returnValidation.blockedZoneName || 'unknown zone'}). Please move to a valid area or report an issue first.`
      );
      return;
    }
    endBookingMutation.mutate();
  };

  const handleSubmitFaultReport = () => {
    if (!faultTitle.trim()) {
      Alert.alert('Missing title', 'Please enter a fault title before submitting.');
      return;
    }
    if (!faultDescription.trim()) {
      Alert.alert('Missing details', 'Please describe the damage or abnormal condition.');
      return;
    }
    issueMutation.mutate();
  };

  const handlePickFaultPhotos = async () => {
    const permission = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (!permission.granted) {
      Alert.alert('Photo permission needed', 'Allow photo access to attach fault images.');
      return;
    }

    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ['images'],
      allowsMultipleSelection: true,
      selectionLimit: 5,
      quality: 0.82,
    });

    if (result.canceled) {
      return;
    }

    const picked = result.assets.slice(0, 5).map((asset, index) => ({
      uri: asset.uri,
      name: asset.fileName || `fault-photo-${index + 1}.jpg`,
      type: asset.mimeType || 'image/jpeg',
    }));
    setFaultPhotos(picked);
  };

  return (
    <>
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <View style={styles.hero}>
        <Text style={styles.heroLabel}>Booking reference</Text>
        <Text style={styles.heroTitle}>{booking.bookingRef}</Text>
        <View style={styles.statusRow}>
          <Text style={styles.statusLabel}>Booking</Text>
          <Text style={[styles.statusValue, { color: statusTone[booking.status] || colors.lime }]}>{booking.status}</Text>
        </View>
        <View style={styles.statusRow}>
          <Text style={styles.statusLabel}>Payment</Text>
          <Text style={styles.statusValue}>{booking.paymentStatus}</Text>
        </View>
        <Text style={styles.heroCaption}>Final price {formatCurrency(Number(booking.priceFinal || 0), 'GBP')}</Text>
      </View>

      <View style={styles.card}>
        <Text style={styles.sectionTitle}>Booking details</Text>
        <Text style={styles.valueText}>Vehicle ID: {booking.scooterId}</Text>
        <Text style={styles.valueText}>Hire option: {booking.hireOptionId}</Text>
        <Text style={styles.valueText}>Start: {booking.startAt || 'Pending'}</Text>
        <Text style={styles.valueText}>End: {booking.endAt || 'Pending'}</Text>
        <Text style={styles.valueText}>Actual start: {booking.actualStartAt || '-'}</Text>
        <Text style={styles.valueText}>Actual end: {booking.actualEndAt || '-'}</Text>
        <PrimaryButton
          label="Report this vehicle"
          onPress={() => navigation.navigate('VehicleDetail', { vehicleId: booking.scooterId, mode: 'report' })}
          style={styles.reportVehicleButton}
        />
      </View>

      {isPendingPayment ? (
        <View style={styles.card}>
          <Text style={styles.sectionTitle}>Payment</Text>
          <Text style={styles.helperText}>Complete simulated payment to move booking from pending to confirmed.</Text>
          <View style={styles.chipRow}>
            <Pressable
              style={[styles.chip, paymentMode === 'SAVED_CARD' && styles.chipActive]}
              onPress={() => setPaymentMode('SAVED_CARD')}
            >
              <Text style={styles.chipText}>Saved card</Text>
            </Pressable>
            <Pressable
              style={[styles.chip, paymentMode === 'ONE_TIME_CARD' && styles.chipActive]}
              onPress={() => setPaymentMode('ONE_TIME_CARD')}
            >
              <Text style={styles.chipText}>One-time card</Text>
            </Pressable>
          </View>

          {paymentMode === 'SAVED_CARD' ? (
            <View>
              {activePaymentMethods.length === 0 ? (
                <Text style={styles.warningText}>No saved cards found. Add one in Wallet or use one-time card.</Text>
              ) : (
                activePaymentMethods.map((method) => (
                  <Pressable
                    key={method.paymentMethodId}
                    style={[styles.methodRow, selectedPaymentMethodId === method.paymentMethodId && styles.methodRowActive]}
                    onPress={() => setSelectedPaymentMethodId(method.paymentMethodId)}
                  >
                    <Text style={styles.methodTitle}>{maskCard(method.brand, method.last4)}</Text>
                    <Text style={styles.methodSubtitle}>Exp {method.expiryMonth}/{method.expiryYear}</Text>
                  </Pressable>
                ))
              )}
            </View>
          ) : (
            <Text style={styles.helperText}>One-time card mode uses backend simulated settlement.</Text>
          )}

          <PrimaryButton
            label={payMutation.isPending ? 'Processing...' : `Pay ${formatCurrency(Number(booking.priceFinal || 0))}`}
            disabled={
              payMutation.isPending || (paymentMode === 'SAVED_CARD' && (!selectedPaymentMethod || !selectedPaymentMethodId))
            }
            onPress={() => payMutation.mutate()}
          />
        </View>
      ) : null}

      {canUpdateBooking ? (
        <View style={styles.card}>
          <Text style={styles.sectionTitle}>Update booking</Text>
          <Text style={styles.helperText}>
            You can adjust hire option or planned start time. If another booking blocks the new time, the backend will reject it.
          </Text>
          <Text style={styles.fieldLabel}>Planned start time</Text>
          <Pressable style={styles.dateButton} onPress={openUpdateStartPicker}>
            <Text style={styles.dateButtonText}>{formatPickerTimestamp(plannedStartInput)}</Text>
          </Pressable>
          <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ paddingVertical: 4 }}>
            {passes.map((plan) => (
              <Pressable
                key={plan.id}
                onPress={() => setSelectedHireOptionId(plan.id)}
                style={[styles.optionCard, selectedHireOptionId === plan.id && styles.optionCardActive]}
              >
                <Text style={styles.optionName}>{plan.name}</Text>
                <Text style={styles.optionPrice}>{formatCurrency(plan.price)}</Text>
              </Pressable>
            ))}
          </ScrollView>
          <PrimaryButton
            label={updateBookingMutation.isPending ? 'Updating...' : 'Update booking'}
            disabled={updateBookingMutation.isPending}
            onPress={() => updateBookingMutation.mutate()}
          />
          <Text style={styles.fieldLabel}>Cancel reason</Text>
          <TextInput
            style={styles.input}
            placeholder="Reason"
            placeholderTextColor={colors.textMuted}
            value={cancelReason}
            onChangeText={setCancelReason}
          />
          <PrimaryButton
            label={cancelBookingMutation.isPending ? 'Cancelling...' : 'Cancel booking'}
            disabled={cancelBookingMutation.isPending}
            onPress={() =>
              Alert.alert('Cancel booking', 'Are you sure you want to cancel this booking?', [
                { text: 'Keep booking', style: 'cancel' },
                { text: 'Cancel booking', style: 'destructive', onPress: () => cancelBookingMutation.mutate() },
              ])
            }
            style={{ marginTop: 10 }}
          />
        </View>
      ) : null}

      {canStartBooking ? (
        <View style={styles.card}>
          <Text style={styles.sectionTitle}>Start ride</Text>
          <PrimaryButton
            label={startBookingMutation.isPending ? 'Starting...' : 'Start booking'}
            disabled={startBookingMutation.isPending}
            onPress={() => startBookingMutation.mutate()}
          />
        </View>
      ) : null}

      {canExtendBooking ? (
        <View style={styles.card}>
          <Text style={styles.sectionTitle}>Extend ride</Text>
          <Text style={styles.helperText}>Choose an additional hire option code.</Text>
          <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ paddingVertical: 4 }}>
            {passes.map((plan) => (
              <Pressable
                key={`${plan.id}-extend`}
                onPress={() => setExtendOptionCode(plan.name)}
                style={[styles.optionCard, extendOptionCode === plan.name && styles.optionCardActive]}
              >
                <Text style={styles.optionName}>{plan.name}</Text>
                <Text style={styles.optionPrice}>{formatCurrency(plan.price)}</Text>
              </Pressable>
            ))}
          </ScrollView>
          <PrimaryButton
            label={extendBookingMutation.isPending ? 'Extending...' : 'Extend booking'}
            disabled={!extendOptionCode || extendBookingMutation.isPending}
            onPress={() => extendBookingMutation.mutate()}
          />
        </View>
      ) : null}

      {canEndBooking ? (
        <View style={styles.card}>
          <Text style={styles.sectionTitle}>Return & end ride</Text>
          <Text style={styles.valueText}>Location: {location ? `${location.latitude.toFixed(5)}, ${location.longitude.toFixed(5)}` : 'Unavailable'}</Text>
          <Text style={[styles.valueText, returnValidation.isValid ? styles.validText : styles.warningText]}>
            Return zone check: {returnValidation.isValid ? 'Valid' : `Blocked (${returnValidation.blockedZoneName || 'unknown'})`}
          </Text>
          <PrimaryButton
            label={endBookingMutation.isPending ? 'Completing...' : 'End booking'}
            disabled={endBookingMutation.isPending}
            onPress={handleEndRide}
          />
        </View>
      ) : null}

      {(canEndBooking || booking.status === 'COMPLETED') ? (
        <View style={styles.card}>
          <Text style={styles.sectionTitle}>Return fault report</Text>
          <Text style={styles.helperText}>Tap the scooter part that has a problem, then describe the fault.</Text>
          <ScooterFaultDiagram selectedPart={faultPart} onSelectPart={setFaultPart} />
          <TextInput
            style={styles.input}
            placeholder="Issue title"
            placeholderTextColor={colors.textMuted}
            value={faultTitle}
            onChangeText={setFaultTitle}
          />
          <TextInput
            style={[styles.input, styles.textarea]}
            multiline
            placeholder="Describe damage, parking issue, or abnormal behavior"
            placeholderTextColor={colors.textMuted}
            value={faultDescription}
            onChangeText={setFaultDescription}
          />
          <Text style={styles.helperText}>Priority is mapped automatically by the backend for fault reports.</Text>
          <View style={styles.photoHeader}>
            <Text style={styles.fieldLabel}>Fault photos</Text>
            <Pressable onPress={handlePickFaultPhotos}>
              <Text style={styles.linkText}>Choose photos</Text>
            </Pressable>
          </View>
          {faultPhotos.length > 0 ? (
            <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.photoStrip}>
              {faultPhotos.map((photo) => (
                <View key={photo.uri} style={styles.photoThumbWrap}>
                  <Image source={{ uri: photo.uri }} style={styles.photoThumb} />
                  <Pressable
                    style={styles.removePhoto}
                    onPress={() => setFaultPhotos((current) => current.filter((item) => item.uri !== photo.uri))}
                  >
                    <Text style={styles.removePhotoText}>x</Text>
                  </Pressable>
                </View>
              ))}
            </ScrollView>
          ) : (
            <Text style={styles.helperText}>Optional: attach up to 5 JPEG/PNG/WEBP images.</Text>
          )}
          <PrimaryButton
            label={issueMutation.isPending ? 'Submitting...' : 'Submit fault report'}
            disabled={issueMutation.isPending}
            onPress={handleSubmitFaultReport}
          />
        </View>
      ) : null}

      <View style={styles.card}>
        <Text style={styles.sectionTitle}>Confirmation & email flow</Text>
        {confirmationQuery.isLoading ? (
          <ActivityIndicator color={colors.lime} />
        ) : confirmationQuery.data ? (
          <>
            <Text style={styles.valueText}>Recipient: {confirmationQuery.data.recipientEmail || 'N/A'}</Text>
            <Text style={styles.valueText}>Status: {confirmationQuery.data.status}</Text>
            <Text style={styles.valueText}>Channel: {confirmationQuery.data.channel}</Text>
            <Text style={styles.valueText}>Resend count: {confirmationQuery.data.resendCount}</Text>
            <Text style={styles.valueText}>Updated: {formatDate(confirmationQuery.data.updatedAt)}</Text>
          </>
        ) : (
          <Text style={styles.helperText}>Confirmation record not available yet (usually created after successful payment).</Text>
        )}
        <PrimaryButton
          label={resendConfirmationMutation.isPending ? 'Sending...' : 'Resend confirmation'}
          disabled={resendConfirmationMutation.isPending}
          onPress={() => resendConfirmationMutation.mutate()}
          style={{ marginTop: 10 }}
        />
      </View>

      <View style={styles.card}>
        <Text style={styles.sectionTitle}>Payments</Text>
        {paymentsQuery.isLoading ? <ActivityIndicator color={colors.lime} /> : null}
        {(paymentsQuery.data ?? []).length === 0 && !paymentsQuery.isLoading ? (
          <Text style={styles.helperText}>No payment records yet.</Text>
        ) : null}
        {(paymentsQuery.data ?? []).map((payment) => (
          <View key={payment.paymentId} style={styles.listRow}>
            <Text style={styles.listTitle}>{payment.paymentId}</Text>
            <Text style={styles.listSubTitle}>{payment.status} | {formatCurrency(Number(payment.amount || 0))}</Text>
          </View>
        ))}
      </View>

      <View style={styles.card}>
        <Text style={styles.sectionTitle}>Booking timeline</Text>
        {timelineQuery.isLoading ? <ActivityIndicator color={colors.lime} /> : null}
        {(timelineQuery.data ?? []).length === 0 && !timelineQuery.isLoading ? (
          <Text style={styles.helperText}>No timeline events yet.</Text>
        ) : null}
        {(timelineQuery.data ?? []).map((event) => (
          <View key={event.eventId} style={styles.listRow}>
            <Text style={styles.listTitle}>{event.eventType}</Text>
            <Text style={styles.listSubTitle}>{event.details || 'No details'} | {formatDate(event.createdAt)}</Text>
          </View>
        ))}
      </View>

      <View style={styles.card}>
        <Text style={styles.sectionTitle}>Route trace</Text>
        <Text style={styles.helperText}>
          Detailed route playback is not available yet because the current backend does not expose per-ride GPS trace points.
        </Text>
        <Text style={styles.valueText}>Vehicle: {booking.scooterId}</Text>
        <Text style={styles.valueText}>Time window: {booking.startAt || 'Pending'} {' -> '} {booking.endAt || 'Pending'}</Text>
      </View>

      {booking.cancelReason ? (
        <View style={styles.card}>
          <Text style={styles.sectionTitle}>Cancellation reason</Text>
          <Text style={styles.valueText}>{booking.cancelReason}</Text>
        </View>
      ) : null}
    </ScrollView>
      <Modal visible={updatePickerVisible} transparent animationType="fade" onRequestClose={() => setUpdatePickerVisible(false)}>
        <View style={styles.modalBackdrop}>
          <View style={styles.pickerCard}>
            <Text style={styles.sectionTitle}>Select start time</Text>
            <View style={styles.pickerHeader}>
              <Text style={styles.pickerHeaderText}>Year</Text>
              <Text style={styles.pickerHeaderText}>Month</Text>
              <Text style={styles.pickerHeaderText}>Day</Text>
              <Text style={styles.pickerHeaderText}>Hour</Text>
              <Text style={styles.pickerHeaderText}>Min</Text>
            </View>
            <View style={styles.pickerRow}>
              <ScrollView style={styles.pickerColumn} showsVerticalScrollIndicator={false}>
                {PICKER_YEARS.map((year) => (
                  <PickerItem key={year} label={`${year}`} selected={pickerYear === year} onPress={() => setPickerYear(year)} />
                ))}
              </ScrollView>
              <ScrollView style={styles.pickerColumn} showsVerticalScrollIndicator={false}>
                {MONTHS.map((month, index) => (
                  <PickerItem key={month} label={month} selected={pickerMonth === index} onPress={() => setPickerMonth(index)} />
                ))}
              </ScrollView>
              <ScrollView style={styles.pickerColumn} showsVerticalScrollIndicator={false}>
                {dayOptions.map((day) => (
                  <PickerItem key={day} label={`${day}`} selected={pickerDay === day} onPress={() => setPickerDay(day)} />
                ))}
              </ScrollView>
              <ScrollView style={styles.pickerColumn} showsVerticalScrollIndicator={false}>
                {hourOptions.map((hour) => (
                  <PickerItem key={hour} label={`${String(hour).padStart(2, '0')}`} selected={pickerHour === hour} onPress={() => setPickerHour(hour)} />
                ))}
              </ScrollView>
              <ScrollView style={styles.pickerColumn} showsVerticalScrollIndicator={false}>
                {minuteOptions.map((minute) => (
                  <PickerItem
                    key={minute}
                    label={`${String(minute).padStart(2, '0')}`}
                    selected={pickerMinute === minute}
                    onPress={() => setPickerMinute(minute)}
                  />
                ))}
              </ScrollView>
            </View>
            <View style={styles.pickerActions}>
              <PrimaryButton label="Close" onPress={() => setUpdatePickerVisible(false)} style={styles.pickerButton} />
              <PrimaryButton label="Confirm" onPress={confirmUpdateStartPicker} style={styles.pickerButton} />
            </View>
          </View>
        </View>
      </Modal>
    </>
  );
};

const PickerItem = ({ label, selected, onPress }: { label: string; selected: boolean; onPress: () => void }) => (
  <Pressable style={[styles.pickerItem, selected && styles.pickerItemSelected]} onPress={onPress}>
    <Text style={[styles.pickerItemLabel, selected && styles.pickerItemLabelSelected]}>{label}</Text>
  </Pressable>
);

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.ink,
  },
  content: {
    padding: 20,
    paddingTop: 96,
    paddingBottom: 36,
  },
  centered: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.ink,
  },
  loadingText: {
    color: colors.textSecondary,
    marginTop: 12,
  },
  hero: {
    backgroundColor: colors.card,
    borderRadius: radii.lg,
    padding: 20,
    marginBottom: 16,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.1)',
  },
  heroLabel: {
    color: colors.textSecondary,
    textTransform: 'uppercase',
    fontSize: 12,
  },
  heroTitle: {
    color: colors.textPrimary,
    fontSize: 22,
    fontWeight: '700',
    marginTop: 6,
  },
  heroCaption: {
    color: colors.textMuted,
    marginTop: 8,
  },
  statusRow: {
    marginTop: 10,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  statusLabel: {
    color: colors.textSecondary,
  },
  statusValue: {
    color: colors.textPrimary,
    fontWeight: '700',
  },
  card: {
    backgroundColor: colors.card,
    borderRadius: radii.lg,
    padding: 16,
    marginBottom: 16,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.08)',
  },
  sectionTitle: {
    color: colors.textPrimary,
    fontSize: 17,
    fontWeight: '700',
    marginBottom: 8,
  },
  helperText: {
    color: colors.textSecondary,
    marginBottom: 10,
  },
  fieldLabel: {
    color: colors.textSecondary,
    fontSize: 12,
    marginTop: 8,
    marginBottom: 6,
    textTransform: 'uppercase',
  },
  photoHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 8,
  },
  photoStrip: {
    marginBottom: 12,
  },
  photoThumbWrap: {
    width: 74,
    height: 74,
    marginRight: 10,
  },
  photoThumb: {
    width: 74,
    height: 74,
    borderRadius: radii.md,
    backgroundColor: 'rgba(255,255,255,0.08)',
  },
  removePhoto: {
    position: 'absolute',
    top: -6,
    right: -6,
    width: 22,
    height: 22,
    borderRadius: 11,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.danger,
  },
  removePhotoText: {
    color: '#FFFFFF',
    fontWeight: '800',
    lineHeight: 18,
  },
  linkText: {
    color: colors.lime,
    fontWeight: '700',
  },
  valueText: {
    color: colors.textPrimary,
    marginBottom: 6,
  },
  reportVehicleButton: {
    marginTop: 10,
  },
  warningText: {
    color: '#FFC857',
  },
  validText: {
    color: '#3CE680',
  },
  chipRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginBottom: 10,
  },
  chip: {
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.16)',
    borderRadius: radii.md,
    paddingHorizontal: 12,
    paddingVertical: 8,
    marginRight: 8,
    marginBottom: 8,
  },
  chipActive: {
    borderColor: colors.lime,
    backgroundColor: 'rgba(131,111,255,0.14)',
  },
  chipText: {
    color: colors.textPrimary,
    fontWeight: '600',
    fontSize: 12,
  },
  methodRow: {
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.12)',
    borderRadius: radii.md,
    padding: 12,
    marginBottom: 8,
  },
  methodRowActive: {
    borderColor: colors.lime,
    backgroundColor: 'rgba(131,111,255,0.12)',
  },
  methodTitle: {
    color: colors.textPrimary,
    fontWeight: '600',
  },
  methodSubtitle: {
    color: colors.textSecondary,
    marginTop: 2,
    fontSize: 12,
  },
  optionCard: {
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.12)',
    borderRadius: radii.md,
    paddingVertical: 10,
    paddingHorizontal: 12,
    marginRight: 10,
    minWidth: 108,
  },
  optionCardActive: {
    borderColor: colors.lime,
    backgroundColor: 'rgba(131,111,255,0.12)',
  },
  optionName: {
    color: colors.textPrimary,
    fontWeight: '600',
  },
  optionPrice: {
    color: colors.textSecondary,
    marginTop: 4,
    fontSize: 12,
  },
  input: {
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.14)',
    borderRadius: radii.md,
    paddingHorizontal: 12,
    paddingVertical: 10,
    color: colors.textPrimary,
    marginBottom: 10,
  },
  dateButton: {
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.14)',
    borderRadius: radii.md,
    paddingHorizontal: 12,
    paddingVertical: 12,
    marginBottom: 10,
  },
  dateButtonText: {
    color: colors.textPrimary,
    fontWeight: '600',
  },
  textarea: {
    minHeight: 96,
    textAlignVertical: 'top',
  },
  modalBackdrop: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.65)',
    justifyContent: 'center',
    paddingHorizontal: 20,
  },
  pickerCard: {
    backgroundColor: colors.graphite,
    borderRadius: radii.lg,
    padding: 16,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.12)',
  },
  pickerHeader: {
    flexDirection: 'row',
    marginTop: 10,
    marginBottom: 8,
  },
  pickerHeaderText: {
    flex: 1,
    color: colors.textSecondary,
    fontWeight: '700',
    textAlign: 'center',
    fontSize: 12,
  },
  pickerRow: {
    flexDirection: 'row',
    height: 220,
  },
  pickerColumn: {
    flex: 1,
    marginHorizontal: 3,
  },
  pickerItem: {
    height: 40,
    borderRadius: radii.sm,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 8,
    backgroundColor: 'rgba(255,255,255,0.05)',
  },
  pickerItemSelected: {
    backgroundColor: 'rgba(131,111,255,0.22)',
    borderWidth: 1,
    borderColor: colors.lime,
  },
  pickerItemLabel: {
    color: colors.textSecondary,
    fontWeight: '600',
    fontSize: 12,
  },
  pickerItemLabelSelected: {
    color: colors.textPrimary,
  },
  pickerActions: {
    flexDirection: 'row',
    marginTop: 12,
    gap: 10,
  },
  pickerButton: {
    flex: 1,
  },
  listRow: {
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: 'rgba(255,255,255,0.1)',
    paddingTop: 10,
    marginTop: 10,
  },
  listTitle: {
    color: colors.textPrimary,
    fontWeight: '600',
  },
  listSubTitle: {
    color: colors.textSecondary,
    fontSize: 12,
    marginTop: 2,
  },
});

export default RideDetailScreen;
