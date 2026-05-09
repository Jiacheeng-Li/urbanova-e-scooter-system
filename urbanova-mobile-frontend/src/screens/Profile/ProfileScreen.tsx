import React, { useEffect, useState } from 'react';
import { Alert, Linking, Modal, Pressable, ScrollView as RNScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { BottomTabNavigationProp } from '@react-navigation/bottom-tabs';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useQuery } from '@tanstack/react-query';
import ScreenContainer from '@components/ScreenContainer';
import { useProfile } from '@hooks/useProfile';
import ProfileHeader from '@components/ProfileHeader';
import { colors, radii } from '@theme/index';
import PrimaryButton from '@components/PrimaryButton';
import { useAuthStore } from '@store/useAuthStore';
import { MainTabParamList, RootStackParamList } from '@models/index';
import { MaterialIcons } from '@expo/vector-icons';
import { useSettingsStore } from '@store/useSettingsStore';
import { AuthService } from '@services/api';
import { validatePasswordStrength } from '@utils/security';
import { getEligibilityHeadline, isUserFacingPromotionType } from '@utils/promotions';
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
  const stackNavigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const fontScale = useSettingsStore((state) => state.fontScale);
  const contrastMode = useSettingsStore((state) => state.contrastMode);
  const setFontScale = useSettingsStore((state) => state.setFontScale);
  const setContrastMode = useSettingsStore((state) => state.setContrastMode);

  const usageSummaryQuery = useQuery({
    queryKey: ['usage-summary', profile?.userId ?? 'guest'],
    queryFn: AuthService.getUsageSummary,
    enabled: !!profile,
  });

  const [profileModalVisible, setProfileModalVisible] = useState(false);
  const [settingsModalVisible, setSettingsModalVisible] = useState(false);
  const [securityModalVisible, setSecurityModalVisible] = useState(false);

  const [fullNameInput, setFullNameInput] = useState('');
  const [phoneInput, setPhoneInput] = useState('');
  const [birthDateInput, setBirthDateInput] = useState('');
  const [birthdayPickerVisible, setBirthdayPickerVisible] = useState(false);
  const [resetEmail, setResetEmail] = useState('');
  const [resetCode, setResetCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmNewPassword, setConfirmNewPassword] = useState('');
  const [birthYear, setBirthYear] = useState(new Date().getFullYear() - 25);
  const [birthMonth, setBirthMonth] = useState(new Date().getMonth());
  const [birthDay, setBirthDay] = useState(new Date().getDate());

  const scaleFont = (size: number) => Math.round(size * fontScale);

  useEffect(() => {
    if (profile) {
      setFullNameInput(profile.fullName || '');
      setPhoneInput(profile.phone || '');
      setBirthDateInput(profile.birthDate || '');
      setResetEmail(profile.email || '');
    }
  }, [profile]);

  const birthMonthLabels = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  const birthYearOptions = Array.from({ length: 90 }, (_, index) => new Date().getFullYear() - index);
  const birthDayOptions = Array.from({ length: new Date(birthYear, birthMonth + 1, 0).getDate() }, (_, index) => index + 1);

  const formatBirthDate = () =>
    `${birthYear}-${String(birthMonth + 1).padStart(2, '0')}-${String(birthDay).padStart(2, '0')}`;

  const openBirthdayPicker = () => {
    if (birthDateInput.trim() && /^\d{4}-\d{2}-\d{2}$/.test(birthDateInput.trim())) {
      const parsed = new Date(birthDateInput.trim());
      if (!Number.isNaN(parsed.getTime())) {
        setBirthYear(parsed.getFullYear());
        setBirthMonth(parsed.getMonth());
        setBirthDay(parsed.getDate());
      }
    }
    setBirthdayPickerVisible(true);
  };

  const confirmBirthdayPicker = () => {
    setBirthDateInput(formatBirthDate());
    setBirthdayPickerVisible(false);
  };

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

  const handleOpenFeedback = () => {
    stackNavigation.navigate('Feedback');
  };

  const handleManagePayment = () => {
    tabNavigation.navigate('Wallet');
  };

  const handleOpenNotifications = () => {
    stackNavigation.navigate('Notifications');
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
    if (birthDateInput.trim() && !/^\d{4}-\d{2}-\d{2}$/.test(birthDateInput.trim())) {
      Alert.alert('Validation', 'Birth date must use YYYY-MM-DD format.');
      return;
    }

    try {
      const updated = await AuthService.updateProfile({
        fullName: fullNameInput.trim(),
        phone: phoneInput.trim() || null,
        birthDate: birthDateInput.trim() || null,
      });
      setUser(updated);
      await refetch();
      Alert.alert('Profile updated', 'Your account details were saved.');
      setProfileModalVisible(false);
    } catch (error: any) {
      Alert.alert('Update failed', error?.response?.data?.error?.message || 'Unable to update profile.');
    }
  };

  const handleRequestResetCode = async () => {
    if (!resetEmail.trim()) {
      Alert.alert('Validation', 'Email is required.');
      return;
    }
    try {
      await AuthService.forgotPassword(resetEmail.trim());
      Alert.alert('Reset code sent', 'If the email is registered, a password reset code has been sent.');
    } catch (error: any) {
      Alert.alert('Request failed', error?.response?.data?.error?.message || 'Unable to send reset code.');
    }
  };

  const handleResetPassword = async () => {
    if (!resetEmail.trim()) {
      Alert.alert('Validation', 'Email is required.');
      return;
    }
    if (resetCode.trim().length !== 6) {
      Alert.alert('Validation', 'A 6-digit reset code is required.');
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
      await AuthService.resetPassword(resetEmail.trim(), resetCode.trim(), newPassword.trim());
      setNewPassword('');
      setConfirmNewPassword('');
      setResetCode('');
      Alert.alert('Password reset', 'Password updated successfully. Please sign in again on next session.');
    } catch (error: any) {
      Alert.alert('Reset failed', error?.response?.data?.error?.message || 'Unable to reset password.');
    }
  };

  const handleReportVehicle = () => {
    Alert.alert('Report a vehicle', 'Open your active booking and submit a return fault report from Ride details.', [
      { text: 'Stay here', style: 'cancel' },
      { text: 'Go to Ride', onPress: () => tabNavigation.navigate('Ride') },
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
      id: 'security',
      title: 'Account security center',
      description: 'Send an email reset code and reset password with validation.',
      handler: handleOpenSecurity,
    },
    {
      id: 'notifications',
      title: 'Notification center',
      description: 'View booking emails, payment updates, and support ticket alerts.',
      handler: handleOpenNotifications,
    },
    {
      id: 'feedback',
      title: 'Send app feedback',
      description: 'Share an idea, complaint, or service comment with the admin team.',
      handler: handleOpenFeedback,
    },
    {
      id: 'support',
      title: 'Contact support',
      description: `Email ${SUPPORT_EMAIL} for account or billing issues.`,
      handler: handleContactSupport,
    },
  ];

  const shortcutButtons = [
    { id: 'profile', label: 'Profile', icon: 'badge', handler: handleManageProfile },
    { id: 'settings', label: 'Settings', icon: 'settings', handler: handleOpenSettings },
  ];

  const discountEligibility = usageSummaryQuery.data?.discountEligibility;
  const promotionHeadline = getEligibilityHeadline(discountEligibility);
  const userFacingPromotionType = discountEligibility?.eligibleTypes?.find(isUserFacingPromotionType);

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
              <Text style={styles.value}>Age group: {discountEligibility?.ageGroup || profile.ageGroup || 'Not set'}</Text>
              <Text style={styles.value}>
                Promotion: {promotionHeadline}
                {userFacingPromotionType && discountEligibility?.estimatedPercentage ? ` (${discountEligibility.estimatedPercentage}% off)` : ''}
              </Text>
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

        <View style={[styles.card, contrastMode === 'high' && styles.cardHighContrast]}>
          <Text style={[styles.sectionLabel, { fontSize: scaleFont(12) }]}>Account shortcut</Text>
          <Text style={[styles.value, { marginTop: 8 }]}>Trips, payment, and support are now accessible from the main tabs above.</Text>
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
            <Text style={[styles.sectionLabel, styles.modalSectionLabel, { fontSize: scaleFont(12) }]}>Birth date</Text>
            <Pressable style={styles.input} onPress={openBirthdayPicker}>
              <Text style={birthDateInput ? styles.inputValue : styles.inputPlaceholder}>
                {birthDateInput || 'Select birth date'}
              </Text>
            </Pressable>
            <Text style={[styles.modalDescription, { fontSize: scaleFont(12) }]}>
              Used for booking age checks and automatic URBANOVA promotions.
            </Text>
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
            <Text style={[styles.modalDescription, { fontSize: scaleFont(13) }]}>Use the email-code reset flow for protected account operations.</Text>
            <Text style={[styles.sectionLabel, styles.modalSectionLabel, { fontSize: scaleFont(12) }]}>Email</Text>
            <TextInput
              style={styles.input}
              placeholder="Email"
              placeholderTextColor={colors.textMuted}
              autoCapitalize="none"
              value={resetEmail}
              onChangeText={setResetEmail}
            />
            <PrimaryButton label="Send reset code" onPress={handleRequestResetCode} />

            <Text style={[styles.sectionLabel, styles.modalSectionLabel, { fontSize: scaleFont(12) }]}>Verification code</Text>
            <TextInput
              style={styles.input}
              placeholder="6-digit code"
              placeholderTextColor={colors.textMuted}
              value={resetCode}
              keyboardType="number-pad"
              onChangeText={(text) => setResetCode(text.replace(/\D/g, '').slice(0, 6))}
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

      <Modal visible={birthdayPickerVisible} transparent animationType="fade" onRequestClose={() => setBirthdayPickerVisible(false)}>
        <View style={styles.modalBackdrop}>
          <View style={[styles.modalCard, contrastMode === 'high' && styles.modalCardHighContrast]}>
            <Text style={[styles.modalTitle, { fontSize: scaleFont(18) }]}>Select birth date</Text>
            <View style={styles.pickerHeader}>
              <Text style={styles.helperText}>Year</Text>
              <Text style={styles.helperText}>Month</Text>
              <Text style={styles.helperText}>Day</Text>
            </View>
            <View style={styles.pickerRow}>
              <RNScrollView style={styles.pickerColumn} showsVerticalScrollIndicator={false}>
                {birthYearOptions.map((year) => (
                  <PickerItem key={year} label={`${year}`} selected={birthYear === year} onPress={() => setBirthYear(year)} />
                ))}
              </RNScrollView>
              <RNScrollView style={styles.pickerColumn} showsVerticalScrollIndicator={false}>
                {birthMonthLabels.map((month, index) => (
                  <PickerItem key={month} label={month} selected={birthMonth === index} onPress={() => setBirthMonth(index)} />
                ))}
              </RNScrollView>
              <RNScrollView style={styles.pickerColumn} showsVerticalScrollIndicator={false}>
                {birthDayOptions.map((day) => (
                  <PickerItem key={day} label={`${day}`} selected={birthDay === day} onPress={() => setBirthDay(day)} />
                ))}
              </RNScrollView>
            </View>
            <PrimaryButton label="Confirm" onPress={confirmBirthdayPicker} />
            <PrimaryButton label="Close" onPress={() => setBirthdayPickerVisible(false)} style={{ marginTop: 10 }} />
          </View>
        </View>
      </Modal>
    </>
  );
};

