import React, { useMemo } from 'react';
import { FlatList, StyleSheet, Text, View } from 'react-native';
import ScreenContainer from '@components/ScreenContainer';
import { useTrips } from '@hooks/useTrips';
import StatCard from '@components/StatCard';
import { colors, radii } from '@theme/index';
import { formatCurrency, formatDate } from '@utils/format';

const TripsScreen = () => {
  const { trips, isLoading } = useTrips();

  const stats = useMemo(() => {
    const completed = trips.filter((trip) => trip.status === 'COMPLETED');
    const totalSpend = completed.reduce((sum, trip) => sum + trip.priceFinal, 0);
    return {
      totalTrips: `${trips.length}`,
      completed: `${completed.length}`,
      spend: formatCurrency(totalSpend),
    };
  }, [trips]);

  return (
    <ScreenContainer>
      <Text style={styles.title}>Ride history</Text>
      <Text style={styles.subtitle}>Review your latest bookings and statuses.</Text>
      <View style={styles.statsRow}>
        <StatCard label="Total bookings" value={stats.totalTrips} caption="All statuses" />
        <StatCard label="Completed" value={stats.completed} caption="Finished rides" />
        <StatCard label="Spend" value={stats.spend} caption="Completed rides" />
      </View>
      {isLoading ? (
        <Text style={styles.loading}>Loading trips...</Text>
      ) : (
        <FlatList
          data={trips}
          keyExtractor={(item) => item.id}
          contentContainerStyle={{ paddingBottom: 40 }}
          renderItem={({ item }) => (
            <View style={styles.tripCard}>
              <View style={styles.tripHeader}>
                <Text style={styles.tripTitle}>{item.bookingRef}</Text>
                <Text style={styles.tripCost}>{formatCurrency(item.priceFinal)}</Text>
              </View>
              <Text style={styles.tripSubtitle}>Vehicle {item.scooterId || 'N/A'}</Text>
              <Text style={styles.tripMeta}>Status {item.status} | Updated {formatDate(item.updatedAt)}</Text>
            </View>
          )}
        />
      )}
    </ScreenContainer>
  );
};

const styles = StyleSheet.create({
  title: {
    fontSize: 28,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  subtitle: {
    color: colors.textSecondary,
    marginBottom: 20,
  },
  statsRow: {
    flexDirection: 'row',
    marginBottom: 24,
  },
  loading: {
    color: colors.textSecondary,
  },
  tripCard: {
    backgroundColor: colors.card,
    borderRadius: radii.lg,
    padding: 16,
    marginBottom: 16,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.08)',
  },
  tripHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  tripTitle: {
    color: colors.textPrimary,
    fontSize: 16,
    fontWeight: '600',
  },
  tripSubtitle: {
    color: colors.textSecondary,
    marginTop: 4,
  },
  tripMeta: {
    color: colors.textMuted,
    fontSize: 12,
    marginTop: 4,
  },
  tripCost: {
    color: colors.lime,
    fontWeight: '700',
  },
});

export default TripsScreen;
