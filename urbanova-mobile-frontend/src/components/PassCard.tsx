import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { Pass } from '@models/index';
import { colors, radii } from '@theme/index';
import { formatCurrency } from '@utils/format';

interface Props {
  pass: Pass;
  onSelect?: (pass: Pass) => void;
  isSelected?: boolean;
}

const PassCard: React.FC<Props> = ({ pass, onSelect, isSelected }) => (
  <Pressable onPress={() => onSelect?.(pass)} style={styles.wrapper}>
    <LinearGradient
      colors={['#0F2217', '#0A0F0A']}
      style={[styles.card, pass.highlight && styles.highlight, isSelected && styles.selected]}
    >
      <View style={styles.row}>
        <Text style={styles.name}>{pass.name}</Text>
        {isSelected ? <Text style={styles.selectionPill}>Selected</Text> : null}
      </View>
      {pass.highlight ? <Text style={styles.tag}>{pass.highlight}</Text> : null}
      <Text style={styles.price}>{formatCurrency(pass.price, 'GBP')}</Text>
      <Text style={styles.description}>{Math.round(pass.durationMinutes / 60)} hour package</Text>
      <Text style={styles.footer}>Billing currency: {pass.currency || 'GBP'}</Text>
    </LinearGradient>
  </Pressable>
);

const styles = StyleSheet.create({
  wrapper: {
    marginBottom: 16,
  },
  card: {
    borderRadius: radii.lg,
    padding: 20,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.08)',
  },
  highlight: {
    borderColor: colors.lime,
  },
  selected: {
    borderColor: colors.lime,
    shadowColor: colors.lime,
    shadowOpacity: 0.25,
    shadowRadius: 12,
    shadowOffset: { width: 0, height: 6 },
  },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  name: {
    color: colors.textPrimary,
    fontSize: 18,
    fontWeight: '600',
  },
  tag: {
    fontSize: 12,
    color: colors.ink,
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: radii.sm,
    backgroundColor: colors.lime,
    marginTop: 8,
    alignSelf: 'flex-start',
  },
  price: {
    color: colors.lime,
    fontSize: 26,
    fontWeight: '700',
    marginTop: 12,
  },
  description: {
    color: colors.textSecondary,
    marginTop: 6,
  },
  footer: {
    marginTop: 16,
    color: colors.textMuted,
  },
  selectionPill: {
    borderWidth: 1,
    borderColor: colors.lime,
    color: colors.lime,
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: radii.sm,
    fontSize: 12,
    fontWeight: '600',
  },
});

export default PassCard;
