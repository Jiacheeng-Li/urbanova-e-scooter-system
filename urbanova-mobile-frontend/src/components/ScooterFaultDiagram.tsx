import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import Svg, { Circle, Line, Path, Rect } from 'react-native-svg';
import { colors, radii } from '@theme/index';

export type FaultPart = 'brake' | 'handlebar' | 'battery' | 'wheel' | 'light' | 'body';

interface Props {
  selectedPart: FaultPart;
  onSelectPart: (part: FaultPart) => void;
}

const PARTS: Array<{ id: FaultPart; label: string; x: number; y: number }> = [
  { id: 'handlebar', label: 'Handlebar', x: 216, y: 46 },
  { id: 'brake', label: 'Brake', x: 198, y: 82 },
  { id: 'battery', label: 'Battery', x: 138, y: 128 },
  { id: 'body', label: 'Body', x: 116, y: 88 },
  { id: 'wheel', label: 'Wheel', x: 70, y: 168 },
  { id: 'light', label: 'Light', x: 238, y: 62 },
];

const ScooterFaultDiagram: React.FC<Props> = ({ selectedPart, onSelectPart }) => (
  <View style={styles.container}>
    <Svg width="100%" height={220} viewBox="0 0 280 220">
      <Line x1="84" y1="164" x2="190" y2="164" stroke="rgba(255,255,255,0.72)" strokeWidth="8" strokeLinecap="round" />
      <Line x1="188" y1="164" x2="216" y2="52" stroke="rgba(255,255,255,0.72)" strokeWidth="8" strokeLinecap="round" />
      <Line x1="206" y1="54" x2="246" y2="54" stroke="rgba(255,255,255,0.72)" strokeWidth="8" strokeLinecap="round" />
      <Rect x="102" y="112" width="72" height="32" rx="12" fill="rgba(131,111,255,0.3)" stroke={colors.lime} />
      <Path d="M92 98 C120 78 156 78 184 100" stroke="rgba(255,255,255,0.5)" strokeWidth="6" fill="none" strokeLinecap="round" />
      <Circle cx="70" cy="168" r="26" stroke="rgba(255,255,255,0.78)" strokeWidth="8" fill="rgba(255,255,255,0.04)" />
      <Circle cx="206" cy="168" r="26" stroke="rgba(255,255,255,0.78)" strokeWidth="8" fill="rgba(255,255,255,0.04)" />
      <Circle cx="238" cy="62" r="8" fill={colors.warning} />
      {PARTS.map((part) => (
        <Circle
          key={part.id}
          cx={part.x}
          cy={part.y}
          r={selectedPart === part.id ? 13 : 10}
          fill={selectedPart === part.id ? colors.lime : colors.graphite}
          stroke="#FFFFFF"
          strokeWidth="2"
        />
      ))}
    </Svg>
    <View style={styles.partGrid}>
      {PARTS.map((part) => (
        <Pressable
          key={part.id}
          style={[styles.partChip, selectedPart === part.id && styles.partChipActive]}
          onPress={() => onSelectPart(part.id)}
        >
          <Text style={styles.partChipText}>{part.label}</Text>
        </Pressable>
      ))}
    </View>
  </View>
);

const styles = StyleSheet.create({
  container: {
    borderRadius: radii.lg,
    backgroundColor: 'rgba(255,255,255,0.04)',
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.08)',
    padding: 10,
    marginVertical: 10,
  },
  partGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
  },
  partChip: {
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.16)',
    borderRadius: radii.md,
    paddingHorizontal: 10,
    paddingVertical: 8,
    marginRight: 8,
    marginBottom: 8,
  },
  partChipActive: {
    borderColor: colors.lime,
    backgroundColor: 'rgba(131,111,255,0.16)',
  },
  partChipText: {
    color: colors.textPrimary,
    fontWeight: '700',
    fontSize: 12,
  },
});

export default ScooterFaultDiagram;
