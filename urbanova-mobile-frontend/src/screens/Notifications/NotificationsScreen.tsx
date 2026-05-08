import React from 'react';
import { Alert, FlatList, Pressable, StyleSheet, Text, View } from 'react-native';
import { useMutation, useQuery } from '@tanstack/react-query';
import { NavigationProp, useNavigation } from '@react-navigation/native';

import ScreenContainer from '@components/ScreenContainer';
import { NotificationRecord, NotificationService } from '@services/api';
import { colors, radii } from '@theme/index';
import { formatDate } from '@utils/format';
import { RootStackParamList } from '@models/index';

const getNotificationLabel = (type: string) => {
  switch (type) {
    case 'BOOKING_CONFIRMATION':
      return 'Booking';
    case 'BOOKING_CANCELLED':
      return 'Cancelled';
    case 'PAYMENT_UPDATED':
      return 'Payment';
    case 'ISSUE_UPDATED':
      return 'Support';
    case 'SCOOTER_LOW_BATTERY':
    case 'SCOOTER_CHARGED':
      return 'Vehicle';
    default:
      return 'Update';
  }
};

const NotificationsScreen = () => {
  const navigation = useNavigation<NavigationProp<RootStackParamList>>();
  const notificationsQuery = useQuery({
    queryKey: ['notifications'],
    queryFn: NotificationService.listMine,
  });

  const markReadMutation = useMutation({
    mutationFn: (notificationId: string) => NotificationService.markRead(notificationId),
    onSuccess: () => notificationsQuery.refetch(),
    onError: (error: any) => {
      Alert.alert('Update failed', error?.response?.data?.error?.message || 'Unable to mark notification as read.');
    },
  });

  const handleOpen = (notification: NotificationRecord) => {
    if (!notification.read) {
      markReadMutation.mutate(notification.notificationId);
    }
    if (notification.relatedBookingId) {
      navigation.navigate('RideDetail', { bookingId: notification.relatedBookingId });
    }
  };

  return (
    <ScreenContainer>
      <Text style={styles.title}>Notifications</Text>
      <Text style={styles.subtitle}>Booking emails, payment updates, and support ticket notifications.</Text>
      {notificationsQuery.isLoading ? <Text style={styles.helper}>Loading notifications...</Text> : null}
      <FlatList
        data={notificationsQuery.data ?? []}
        keyExtractor={(item) => item.notificationId}
        contentContainerStyle={{ paddingBottom: 140 }}
        ListEmptyComponent={!notificationsQuery.isLoading ? <Text style={styles.helper}>No notifications yet.</Text> : null}
        renderItem={({ item }) => (
          <Pressable style={[styles.card, !item.read && styles.cardUnread]} onPress={() => handleOpen(item)}>
            <View style={styles.cardHeader}>
              <Text style={styles.badge}>{getNotificationLabel(item.type)}</Text>
              {!item.read ? <Text style={styles.unreadDot}>New</Text> : null}
            </View>
            <Text style={styles.cardTitle}>{item.title}</Text>
            <Text style={styles.cardMessage}>{item.message}</Text>
            <Text style={styles.cardMeta}>
              {formatDate(item.createdAt)}
              {item.relatedBookingId ? ' | Tap to open booking' : ''}
            </Text>
          </Pressable>
        )}
      />
    </ScreenContainer>
  );
};

const styles = StyleSheet.create({
  title: {
    fontSize: 28,
    fontWeight: '800',
    color: colors.textPrimary,
  },
  subtitle: {
    color: colors.textSecondary,
    marginTop: 6,
    marginBottom: 20,
  },
  helper: {
    color: colors.textSecondary,
    marginTop: 12,
  },
  card: {
    backgroundColor: colors.card,
    borderRadius: radii.lg,
    padding: 16,
    marginBottom: 14,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.08)',
  },
  cardUnread: {
    borderColor: 'rgba(131,111,255,0.55)',
    backgroundColor: 'rgba(131,111,255,0.1)',
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 10,
  },
  badge: {
    color: colors.lime,
    fontWeight: '800',
    fontSize: 12,
    textTransform: 'uppercase',
  },
  unreadDot: {
    color: colors.ink,
    backgroundColor: colors.lime,
    borderRadius: 999,
    paddingHorizontal: 8,
    paddingVertical: 2,
    fontSize: 11,
    fontWeight: '800',
  },
  cardTitle: {
    color: colors.textPrimary,
    fontWeight: '800',
    fontSize: 16,
  },
  cardMessage: {
    color: colors.textSecondary,
    marginTop: 6,
  },
  cardMeta: {
    color: colors.textMuted,
    marginTop: 10,
    fontSize: 12,
  },
});

export default NotificationsScreen;
