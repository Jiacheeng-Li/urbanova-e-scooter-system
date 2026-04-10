import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { colors, radii } from '@theme/index';

interface Props {
  label: string;
  value: string;
  caption?: string;
}

const StatCard: React.FC<Props> = ({ label, value, caption }) => (
  <View style={styles.card}>
    <Text style={styles.label}>{label}</Text>
    <Text style={styles.value}>{value}</Text>
    {caption ? <Text style={styles.caption}>{caption}</Text> : null}
  </View>
);

const styles = StyleSheet.create({
  card: {
    flex: 1,
    backgroundColor: 'rgba(255,255,255,0.06)',
    padding: 16,
    borderRadius: radii.md,
    marginRight: 12,
  },
  label: {
    color: colors.textSecondary,
    fontSize: 12,
    textTransform: 'uppercase',
  },
  value: {
    marginTop: 8,
    fontSize: 20,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  caption: {
    marginTop: 4,
    color: colors.textMuted,
  },
});

export default StatCard;
