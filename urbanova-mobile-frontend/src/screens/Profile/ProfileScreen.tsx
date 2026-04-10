import React, { useState } from 'react';
import { Alert, Linking, Modal, Pressable, StyleSheet, Text, View } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { BottomTabNavigationProp } from '@react-navigation/bottom-tabs';
import ScreenContainer from '@components/ScreenContainer';
import { useProfile } from '@hooks/useProfile';
import ProfileHeader from '@components/ProfileHeader';
import { colors, radii } from '@theme/index';
import PrimaryButton from '@components/PrimaryButton';
import { useAuthStore } from '@store/useAuthStore';
import { MainTabParamList } from '@models/index';
import { MaterialIcons } from '@expo/vector-icons';
import { useSettingsStore } from '@store/useSettingsStore';

const SUPPORT_EMAIL = 'support@urbanova.app';

const FONT_SIZE_OPTIONS = [
  { label: 'Small', value: 0.9 },
  { label: 'Default', value: 1 },
  { label: 'Large', value: 1.2 },
];

const CONTRAST_OPTIONS: { label: string; value: 'standard' | 'high' }[] = [
  { label: 'Standard', value: 'standard' },
  { label: 'High contrast', value: 'high' },
];

const ProfileScreen = () => {
  const { profile } = useProfile();
  const logout = useAuthStore((state) => state.logout);
  const tabNavigation = useNavigation<BottomTabNavigationProp<MainTabParamList>>();
  const fontScale = useSettingsStore((state) => state.fontScale);
  const contrastMode = useSettingsStore((state) => state.contrastMode);
  const setFontScale = useSettingsStore((state) => state.setFontScale);
  const setContrastMode = useSettingsStore((state) => state.setContrastMode);
  const [profileModalVisible, setProfileModalVisible] = useState(false);
  const [settingsModalVisible, setSettingsModalVisible] = useState(false);
  const scaleFont = (size: number) => Math.round(size * fontScale);

  const handleLogout = () => {
    Alert.alert('Sign out', 'Are you sure you want to sign out?', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Confirm',
        onPress: () => logout(),
      },
    ]);
  };

  const handleContactSupport = () => {
    Linking.openURL(`mailto:${SUPPORT_EMAIL}?subject=URBANOVA%20support`);
  };

  const handleManagePayment = () => {
    Alert.alert('Payments', 'Online payment management is coming soon. For now, please contact support to update cards.');
  };

  const handleViewTrips = () => {
    tabNavigation.navigate('Trips');
  };

  const handleManageProfile = () => {
    setProfileModalVisible(true);
  };

  const handleOpenSettings = () => {
    setSettingsModalVisible(true);
  };

  const handleUpdateContact = () => {
    Alert.alert('Update contact info', 'We will sync profile editing with the backend soon. For now, contact support with the details you want to change.');
  };

  const handleReportVehicle = () => {
    Alert.alert('Report a vehicle', 'Head over to the Ride tab to pick a scooter and file a report from its card.', [
      { text: 'Stay here', style: 'cancel' },
      {
        text: 'Go to Ride',
        onPress: () => tabNavigation.navigate('Ride'),
      },
    ]);
  };

  if (!profile) {
    return (
      <ScreenContainer>
        <Text style={{ color: colors.textPrimary }}>Loading profile...</Text>
      </ScreenContainer>
    );
  }

  const actionItems = [
    {
      id: 'contact',
      title: 'Update contact information',
      description: 'Keep your phone and email current for ride receipts.',
      handler: handleUpdateContact,
    },
    {
      id: 'payment',
      title: 'Manage payment methods',
      description: 'Choose which card is used for URBANOVA Cash and passes.',
      handler: handleManagePayment,
    },
    {
      id: 'history',
      title: 'View ride history',
      description: 'Jump to your Trips tab for detailed receipts.',
      handler: handleViewTrips,
    },
    {
      id: 'support',
      title: 'Contact support',
      description: `Email ${SUPPORT_EMAIL} or chat with an agent.`,
      handler: handleContactSupport,
    },
    {
      id: 'report',
      title: 'Report a vehicle issue',
      description: 'Send us scooter damage or parking concerns.',
      handler: handleReportVehicle,
    },
  ];

  const shortcutButtons = [
    { id: 'profile', label: 'Profile', icon: 'badge', handler: handleManageProfile },
    { id: 'orders', label: 'Orders', icon: 'history', handler: handleViewTrips },
    { id: 'settings', label: 'Settings', icon: 'settings', handler: handleOpenSettings },
  ];

  return (
    <>
      <ScreenContainer scrollable contentStyle={styles.scrollContent}>
        <ProfileHeader profile={profile} />
        <View style={styles.shortcutRow}>
          {shortcutButtons.map((item, index) => (
            <Pressable
              key={item.id}
              style={[
                styles.shortcutCard,
                contrastMode === 'high' && styles.shortcutCardHighContrast,
                index < shortcutButtons.length - 1 && styles.shortcutSpacing,
              ]}
              onPress={item.handler}
            >
              <MaterialIcons name={item.icon as any} size={20} color={colors.textPrimary} />
              <Text style={[styles.shortcutLabel, { fontSize: scaleFont(13) }]}>{item.label}</Text>
            </Pressable>
          ))}
        </View>
        <View style={[styles.actionList, contrastMode === 'high' && styles.cardHighContrast]}>
          {actionItems.map((item, index) => (
            <Pressable
              key={item.id}
              style={[styles.actionRow, index === actionItems.length - 1 && styles.actionRowLast]}
              onPress={item.handler}
            >
              <Text style={[styles.actionTitle, { fontSize: scaleFont(15) }]}>{item.title}</Text>
              <Text style={[styles.actionSubtitle, { fontSize: scaleFont(13) }]}>{item.description}</Text>
            </Pressable>
          ))}
        </View>
        <View style={[styles.card, styles.needHelpCard, contrastMode === 'high' && styles.cardHighContrast]}>
          <Text style={[styles.needHelpTitle, { fontSize: scaleFont(16) }]}>Need help?</Text>
          <Text style={[styles.needHelpValue, { fontSize: scaleFont(14) }]}>{SUPPORT_EMAIL}</Text>
          <Text style={[styles.needHelpHint, { fontSize: scaleFont(12) }]}>Available 24/7 via email</Text>
          <Pressable onPress={handleContactSupport}>
            <Text style={[styles.linkText, { fontSize: scaleFont(14) }]}>Email support</Text>
          </Pressable>
        </View>
        <PrimaryButton label="Sign out" onPress={handleLogout} style={styles.logoutButton} />
      </ScreenContainer>

      <Modal
        visible={profileModalVisible}
        transparent
        animationType="fade"
        onRequestClose={() => setProfileModalVisible(false)}
      >
        <View style={styles.modalBackdrop}>
          <View style={[styles.modalCard, contrastMode === 'high' && styles.modalCardHighContrast]}>
            <Text style={[styles.modalTitle, { fontSize: scaleFont(18) }]}>Profile details</Text>
            <Text style={[styles.sectionLabel, styles.modalSectionLabel, { fontSize: scaleFont(12) }]}>Phone</Text>
            <Text style={[styles.value, { fontSize: scaleFont(16) }]}>{profile.phone || 'Not provided'}</Text>
            <Text style={[styles.sectionLabel, styles.modalSectionLabel, { fontSize: scaleFont(12) }]}>Email</Text>
            <Text style={[styles.value, { fontSize: scaleFont(16) }]}>{profile.email || 'Not provided'}</Text>
            <Text style={[styles.sectionLabel, styles.modalSectionLabel, { fontSize: scaleFont(12) }]}>User ID</Text>
            <Text style={[styles.value, { fontSize: scaleFont(16) }]}>{profile.userId}</Text>
            <Text style={[styles.sectionLabel, styles.modalSectionLabel, { fontSize: scaleFont(12) }]}>Member since</Text>
            <Text style={[styles.value, { fontSize: scaleFont(16) }]}>
              {profile.createdAt ? new Date(profile.createdAt).toLocaleDateString('en-GB') : '-'}
            </Text>
            <PrimaryButton label="Close" onPress={() => setProfileModalVisible(false)} style={{ marginTop: 16 }} />
          </View>
        </View>
      </Modal>

      <Modal
        visible={settingsModalVisible}
        transparent
        animationType="fade"
        onRequestClose={() => setSettingsModalVisible(false)}
      >
        <View style={styles.modalBackdrop}>
          <View style={[styles.modalCard, contrastMode === 'high' && styles.modalCardHighContrast]}>
            <Text style={[styles.modalTitle, { fontSize: scaleFont(18) }]}>Settings</Text>
            <Text style={[styles.modalDescription, { fontSize: scaleFont(13) }]}>
              Adjust how URBANOVA looks on this device.
            </Text>
            <Text style={[styles.sectionLabel, styles.modalSectionLabel, { fontSize: scaleFont(12) }]}>Font size</Text>
            <View style={styles.optionRow}>
              {FONT_SIZE_OPTIONS.map((option) => (
                <Pressable
                  key={option.label}
                  style={[styles.optionChip, fontScale === option.value && styles.optionChipActive]}
                  onPress={() => setFontScale(option.value)}
                >
                  <Text style={[styles.optionChipLabel, { fontSize: scaleFont(12) }]}>{option.label}</Text>
                </Pressable>
              ))}
            </View>
            <Text style={[styles.sectionLabel, styles.modalSectionLabel, { fontSize: scaleFont(12) }]}>Theme</Text>
            <View style={styles.optionRow}>
              {CONTRAST_OPTIONS.map((option) => (
                <Pressable
                  key={option.value}
                  style={[styles.optionChip, contrastMode === option.value && styles.optionChipActive]}
                  onPress={() => setContrastMode(option.value)}
                >
                  <Text style={[styles.optionChipLabel, { fontSize: scaleFont(12) }]}>{option.label}</Text>
                </Pressable>
              ))}
            </View>
            <PrimaryButton label="Done" onPress={() => setSettingsModalVisible(false)} style={{ marginTop: 16 }} />
          </View>
        </View>
      </Modal>
    </>
  );
};

