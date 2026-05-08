import React, { useMemo, useState } from 'react';
import { FlatList, Pressable, StyleSheet, Text, View } from 'react-native';
import { NavigationProp, useNavigation } from '@react-navigation/native';
import ScreenContainer from '@components/ScreenContainer';
import { useTrips } from '@hooks/useTrips';
import StatCard from '@components/StatCard';
import { colors, radii } from '@theme/index';
import { formatCurrency, formatDate } from '@utils/format';
import { RootStackParamList } from '@models/index';

type TripTab = 'reserved' | 'inProgress' | 'completed';

const normalizeTripBucket = (status: string): TripTab => {
  const value = status.toUpperCase();
  if (value.includes('ACTIVE') || value.includes('STARTED') || value.includes('IN_PROGRESS')) return 'inProgress';
  if (value.includes('COMPLETED') || value.includes('PAID') || value.includes('DONE')) return 'completed';
  return 'reserved';
};

const TripsScreen = () => {
  const { trips, isLoading } = useTrips();
  const navigation = useNavigation<NavigationProp<RootStackParamList>>();
  const [activeTab, setActiveTab] = useState<TripTab>('reserved');

  const stats = useMemo(() => {
    const completed = trips.filter((trip) => trip.status === 'COMPLETED');
    const totalSpend = completed.reduce((sum, trip) => sum + trip.priceFinal, 0);
    return {
      totalTrips: `${trips.length}`,
      completed: `${completed.length}`,
      spend: formatCurrency(totalSpend),
    };
  }, [trips]);

  const activeTrips = useMemo(() => trips.filter((trip) => normalizeTripBucket(trip.status) === activeTab), [activeTab, trips]);

  return (
    <ScreenContainer>
      <Text style={styles.title}>Ride history</Text>
      <Text style={styles.subtitle}>Open any trip to view booking details, timeline, and status updates.</Text>
      <View style={styles.statsRow}>
        <StatCard label="Bookings" value={stats.totalTrips} caption="All rides" compact />
        <StatCard label="Completed" value={stats.completed} caption="Finished" compact />
        <StatCard label="Spend" value={stats.spend} caption="Paid rides" compact />
      </View>

      <View style={styles.tabRow}>
        {[
          { key: 'reserved', label: 'Reserved' },
          { key: 'inProgress', label: 'In progress' },
          { key: 'completed', label: 'Completed' },
        ].map((tab) => (
          <Pressable
            key={tab.key}
            style={[styles.tabChip, activeTab === tab.key && styles.tabChipActive]}
            onPress={() => setActiveTab(tab.key as TripTab)}
          >
            <Text style={[styles.tabChipLabel, activeTab === tab.key && styles.tabChipLabelActive]}>{tab.label}</Text>
          </Pressable>
        ))}
      </View>

      {isLoading ? (
        <Text style={styles.loading}>Loading trips...</Text>
      ) : (
        <View style={styles.listContainer}>
          <FlatList
            style={styles.list}
            data={activeTrips}
            keyExtractor={(item) => item.id}
            contentContainerStyle={{ paddingBottom: 180 }}
            ListFooterComponent={<View style={{ height: 80 }} />}
            ListEmptyComponent={<Text style={styles.sectionEmpty}>No trips here yet.</Text>}
            renderItem={({ item: trip }) => (
              <Pressable style={styles.tripCard} onPress={() => navigation.navigate('RideDetail', { bookingId: trip.id })}>
                <View style={styles.tripHeader}>
                  <Text style={styles.tripTitle}>Trip</Text>
                  <Text style={styles.tripCost}>{formatCurrency(trip.priceFinal)}</Text>
                </View>
                <Text style={styles.tripSubtitle}>{trip.bookingRef}</Text>
                <Text style={styles.tripSubtitle}>Vehicle {trip.scooterId || 'N/A'}</Text>
                <Text style={styles.tripMeta}>Status {trip.status} | Updated {formatDate(trip.updatedAt)}</Text>
                <Text style={styles.tripMeta}>Start {trip.startAt ? formatDate(trip.startAt) : 'Pending'} | End {trip.endAt ? formatDate(trip.endAt) : 'Pending'}</Text>
                <View style={styles.detailHintRow}>
                  <Text style={styles.detailHint}>Tap to open detailed trip view</Text>
                  <Text style={styles.routeHint}>Route trace: unavailable for now</Text>
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
    marginBottom: 18,
  },
  tabRow: {
    flexDirection: 'row',
    marginBottom: 16,
    padding: 6,
    borderRadius: radii.lg,
    backgroundColor: 'rgba(255,255,255,0.04)',
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.08)',
  },
  tabChip: {
    flex: 1,
    paddingVertical: 10,
    borderRadius: radii.md,
    alignItems: 'center',
  },
  tabChipActive: {
    backgroundColor: 'rgba(131,111,255,0.22)',
    borderWidth: 1,
    borderColor: colors.lime,
  },
  tabChipLabel: {
    color: colors.textSecondary,
    fontWeight: '600',
    fontSize: 12,
  },
  tabChipLabelActive: {
    color: colors.textPrimary,
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
  sectionEmpty: {
    color: colors.textMuted,
    marginTop: 8,
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
    fontSize: 15,
    fontWeight: '700',
  },
  tripSubtitle: {
    color: colors.textSecondary,
    marginTop: 4,
    fontSize: 12,
  },
  tripMeta: {
    color: colors.textMuted,
    fontSize: 11,
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
