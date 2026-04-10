import React from 'react';
import { Image, Pressable, StyleSheet, Text, View } from 'react-native';
import { Vehicle } from '@models/index';
import { colors, radii } from '@theme/index';
import { formatCurrency } from '@utils/format';

interface Props {
  vehicle: Vehicle;
  isSelected?: boolean;
  onPress?: (vehicle: Vehicle) => void;
  onReserve?: (vehicle: Vehicle) => void;
}

const VehicleCard: React.FC<Props> = ({ vehicle, isSelected, onPress, onReserve }) => (
  <Pressable onPress={() => onPress?.(vehicle)} style={[styles.card, isSelected && styles.selectedCard]}>
    {vehicle.image ? <Image source={{ uri: vehicle.image }} style={styles.image} /> : <View style={styles.placeholder} />}
    <View style={styles.info}>
      <View style={styles.heading}>
        <Text style={styles.name}>{vehicle.name}</Text>
        <Text style={[styles.badge, statusColor(vehicle.status)]}>{vehicle.status}</Text>
      </View>
      <Text style={styles.meta}>
        {vehicle.battery}% | {vehicle.distance.toFixed(1)} km | {formatCurrency(vehicle.pricePerMin, 'GBP')}/min
      </Text>
      <Pressable style={styles.reserveBtn} onPress={() => onReserve?.(vehicle)}>
        <Text style={styles.reserveLabel}>Reserve</Text>
      </Pressable>
    </View>
  </Pressable>
);

const statusColor = (status: string) => {
  switch (status) {
    case 'available':
      return { backgroundColor: 'rgba(131,111,255,0.12)', color: colors.lime };
    case 'low-battery':
      return { backgroundColor: 'rgba(255,212,71,0.12)', color: colors.warning };
    case 'reserved':
      return { backgroundColor: 'rgba(34,132,198,0.15)', color: colors.info };
    case 'in-ride':
      return { backgroundColor: 'rgba(255,255,255,0.12)', color: colors.textSecondary };
    default:
      return { backgroundColor: 'rgba(255,255,255,0.12)', color: colors.textSecondary };
  }
};

const styles = StyleSheet.create({
  card: {
    backgroundColor: 'rgba(5,9,5,0.75)',
    borderRadius: radii.lg,
    padding: 16,
    flexDirection: 'row',
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.08)',
    marginBottom: 12,
  },
  selectedCard: {
    borderColor: colors.lime,
  },
  image: {
    width: 64,
    height: 64,
    borderRadius: radii.md,
    marginRight: 12,
  },
  placeholder: {
    width: 64,
    height: 64,
    marginRight: 12,
    borderRadius: radii.md,
    backgroundColor: 'rgba(255,255,255,0.08)',
  },
  info: {
    flex: 1,
  },
  heading: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  name: {
    color: colors.textPrimary,
    fontSize: 16,
    fontWeight: '600',
  },
  badge: {
    fontSize: 12,
    fontWeight: '600',
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: radii.sm,
    textTransform: 'capitalize',
  } as any,
  meta: {
    color: colors.textSecondary,
    marginTop: 6,
    marginBottom: 10,
  },
  reserveBtn: {
    alignSelf: 'flex-start',
    borderRadius: radii.md,
    borderWidth: 1,
    borderColor: colors.lime,
    paddingHorizontal: 14,
    paddingVertical: 6,
  },
  reserveLabel: {
    color: colors.lime,
    fontWeight: '600',
  },
});

export default VehicleCard;
