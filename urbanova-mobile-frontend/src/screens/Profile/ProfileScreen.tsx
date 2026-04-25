import React, { useEffect, useState } from 'react';
import { Alert, Linking, Modal, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { BottomTabNavigationProp } from '@react-navigation/bottom-tabs';
import { useQuery } from '@tanstack/react-query';
import ScreenContainer from '@components/ScreenContainer';
import { useProfile } from '@hooks/useProfile';
import ProfileHeader from '@components/ProfileHeader';
import { colors, radii } from '@theme/index';
import PrimaryButton from '@components/PrimaryButton';
import { useAuthStore } from '@store/useAuthStore';
import { MainTabParamList } from '@models/index';
import { MaterialIcons } from '@expo/vector-icons';
import { useSettingsStore } from '@store/useSettingsStore';
import { AuthService } from '@services/api';
import { validatePasswordStrength } from '@utils/security';
import { LinearGradient } from 'expo-linear-gradient';

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
  const { profile, refetch } = useProfile();
  const logout = useAuthStore((state) => state.logout);
  const setUser = useAuthStore((state) => state.setUser);
  const tabNavigation = useNavigation<BottomTabNavigationProp<MainTabParamList>>();
  const fontScale = useSettingsStore((state) => state.fontScale);
  const contrastMode = useSettingsStore((state) => state.contrastMode);
  const setFontScale = useSettingsStore((state) => state.setFontScale);
  const setContrastMode = useSettingsStore((state) => state.setContrastMode);

  const usageSummaryQuery = useQuery({
    queryKey: ['usage-summary'],
    queryFn: AuthService.getUsageSummary,
    enabled: !!profile,
  });

  const [profileModalVisible, setProfileModalVisible] = useState(false);
  const [settingsModalVisible, setSettingsModalVisible] = useState(false);
  const [securityModalVisible, setSecurityModalVisible] = useState(false);

  const [fullNameInput, setFullNameInput] = useState('');
  const [phoneInput, setPhoneInput] = useState('');
  const [resetEmail, setResetEmail] = useState('');
  const [resetToken, setResetToken] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmNewPassword, setConfirmNewPassword] = useState('');

  const scaleFont = (size: number) => Math.round(size * fontScale);

  useEffect(() => {
    if (profile) {
      setFullNameInput(profile.fullName || '');
      setPhoneInput(profile.phone || '');
      setResetEmail(profile.email || '');
    }
  }, [profile]);

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
    tabNavigation.navigate('Wallet');
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

  const handleOpenSecurity = () => {
    setSecurityModalVisible(true);
  };

  const handleUpdateProfile = async () => {
    if (!fullNameInput.trim()) {
      Alert.alert('Validation', 'Full name is required.');
      return;
    }
    if (phoneInput && !/^[+0-9\s()-]{6,20}$/.test(phoneInput)) {
      Alert.alert('Validation', 'Phone format looks invalid.');
      return;
    }

    try {
      const updated = await AuthService.updateProfile({
        fullName: fullNameInput.trim(),
        phone: phoneInput.trim() || null,
      });
      setUser(updated);
      await refetch();
      Alert.alert('Profile updated', 'Your account details were saved.');
      setProfileModalVisible(false);
    } catch (error: any) {
      Alert.alert('Update failed', error?.response?.data?.error?.message || 'Unable to update profile.');
    }
  };

  const handleRequestResetToken = async () => {
    if (!resetEmail.trim()) {
      Alert.alert('Validation', 'Email is required.');
      return;
    }
    try {
      const result = await AuthService.forgotPassword(resetEmail.trim());
      setResetToken(result.resetToken || '');
      Alert.alert('Reset token generated', 'A reset token was generated. It has been pre-filled for this environment.');
    } catch (error: any) {
      Alert.alert('Request failed', error?.response?.data?.error?.message || 'Unable to generate reset token.');
    }
  };

  const handleResetPassword = async () => {
    if (!resetToken.trim()) {
      Alert.alert('Validation', 'Reset token is required.');
      return;
    }
    const strengthError = validatePasswordStrength(newPassword.trim());
    if (strengthError) {
      Alert.alert('Validation', strengthError);
      return;
    }
    if (newPassword !== confirmNewPassword) {
      Alert.alert('Validation', 'Passwords do not match.');
      return;
    }

    try {
      await AuthService.resetPassword(resetToken.trim(), newPassword.trim());
      setNewPassword('');
      setConfirmNewPassword('');
      setResetToken('');
      Alert.alert('Password reset', 'Password updated successfully. Please sign in again on next session.');
    } catch (error: any) {
      Alert.alert('Reset failed', error?.response?.data?.error?.message || 'Unable to reset password.');
    }
  };

  const handleReportVehicle = () => {
    Alert.alert('Report a vehicle', 'Open your active booking and submit a return fault report from Ride details.', [
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
      handler: handleManageProfile,
    },
    {
      id: 'payment',
      title: 'Manage payment methods',
      description: 'Bind cards and choose your default method in Wallet.',
      handler: handleManagePayment,
    },
    {
      id: 'history',
      title: 'View ride history',
      description: 'Jump to Trips for booking statuses and updates.',
      handler: handleViewTrips,
    },
    {
      id: 'security',
      title: 'Account security center',
      description: 'Request reset token and reset password with validation.',
      handler: handleOpenSecurity,
    },
    {
      id: 'support',
      title: 'Contact support',
      description: `Email ${SUPPORT_EMAIL} for account or billing issues.`,
      handler: handleContactSupport,
    },
    {
      id: 'report',
      title: 'Report return issue',
      description: 'Submit damage or abnormal return conditions from Ride details.',
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

        <LinearGradient colors={['rgba(131,111,255,0.35)', 'rgba(34,42,33,0.95)']} style={[styles.card, contrastMode === 'high' && styles.cardHighContrast]}>
          <Text style={[styles.sectionLabel, { fontSize: scaleFont(12) }]}>Usage summary</Text>
          {usageSummaryQuery.isLoading ? <Text style={styles.value}>Loading usage summary...</Text> : null}
          {usageSummaryQuery.data ? (
            <>
              <Text style={styles.value}>Bookings: {usageSummaryQuery.data.bookingCount}</Text>
              <Text style={styles.value}>Hours used: {usageSummaryQuery.data.hoursUsed}</Text>
              <Text style={styles.value}>Total spent: GBP {usageSummaryQuery.data.totalSpent}</Text>
              <Text style={styles.value}>7-day usage: {usageSummaryQuery.data.hoursLast7Days} hours</Text>
            </>
          ) : null}
        </LinearGradient>

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
              <View style={styles.shortcutIconWrap}>
                <MaterialIcons name={item.icon as any} size={20} color={colors.textPrimary} />
              </View>
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
              <View style={styles.actionContent}>
                <Text style={[styles.actionTitle, { fontSize: scaleFont(15) }]}>{item.title}</Text>
                <Text style={[styles.actionSubtitle, { fontSize: scaleFont(13) }]}>{item.description}</Text>
              </View>
              <MaterialIcons name="chevron-right" size={20} color={colors.textMuted} />
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

      <Modal visible={profileModalVisible} transparent animationType="fade" onRequestClose={() => setProfileModalVisible(false)}>
        <View style={styles.modalBackdrop}>
          <View style={[styles.modalCard, contrastMode === 'high' && styles.modalCardHighContrast]}>
            <Text style={[styles.modalTitle, { fontSize: scaleFont(18) }]}>Profile details</Text>
            <Text style={[styles.sectionLabel, styles.modalSectionLabel, { fontSize: scaleFont(12) }]}>Full name</Text>
            <TextInput
              style={styles.input}
              placeholder="Full name"
              placeholderTextColor={colors.textMuted}
              value={fullNameInput}
              onChangeText={setFullNameInput}
            />
            <Text style={[styles.sectionLabel, styles.modalSectionLabel, { fontSize: scaleFont(12) }]}>Phone</Text>
            <TextInput
              style={styles.input}
              placeholder="+44 ..."
              placeholderTextColor={colors.textMuted}
              value={phoneInput}
              onChangeText={setPhoneInput}
            />
            <Text style={[styles.sectionLabel, styles.modalSectionLabel, { fontSize: scaleFont(12) }]}>Email</Text>
            <Text style={[styles.value, { fontSize: scaleFont(16) }]}>{profile.email || 'Not provided'}</Text>
            <Text style={[styles.sectionLabel, styles.modalSectionLabel, { fontSize: scaleFont(12) }]}>User ID</Text>
            <Text style={[styles.value, { fontSize: scaleFont(16) }]}>{profile.userId}</Text>
            <PrimaryButton label="Save profile" onPress={handleUpdateProfile} style={{ marginTop: 16 }} />
            <PrimaryButton label="Close" onPress={() => setProfileModalVisible(false)} style={{ marginTop: 10 }} />
          </View>
        </View>
      </Modal>

      <Modal visible={settingsModalVisible} transparent animationType="fade" onRequestClose={() => setSettingsModalVisible(false)}>
        <View style={styles.modalBackdrop}>
          <View style={[styles.modalCard, contrastMode === 'high' && styles.modalCardHighContrast]}>
            <Text style={[styles.modalTitle, { fontSize: scaleFont(18) }]}>Settings</Text>
            <Text style={[styles.modalDescription, { fontSize: scaleFont(13) }]}>Adjust how URBANOVA looks on this device.</Text>
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

      <Modal visible={securityModalVisible} transparent animationType="fade" onRequestClose={() => setSecurityModalVisible(false)}>
        <View style={styles.modalBackdrop}>
          <View style={[styles.modalCard, contrastMode === 'high' && styles.modalCardHighContrast]}>
            <Text style={[styles.modalTitle, { fontSize: scaleFont(18) }]}>Security center</Text>
            <Text style={[styles.modalDescription, { fontSize: scaleFont(13) }]}>Use forgot/reset flow and validation for protected account operations.</Text>
            <Text style={[styles.sectionLabel, styles.modalSectionLabel, { fontSize: scaleFont(12) }]}>Email</Text>
            <TextInput
              style={styles.input}
              placeholder="Email"
              placeholderTextColor={colors.textMuted}
              autoCapitalize="none"
              value={resetEmail}
              onChangeText={setResetEmail}
            />
            <PrimaryButton label="Request reset token" onPress={handleRequestResetToken} />

            <Text style={[styles.sectionLabel, styles.modalSectionLabel, { fontSize: scaleFont(12) }]}>Reset token</Text>
            <TextInput
              style={styles.input}
              placeholder="Paste reset token"
              placeholderTextColor={colors.textMuted}
              value={resetToken}
              onChangeText={setResetToken}
            />
            <Text style={[styles.sectionLabel, styles.modalSectionLabel, { fontSize: scaleFont(12) }]}>New password</Text>
            <TextInput
              style={styles.input}
              placeholder="At least 8 chars, A-z, 0-9"
              placeholderTextColor={colors.textMuted}
              secureTextEntry
              value={newPassword}
              onChangeText={setNewPassword}
            />
            <Text style={[styles.sectionLabel, styles.modalSectionLabel, { fontSize: scaleFont(12) }]}>Confirm password</Text>
            <TextInput
              style={styles.input}
              placeholder="Re-enter password"
              placeholderTextColor={colors.textMuted}
              secureTextEntry
              value={confirmNewPassword}
              onChangeText={setConfirmNewPassword}
            />
            <PrimaryButton label="Reset password" onPress={handleResetPassword} style={{ marginTop: 8 }} />
            <PrimaryButton label="Close" onPress={() => setSecurityModalVisible(false)} style={{ marginTop: 10 }} />
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
  shortcutIconWrap: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: 'rgba(131,111,255,0.28)',
    alignItems: 'center',
    justifyContent: 'center',
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
    flexDirection: 'row',
    alignItems: 'center',
  },
  actionRowLast: {
    borderBottomWidth: 0,
  },
  actionContent: {
    flex: 1,
    paddingRight: 12,
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
  input: {
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.14)',
    borderRadius: radii.md,
    paddingHorizontal: 12,
    paddingVertical: 10,
    color: colors.textPrimary,
    marginTop: 8,
  },
});

export default ProfileScreen;
