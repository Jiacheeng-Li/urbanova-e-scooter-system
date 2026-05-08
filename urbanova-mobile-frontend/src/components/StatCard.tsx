import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { colors, radii } from '@theme/index';

interface Props {
  label: string;
  value: string;
  caption?: string;
  compact?: boolean;
}

const StatCard: React.FC<Props> = ({ label, value, caption, compact }) => (
  <View style={[styles.card, compact && styles.cardCompact]}>
    <Text style={[styles.label, compact && styles.labelCompact]} numberOfLines={1} adjustsFontSizeToFit>
      {label}
    </Text>
    <Text style={[styles.value, compact && styles.valueCompact]} numberOfLines={1} adjustsFontSizeToFit>
      {value}
    </Text>
    {caption ? (
      <Text style={[styles.caption, compact && styles.captionCompact]} numberOfLines={1} adjustsFontSizeToFit>
        {caption}
      </Text>
    ) : null}
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
  cardCompact: {
    paddingHorizontal: 10,
    paddingVertical: 12,
    marginRight: 8,
  },
  label: {
    color: colors.textSecondary,
    fontSize: 12,
    textTransform: 'uppercase',
  },
  labelCompact: {
    fontSize: 10,
    letterSpacing: 0.2,
  },
  value: {
    marginTop: 8,
    fontSize: 20,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  valueCompact: {
    fontSize: 17,
  },
  caption: {
    marginTop: 4,
    color: colors.textMuted,
  },
  captionCompact: {
    fontSize: 10,
  },
});

export default StatCard;
