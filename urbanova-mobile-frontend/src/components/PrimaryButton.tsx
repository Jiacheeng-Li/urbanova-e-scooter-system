import { LinearGradient } from 'expo-linear-gradient';
import React from 'react';
import { ActivityIndicator, Pressable, StyleSheet, Text, ViewStyle } from 'react-native';
import { colors, radii } from '@theme/index';

interface Props {
  label: string;
  onPress: () => void;
  isLoading?: boolean;
  disabled?: boolean;
  style?: ViewStyle;
}

const PrimaryButton: React.FC<Props> = ({ label, onPress, isLoading, disabled, style }) => (
  <Pressable onPress={onPress} disabled={disabled || isLoading} style={[styles.wrapper, style]}>
    <LinearGradient colors={[colors.lime, colors.limeDark]} style={styles.gradient} start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }}>
      {isLoading ? <ActivityIndicator color={colors.ink} /> : <Text style={styles.label}>{label}</Text>}
    </LinearGradient>
  </Pressable>
);

const styles = StyleSheet.create({
  wrapper: {
    alignSelf: 'stretch',
  },
  gradient: {
    borderRadius: radii.lg,
    paddingVertical: 16,
    alignItems: 'center',
  },
  label: {
    fontWeight: '600',
    fontSize: 16,
    color: colors.ink,
  },
});

export default PrimaryButton;
