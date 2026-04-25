import React, { useMemo } from 'react';
import { FlatList, Pressable, StyleSheet, Text, View } from 'react-native';
import { NavigationProp, useNavigation } from '@react-navigation/native';
import ScreenContainer from '@components/ScreenContainer';
import { useTrips } from '@hooks/useTrips';
import StatCard from '@components/StatCard';
import { colors, radii } from '@theme/index';
import { formatCurrency, formatDate } from '@utils/format';
import { RootStackParamList } from '@models/index';

const TripsScreen = () => {
  const { trips, isLoading } = useTrips();
  const navigation = useNavigation<NavigationProp<RootStackParamList>>();

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
      <Text style={styles.subtitle}>Open any trip to view booking details, timeline, and status updates.</Text>
      <View style={styles.statsRow}>
        <StatCard label="Total bookings" value={stats.totalTrips} caption="All statuses" />
        <StatCard label="Completed" value={stats.completed} caption="Finished rides" />
        <StatCard label="Spend" value={stats.spend} caption="Completed rides" />
      </View>

      {isLoading ? (
        <Text style={styles.loading}>Loading trips...</Text>
      ) : (
        <View style={styles.listContainer}>
          <FlatList
            style={styles.list}
            data={trips}
            keyExtractor={(item) => item.id}
            contentContainerStyle={{ paddingBottom: 120 }}
            renderItem={({ item }) => (
              <Pressable style={styles.tripCard} onPress={() => navigation.navigate('RideDetail', { bookingId: item.id })}>
                <View style={styles.tripHeader}>
                  <Text style={styles.tripTitle}>{item.bookingRef}</Text>
                  <Text style={styles.tripCost}>{formatCurrency(item.priceFinal)}</Text>
                </View>
                <Text style={styles.tripSubtitle}>Vehicle {item.scooterId || 'N/A'}</Text>
                <Text style={styles.tripMeta}>Status {item.status} | Updated {formatDate(item.updatedAt)}</Text>
                <Text style={styles.tripMeta}>Start {item.startAt ? formatDate(item.startAt) : 'Pending'} | End {item.endAt ? formatDate(item.endAt) : 'Pending'}</Text>
                <View style={styles.detailHintRow}>
                  <Text style={styles.detailHint}>Tap to open detailed trip view</Text>
                  <Text style={styles.routeHint}>Route trace: unavailable (backend location history not exposed yet)</Text>
                </View>
              </Pressable>
            )}
          />
        </View>
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
  listContainer: {
    flex: 1,
    minHeight: 220,
  },
  list: {
    flex: 1,
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
  detailHintRow: {
    marginTop: 10,
    paddingTop: 10,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: 'rgba(255,255,255,0.12)',
  },
  detailHint: {
    color: colors.lime,
    fontWeight: '600',
    fontSize: 12,
  },
  routeHint: {
    marginTop: 4,
    color: colors.textMuted,
    fontSize: 11,
  },
});

export default TripsScreen;
