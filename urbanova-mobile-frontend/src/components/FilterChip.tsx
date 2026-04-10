import React from 'react';
import { Pressable, StyleSheet, Text } from 'react-native';
import { colors, radii } from '@theme/index';

interface Props {
  label: string;
  isActive?: boolean;
  onPress: () => void;
}

const FilterChip: React.FC<Props> = ({ label, isActive, onPress }) => (
  <Pressable onPress={onPress} style={[styles.chip, isActive && styles.activeChip]}>
    <Text style={[styles.label, isActive && styles.activeLabel]}>{label}</Text>
  </Pressable>
);

const styles = StyleSheet.create({
  chip: {
    borderRadius: radii.lg,
    paddingHorizontal: 16,
    paddingVertical: 8,
    backgroundColor: 'rgba(255,255,255,0.08)',
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.12)',
    marginRight: 8,
  },
  activeChip: {
    backgroundColor: colors.limeDark,
    borderColor: colors.lime,
  },
  label: {
    color: colors.textSecondary,
    fontSize: 14,
    fontWeight: '500',
  },
  activeLabel: {
    color: colors.ink,
  },
});

export default FilterChip;
