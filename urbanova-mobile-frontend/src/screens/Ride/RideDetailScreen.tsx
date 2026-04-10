import React from 'react';
import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useQuery } from '@tanstack/react-query';
import { BookingService } from '@services/api';
import { RootStackParamList } from '@models/index';
import PrimaryButton from '@components/PrimaryButton';
import { colors, radii } from '@theme/index';
import { formatCurrency } from '@utils/format';

type Props = NativeStackScreenProps<RootStackParamList, 'RideDetail'>;

const RideDetailScreen: React.FC<Props> = ({ route }) => {
  const { bookingId } = route.params;

  const { data, isLoading, error, refetch, isRefetching } = useQuery({
    queryKey: ['booking-detail', bookingId],
    queryFn: () => BookingService.getDetail(bookingId),
  });

  if (isLoading || isRefetching) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color={colors.lime} />
        <Text style={styles.loadingText}>Fetching booking details...</Text>
      </View>
    );
  }

  if (error || !data) {
    return (
      <View style={styles.centered}>
        <Text style={styles.errorText}>Unable to load booking. Please try again.</Text>
        <PrimaryButton label="Reload" onPress={() => refetch()} style={{ marginTop: 16 }} />
      </View>
    );
  }

  const infoRows = [
    { label: 'Booking reference', value: data.bookingRef },
    { label: 'Vehicle ID', value: data.scooterId },
    { label: 'Hire option', value: data.hireOptionId },
    { label: 'Status', value: data.status },
    { label: 'Payment status', value: data.paymentStatus },
    { label: 'Start time', value: data.startAt || 'Pending' },
    { label: 'End time', value: data.endAt || 'In ride' },
    { label: 'Created at', value: data.createdAt },
  ];

  return (
    <View style={styles.container}>
      <View style={styles.hero}>
        <Text style={styles.heroLabel}>Current booking</Text>
        <Text style={styles.heroTitle}>{formatCurrency(data.priceFinal, 'GBP')}</Text>
        <Text style={styles.heroCaption}>
          Base {formatCurrency(data.priceBase, 'GBP')} ? Discount {formatCurrency(data.priceDiscount, 'GBP')}
        </Text>
      </View>
      <View style={styles.card}>
        {infoRows.map((row) => (
          <View key={row.label} style={styles.row}>
            <Text style={styles.rowLabel}>{row.label}</Text>
            <Text style={styles.rowValue}>{row.value}</Text>
          </View>
        ))}
      </View>
      {data.cancelReason ? (
        <View style={styles.card}>
          <Text style={styles.rowLabel}>Cancellation reason</Text>
          <Text style={styles.rowValue}>{data.cancelReason}</Text>
        </View>
      ) : null}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
    backgroundColor: colors.ink,
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
  errorText: {
    color: '#ff6b6b',
  },
  hero: {
    backgroundColor: colors.card,
    borderRadius: radii.lg,
    padding: 24,
    marginBottom: 20,
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
    fontSize: 32,
    fontWeight: '700',
    marginTop: 8,
  },
  heroCaption: {
    color: colors.textMuted,
    marginTop: 4,
  },
  card: {
    backgroundColor: colors.card,
    borderRadius: radii.lg,
    padding: 20,
    marginBottom: 16,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.08)',
  },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 12,
  },
  rowLabel: {
    color: colors.textSecondary,
  },
  rowValue: {
    color: colors.textPrimary,
    fontWeight: '600',
  },
});

export default RideDetailScreen;
