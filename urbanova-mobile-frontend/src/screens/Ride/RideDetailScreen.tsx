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
} from 'react-native';
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

type Props = NativeStackScreenProps<RootStackParamList, 'RideDetail'>;

const statusTone: Record<string, string> = {
  PENDING_PAYMENT: '#FFB020',
  CONFIRMED: '#40C057',
  ACTIVE: '#4DABF7',
  COMPLETED: '#9CA3AF',
  CANCELLED: '#F45B69',
};

const RideDetailScreen: React.FC<Props> = ({ route }) => {
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
  const [faultPriority, setFaultPriority] = useState<'LOW' | 'HIGH' | 'CRITICAL'>('HIGH');

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
    mutationFn: (hireOptionId: string) => BookingService.update(bookingId, { hireOptionId }),
    onSuccess: async () => {
      Alert.alert('Booking updated', 'Booking details were refreshed and payment status was reset as expected.');
      await refreshBookingRelatedData();
    },
    onError: (error: any) => {
      Alert.alert('Update failed', error?.response?.data?.error?.message || 'Unable to update booking.');
    },
  });

  const startBookingMutation = useMutation({
    mutationFn: () => BookingService.start(bookingId),
    onSuccess: async () => {
      Alert.alert('Ride started', 'Your booking is now active.');
      await refreshBookingRelatedData();
    },
    onError: (error: any) => {
      Alert.alert('Cannot start ride', error?.response?.data?.error?.message || 'Unable to start this booking.');
    },
  });

  const endBookingMutation = useMutation({
    mutationFn: () => BookingService.end(bookingId),
    onSuccess: async () => {
      Alert.alert('Ride completed', 'Return was completed and booking status has been updated.');
      await refreshBookingRelatedData();
    },
    onError: (error: any) => {
      Alert.alert('Cannot end ride', error?.response?.data?.error?.message || 'Unable to end this booking.');
    },
  });

  const extendBookingMutation = useMutation({
    mutationFn: () => BookingService.extend(bookingId, { additionalHireOptionCode: extendOptionCode }),
    onSuccess: async (result) => {
      Alert.alert('Ride extended', `New end time set. Additional charge: ${formatCurrency(Number(result.additionalCharge || 0))}`);
      await refreshBookingRelatedData();
    },
    onError: (error: any) => {
      Alert.alert('Extension failed', error?.response?.data?.error?.message || 'Unable to extend this booking.');
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
        title: faultTitle.trim(),
        description: faultDescription.trim(),
        priority: faultPriority,
      }),
    onSuccess: () => {
      setFaultDescription('');
      Alert.alert('Issue submitted', 'Return fault report was submitted for follow-up.');
    },
    onError: (error: any) => {
      Alert.alert('Report failed', error?.response?.data?.error?.message || 'Unable to submit issue report.');
    },
  });

  const booking = bookingQuery.data;

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

  return (
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
          <Text style={styles.helperText}>You can adjust booking hire option before ride completion.</Text>
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
            disabled={!selectedHireOptionId || selectedHireOptionId === booking.hireOptionId || updateBookingMutation.isPending}
            onPress={() => updateBookingMutation.mutate(selectedHireOptionId)}
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
          <Text style={styles.helperText}>Report damage or abnormal conditions during/after return.</Text>
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
          <View style={styles.chipRow}>
            {(['LOW', 'HIGH', 'CRITICAL'] as const).map((priority) => (
              <Pressable
                key={priority}
                style={[styles.chip, faultPriority === priority && styles.chipActive]}
                onPress={() => setFaultPriority(priority)}
              >
                <Text style={styles.chipText}>{priority}</Text>
              </Pressable>
            ))}
          </View>
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
  );
};

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
  valueText: {
    color: colors.textPrimary,
    marginBottom: 6,
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
  textarea: {
    minHeight: 96,
    textAlignVertical: 'top',
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