const PickerItem = ({ label, selected, onPress }: { label: string; selected: boolean; onPress: () => void }) => (
  <Pressable style={[styles.pickerItem, selected && styles.pickerItemActive]} onPress={onPress}>
    <Text style={[styles.pickerItemLabel, selected && styles.pickerItemLabelActive]}>{label}</Text>
  </Pressable>
);

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
    marginBottom: 20,
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
  inputValue: {
    color: colors.textPrimary,
    fontWeight: '600',
  },
  inputPlaceholder: {
    color: colors.textMuted,
  },
  helperText: {
    color: colors.textSecondary,
    fontSize: 12,
  },
  pickerHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 10,
  },
  pickerRow: {
    flexDirection: 'row',
    marginTop: 8,
    height: 220,
  },
  pickerColumn: {
    flex: 1,
    marginRight: 8,
  },
  pickerItem: {
    height: 40,
    borderRadius: radii.sm,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 8,
    backgroundColor: 'rgba(255,255,255,0.05)',
  },
  pickerItemActive: {
    backgroundColor: 'rgba(131,111,255,0.22)',
    borderWidth: 1,
    borderColor: colors.lime,
  },
  pickerItemLabel: {
    color: colors.textSecondary,
    fontWeight: '600',
  },
  pickerItemLabelActive: {
    color: colors.textPrimary,
  },
});

export default ProfileScreen;
