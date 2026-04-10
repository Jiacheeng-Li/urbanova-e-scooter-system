import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { UserProfileData } from '@services/api';
import { colors, radii } from '@theme/index';

interface Props {
  profile: UserProfileData;
}

const ProfileHeader: React.FC<Props> = ({ profile }) => (
  <LinearGradient colors={['#132019', '#0A0F0A']} style={styles.container}>
    <View style={[styles.avatar, { backgroundColor: colors.lime }]}>
      <Text style={styles.avatarLabel}>{profile.fullName?.[0]?.toUpperCase() || 'U'}</Text>
    </View>
    <View style={styles.content}>
      <Text style={styles.name}>{profile.fullName || 'Rider'}</Text>
      <Text style={styles.meta}>
        {(profile.role || 'Standard').toUpperCase()} | {(profile.discountCategory || 'General').toLowerCase()} member
      </Text>
      <View style={styles.row}>
        <Text style={styles.badge}>{profile.role?.toUpperCase() || 'USER'}</Text>
        <Text style={styles.badge}>ID {profile.userId?.slice(-4) || '0000'}</Text>
      </View>
    </View>
  </LinearGradient>
);

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 20,
    borderRadius: radii.lg,
    marginBottom: 20,
  },
  avatar: {
    width: 64,
    height: 64,
    borderRadius: 32,
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarLabel: {
    color: colors.ink,
    fontSize: 28,
    fontWeight: '700',
  },
  content: {
    marginLeft: 16,
  },
  name: {
    color: colors.textPrimary,
    fontSize: 20,
    fontWeight: '600',
  },
  meta: {
    color: colors.textSecondary,
    marginTop: 4,
  },
  row: {
    flexDirection: 'row',
    marginTop: 8,
  },
  badge: {
    backgroundColor: 'rgba(255,255,255,0.08)',
    color: colors.textPrimary,
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: radii.sm,
    marginRight: 8,
    fontSize: 12,
    fontWeight: '600',
  },
});

export default ProfileHeader;