const styles = StyleSheet.create({
  scrollContent: {
    paddingBottom: 32,
  },
  card: {
    backgroundColor: colors.card,
    borderRadius: radii.lg,
    padding: 20,
    marginBottom: 16,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.08)',
  },
  cardHighContrast: {
    backgroundColor: '#1B1C35',
    borderColor: colors.lime,
  },
  shortcutRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 16,
  },
  shortcutCard: {
    flex: 1,
    borderRadius: radii.md,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.12)',
    paddingVertical: 16,
    alignItems: 'center',
    backgroundColor: colors.surface,
  },
  shortcutCardHighContrast: {
    borderColor: colors.lime,
    backgroundColor: '#1B1C35',
  },
  shortcutSpacing: {
    marginRight: 12,
  },
  shortcutLabel: {
    color: colors.textPrimary,
    marginTop: 8,
    fontWeight: '600',
  },
  label: {
    color: colors.textSecondary,
    fontSize: 12,
    textTransform: 'uppercase',
    marginTop: 8,
  },
  value: {
    color: colors.textPrimary,
    fontSize: 16,
    marginTop: 4,
  },
  actionList: {
    backgroundColor: colors.card,
    borderRadius: radii.lg,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.08)',
    marginTop: 8,
    marginBottom: 24,
  },
  actionRow: {
    padding: 16,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: 'rgba(255,255,255,0.08)',
  },
  actionRowLast: {
    borderBottomWidth: 0,
  },
  actionTitle: {
    color: colors.textPrimary,
    fontWeight: '600',
  },
  actionSubtitle: {
    color: colors.textSecondary,
    marginTop: 4,
    fontSize: 13,
  },
  needHelpCard: {
    alignItems: 'flex-start',
  },
  needHelpTitle: {
    color: colors.textPrimary,
    fontWeight: '600',
  },
  needHelpValue: {
    color: colors.textPrimary,
    marginTop: 6,
  },
  needHelpHint: {
    color: colors.textSecondary,
    marginTop: 4,
  },
  linkText: {
    color: colors.lime,
    fontWeight: '600',
    marginTop: 8,
  },
  logoutButton: {
    marginTop: 12,
  },
  modalBackdrop: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.6)',
    justifyContent: 'center',
    paddingHorizontal: 24,
  },
  modalCard: {
    backgroundColor: colors.graphite,
    borderRadius: radii.lg,
    padding: 24,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.12)',
  },
  modalCardHighContrast: {
    backgroundColor: '#1B1C35',
    borderColor: colors.lime,
  },
  modalTitle: {
    color: colors.textPrimary,
    fontWeight: '700',
    marginBottom: 8,
  },
  modalDescription: {
    color: colors.textSecondary,
    marginBottom: 12,
  },
  modalSectionLabel: {
    marginTop: 12,
  },
  sectionLabel: {
    color: colors.textSecondary,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  optionRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginTop: 8,
  },
  optionChip: {
    paddingVertical: 8,
    paddingHorizontal: 12,
    borderRadius: radii.md,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.2)',
    marginRight: 8,
    marginBottom: 8,
  },
  optionChipActive: {
    borderColor: colors.lime,
    backgroundColor: 'rgba(131,111,255,0.15)',
  },
  optionChipLabel: {
    color: colors.textPrimary,
    fontWeight: '600',
  },
});

export default ProfileScreen;
