import React, { useState } from 'react';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import {
  ActivityIndicator,
  Alert,
  KeyboardAvoidingView,
  Modal,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import ScreenContainer from '@components/ScreenContainer';
import PrimaryButton from '@components/PrimaryButton';
import { RootStackParamList } from '@models/index';
import { colors } from '@theme/colors';
import { AuthService, PaymentMethodService } from '@services/api';
import { useAuthStore } from '@store/useAuthStore';
import { formatCardNumberForInput, parseExpiry, sanitizeCardNumber, validatePasswordStrength } from '@utils/security';

type Props = NativeStackScreenProps<RootStackParamList, 'Register'>;

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
const CURRENT_YEAR = new Date().getFullYear();
const BIRTH_YEARS = Array.from({ length: 100 }, (_, index) => CURRENT_YEAR - index);

const getDayCount = (year: number, monthIndex: number) => new Date(year, monthIndex + 1, 0).getDate();

const formatBirthDate = (year: number, monthIndex: number, day: number) =>
  `${year}-${String(monthIndex + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;

const RegisterScreen: React.FC<Props> = ({ navigation }) => {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [birthDate, setBirthDate] = useState('');
  const [birthPickerVisible, setBirthPickerVisible] = useState(false);
  const [pickerYear, setPickerYear] = useState(CURRENT_YEAR - 20);
  const [pickerMonth, setPickerMonth] = useState(0);
  const [pickerDay, setPickerDay] = useState(1);
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [verificationCode, setVerificationCode] = useState('');
  const [emailVerified, setEmailVerified] = useState(false);
  const [sendingCode, setSendingCode] = useState(false);
  const [verifyingCode, setVerifyingCode] = useState(false);

  const [bindCard, setBindCard] = useState(false);
  const [cardBrand, setCardBrand] = useState('VISA');
  const [cardNumber, setCardNumber] = useState('');
  const [cardExpiry, setCardExpiry] = useState('');
  const [cardLabel, setCardLabel] = useState('Primary card');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const setAuthPayload = useAuthStore((state) => state.setAuthPayload);

  const normalizedEmail = email.trim().toLowerCase();
  const birthDayOptions = Array.from({ length: getDayCount(pickerYear, pickerMonth) }, (_, index) => index + 1);

  const handleSendVerification = async () => {
    if (!normalizedEmail) {
      setError('Please provide your email before requesting a code');
      return;
    }
    setSendingCode(true);
    setError('');
    try {
      await AuthService.sendEmailVerification(normalizedEmail);
      Alert.alert('Verification code sent', 'Please check your email and enter the 6-digit code.');
    } catch (err: any) {
      setError(err?.response?.data?.error?.message || 'Unable to send verification code.');
    } finally {
      setSendingCode(false);
    }
  };

  const handleVerifyEmail = async () => {
    if (!normalizedEmail || verificationCode.trim().length !== 6) {
      setError('Enter your email and 6-digit verification code');
      return;
    }
    setVerifyingCode(true);
    setError('');
    try {
      const result = await AuthService.verifyEmailVerification(normalizedEmail, verificationCode.trim());
      if (result.verified === false) {
        setEmailVerified(false);
        setError(result.message || 'Verification failed. Please check the code and try again.');
        return;
      }
      setEmailVerified(true);
      Alert.alert('Email verified', 'You can now complete registration.');
    } catch (err: any) {
      setEmailVerified(false);
      setError(err?.response?.data?.error?.message || 'Verification failed.');
    } finally {
      setVerifyingCode(false);
    }
  };

  const openBirthPicker = () => {
    if (birthDate) {
      const parsed = new Date(`${birthDate}T00:00:00`);
      if (!Number.isNaN(parsed.getTime())) {
        setPickerYear(parsed.getFullYear());
        setPickerMonth(parsed.getMonth());
        setPickerDay(parsed.getDate());
      }
    }
    setBirthPickerVisible(true);
  };

  const confirmBirthPicker = () => {
    const selected = new Date(pickerYear, pickerMonth, pickerDay);
    if (selected.getTime() > Date.now()) {
      setError('Birth date cannot be in the future.');
      return;
    }
    setBirthDate(formatBirthDate(pickerYear, pickerMonth, Math.min(pickerDay, getDayCount(pickerYear, pickerMonth))));
    setError('');
    setBirthPickerVisible(false);
  };

  const handleRegister = async () => {
    if (!fullName.trim()) {
      setError('Please provide your full name');
      return;
    }
    if (!email.trim()) {
      setError('Please provide your email');
      return;
    }
    if (!emailVerified) {
      setError('Please verify your email before signing up');
      return;
    }
    if (birthDate.trim() && !/^\d{4}-\d{2}-\d{2}$/.test(birthDate.trim())) {
      setError('Birth date must use YYYY-MM-DD format');
      return;
    }

    const passwordError = validatePasswordStrength(password.trim());
    if (passwordError) {
      setError(passwordError);
      return;
    }
    if (password !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    let expiryMonth = 0;
    let expiryYear = 0;
    if (bindCard) {
      const parsedExpiry = parseExpiry(cardExpiry);
      const digits = sanitizeCardNumber(cardNumber);
      if (!cardBrand.trim()) {
        setError('Card brand is required when binding a card');
        return;
      }
      if (digits.length < 12 || digits.length > 19) {
        setError('Card number must be between 12 and 19 digits');
        return;
      }
      if (!parsedExpiry) {
        setError('Expiry must be in MMYY format');
        return;
      }
      expiryMonth = parsedExpiry.month;
      expiryYear = parsedExpiry.year;
    }

    setError('');
    setLoading(true);
    try {
      const payload = await AuthService.register({
        email: normalizedEmail,
        password: password.trim(),
        fullName: fullName.trim(),
        phone: phone.trim() || undefined,
        birthDate: birthDate.trim() || undefined,
      });
      setAuthPayload(payload);

      if (bindCard) {
        try {
          await PaymentMethodService.create({
            brand: cardBrand.trim().toUpperCase(),
            cardNumber: sanitizeCardNumber(cardNumber),
            expiryMonth,
            expiryYear,
            label: cardLabel.trim() || 'Primary card',
            isDefault: true,
          });
        } catch (cardError: any) {
          const cardErrorMessage = cardError?.response?.data?.error?.message || 'Card binding failed after registration.';
          Alert.alert('Account created', `${cardErrorMessage} You can add the card later in Wallet.`);
        }
      }
    } catch (err: any) {
      const msg = err?.response?.data?.error?.message || 'Registration failed, please try again.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScreenContainer scrollable contentStyle={styles.content}>
        <Text style={styles.title}>Create a URBANOVA account</Text>
        <Text style={styles.subtitle}>Fill in the details below to unlock your rides.</Text>
        {error ? <Text style={styles.errorText}>{error}</Text> : null}

        <Text style={styles.label}>Full name</Text>
        <TextInput
          style={styles.input}
          placeholder="Full name"
          placeholderTextColor={colors.textMuted}
          value={fullName}
          onChangeText={(text) => {
            setFullName(text);
            setError('');
          }}
        />

        <Text style={styles.label}>Email</Text>
        <TextInput
          style={styles.input}
          placeholder="you@email.com"
          placeholderTextColor={colors.textMuted}
          keyboardType="email-address"
          autoCapitalize="none"
          value={email}
          onChangeText={(text) => {
            setEmail(text);
            setEmailVerified(false);
            setError('');
          }}
        />

        <View style={styles.verifyPanel}>
          <View style={styles.verifyHeader}>
            <Text style={styles.verifyTitle}>{emailVerified ? 'Email verified' : 'Email verification required'}</Text>
            <Text style={[styles.verifyStatus, emailVerified && styles.verifyStatusOk]}>
              {emailVerified ? 'Verified' : 'Pending'}
            </Text>
          </View>
          <View style={styles.verifyRow}>
            <TextInput
              style={[styles.input, styles.verifyInput]}
              placeholder="6-digit code"
              placeholderTextColor={colors.textMuted}
              keyboardType="number-pad"
              value={verificationCode}
              onChangeText={(text) => {
                setVerificationCode(text.replace(/\D/g, '').slice(0, 6));
                setEmailVerified(false);
                setError('');
              }}
            />
            <Pressable style={styles.smallButton} onPress={handleSendVerification} disabled={sendingCode}>
              <Text style={styles.smallButtonText}>{sendingCode ? 'Sending...' : 'Send'}</Text>
            </Pressable>
            <Pressable style={styles.smallButton} onPress={handleVerifyEmail} disabled={verifyingCode}>
              <Text style={styles.smallButtonText}>{verifyingCode ? 'Checking...' : 'Verify'}</Text>
            </Pressable>
          </View>
        </View>

        <Text style={styles.label}>Phone number (optional)</Text>
        <TextInput
          style={styles.input}
          placeholder="+44 20 1234 5678"
          placeholderTextColor={colors.textMuted}
          keyboardType="phone-pad"
          value={phone}
          onChangeText={(text) => {
            setPhone(text);
            setError('');
          }}
        />

        <Text style={styles.label}>Birth date (optional)</Text>
        <Pressable style={styles.input} onPress={openBirthPicker}>
          <Text style={birthDate ? styles.dateText : styles.placeholderText}>{birthDate || 'Select birth date'}</Text>
        </Pressable>
        <Text style={styles.fieldHint}>Used for age checks and automatic promotions. Riders under 12 cannot book.</Text>

        <Text style={styles.label}>Password</Text>
        <TextInput
          style={styles.input}
          placeholder="At least 8 chars, A-z, 0-9"
          placeholderTextColor={colors.textMuted}
          secureTextEntry
          value={password}
          onChangeText={(text) => {
            setPassword(text);
            setError('');
          }}
        />

        <Text style={styles.label}>Confirm password</Text>
        <TextInput
          style={styles.input}
          placeholder="Re-enter your password"
          placeholderTextColor={colors.textMuted}
          secureTextEntry
          value={confirmPassword}
          onChangeText={(text) => {
            setConfirmPassword(text);
            setError('');
          }}
        />

        <View style={styles.cardBindHeader}>
          <Text style={styles.label}>Bind a payment card now</Text>
          <Pressable
            onPress={() => {
              setBindCard((prev) => !prev);
              setError('');
            }}
            style={[styles.toggleChip, bindCard && styles.toggleChipActive]}
          >
            <Text style={styles.toggleChipText}>{bindCard ? 'Enabled' : 'Optional'}</Text>
          </Pressable>
        </View>

        {bindCard ? (
          <>
            <Text style={styles.label}>Card brand</Text>
            <TextInput
              style={styles.input}
              placeholder="VISA / MASTERCARD"
              placeholderTextColor={colors.textMuted}
              autoCapitalize="characters"
              value={cardBrand}
              onChangeText={(text) => {
                setCardBrand(text);
                setError('');
              }}
            />

            <Text style={styles.label}>Card number</Text>
            <TextInput
              style={styles.input}
              placeholder="4111 1111 1111 1111"
              placeholderTextColor={colors.textMuted}
              keyboardType="number-pad"
              value={cardNumber}
              onChangeText={(text) => {
                setCardNumber(formatCardNumberForInput(text));
                setError('');
              }}
            />

            <Text style={styles.label}>Expiry (MMYY)</Text>
            <TextInput
              style={styles.input}
              placeholder="1230"
              placeholderTextColor={colors.textMuted}
              keyboardType="number-pad"
              value={cardExpiry}
              onChangeText={(text) => {
                const digits = text.replace(/\D/g, '').slice(0, 4);
                setCardExpiry(digits);
                setError('');
              }}
            />

            <Text style={styles.label}>Card label (optional)</Text>
            <TextInput
              style={styles.input}
              placeholder="Personal card"
              placeholderTextColor={colors.textMuted}
              value={cardLabel}
              onChangeText={(text) => {
                setCardLabel(text);
                setError('');
              }}
            />
          </>
        ) : null}

        <PrimaryButton label={loading ? '' : 'Sign up'} onPress={handleRegister} disabled={loading || !emailVerified} />
        {loading && <ActivityIndicator color={colors.lime} style={styles.loader} />}

        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backLink}>
          <Text style={styles.helper}>
            Already have an account?
            <Text style={styles.link}> Back to sign in</Text>
          </Text>
        </TouchableOpacity>

        <Modal visible={birthPickerVisible} transparent animationType="fade" onRequestClose={() => setBirthPickerVisible(false)}>
          <View style={styles.modalBackdrop}>
            <View style={styles.pickerCard}>
              <Text style={styles.pickerTitle}>Select birth date</Text>
              <View style={styles.pickerHeader}>
                <Text style={styles.pickerHeaderText}>Year</Text>
                <Text style={styles.pickerHeaderText}>Month</Text>
                <Text style={styles.pickerHeaderText}>Day</Text>
              </View>
              <View style={styles.pickerRow}>
                <ScrollView style={styles.pickerColumn} showsVerticalScrollIndicator={false}>
                  {BIRTH_YEARS.map((year) => (
                    <PickerItem key={year} label={`${year}`} selected={pickerYear === year} onPress={() => setPickerYear(year)} />
                  ))}
                </ScrollView>
                <ScrollView style={styles.pickerColumn} showsVerticalScrollIndicator={false}>
                  {MONTHS.map((month, index) => (
                    <PickerItem key={month} label={month} selected={pickerMonth === index} onPress={() => setPickerMonth(index)} />
                  ))}
                </ScrollView>
                <ScrollView style={styles.pickerColumn} showsVerticalScrollIndicator={false}>
                  {birthDayOptions.map((day) => (
                    <PickerItem key={day} label={`${day}`} selected={pickerDay === day} onPress={() => setPickerDay(day)} />
                  ))}
                </ScrollView>
              </View>
              <View style={styles.pickerActions}>
                <PrimaryButton label="Close" onPress={() => setBirthPickerVisible(false)} style={styles.pickerButton} />
                <PrimaryButton label="Confirm" onPress={confirmBirthPicker} style={styles.pickerButton} />
              </View>
            </View>
          </View>
        </Modal>
      </ScreenContainer>
    </KeyboardAvoidingView>
  );
};

const PickerItem = ({ label, selected, onPress }: { label: string; selected: boolean; onPress: () => void }) => (
  <Pressable style={[styles.pickerItem, selected && styles.pickerItemSelected]} onPress={onPress}>
    <Text style={[styles.pickerItemLabel, selected && styles.pickerItemLabelSelected]}>{label}</Text>
  </Pressable>
);

const styles = StyleSheet.create({
  content: {
    paddingTop: 32,
    paddingBottom: 48,
  },
  title: {
    fontSize: 28,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  subtitle: {
    color: colors.textSecondary,
    marginTop: 8,
    marginBottom: 24,
  },
  label: {
    color: colors.textSecondary,
    marginBottom: 8,
    marginTop: 12,
  },
  input: {
    borderRadius: 18,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.16)',
    padding: 16,
    color: colors.textPrimary,
    fontSize: 16,
    backgroundColor: colors.card,
  },
  dateText: {
    color: colors.textPrimary,
    fontSize: 16,
  },
  placeholderText: {
    color: colors.textMuted,
    fontSize: 16,
  },
  cardBindHeader: {
    marginTop: 16,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  verifyPanel: {
    borderRadius: 18,
    borderWidth: 1,
    borderColor: 'rgba(131,111,255,0.35)',
    backgroundColor: 'rgba(131,111,255,0.08)',
    padding: 12,
    marginTop: 12,
    marginBottom: 4,
  },
  verifyHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 10,
  },
  verifyTitle: {
    color: colors.textPrimary,
    fontWeight: '700',
  },
  verifyStatus: {
    color: colors.warning,
    fontSize: 12,
    fontWeight: '700',
  },
  verifyStatusOk: {
    color: colors.success,
  },
  verifyRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  verifyInput: {
    flex: 1,
    marginRight: 8,
  },
  smallButton: {
    borderRadius: 14,
    borderWidth: 1,
    borderColor: colors.lime,
    paddingHorizontal: 10,
    paddingVertical: 12,
    marginLeft: 6,
  },
  smallButtonText: {
    color: colors.lime,
    fontWeight: '700',
    fontSize: 12,
  },
  fieldHint: {
    color: colors.textMuted,
    fontSize: 12,
    marginTop: 6,
    marginBottom: 4,
  },
  toggleChip: {
    borderRadius: 999,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.2)',
    paddingHorizontal: 12,
    paddingVertical: 6,
  },
  toggleChipActive: {
    borderColor: colors.lime,
    backgroundColor: 'rgba(131,111,255,0.12)',
  },
  toggleChipText: {
    color: colors.textPrimary,
    fontWeight: '600',
    fontSize: 12,
  },
  helper: {
    color: colors.textMuted,
    marginTop: 16,
    fontSize: 14,
    textAlign: 'center',
  },
  loader: {
    marginTop: 16,
  },
  backLink: {
    marginTop: 16,
  },
  link: {
    color: colors.lime,
    fontWeight: '600',
  },
  errorText: {
    color: '#ff6b6b',
    marginBottom: 16,
    textAlign: 'center',
    fontSize: 14,
  },
  modalBackdrop: {
    flex: 1,
    justifyContent: 'center',
    backgroundColor: 'rgba(0,0,0,0.65)',
    paddingHorizontal: 20,
  },
  pickerCard: {
    borderRadius: 22,
    padding: 18,
    backgroundColor: colors.graphite,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.12)',
  },
  pickerTitle: {
    color: colors.textPrimary,
    fontSize: 20,
    fontWeight: '800',
  },
  pickerHeader: {
    flexDirection: 'row',
    marginTop: 16,
    marginBottom: 8,
  },
  pickerHeaderText: {
    flex: 1,
    color: colors.textSecondary,
    fontWeight: '700',
    textAlign: 'center',
  },
  pickerRow: {
    flexDirection: 'row',
    height: 220,
  },
  pickerColumn: {
    flex: 1,
    marginHorizontal: 4,
  },
  pickerItem: {
    height: 40,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 8,
    backgroundColor: 'rgba(255,255,255,0.05)',
  },
  pickerItemSelected: {
    backgroundColor: 'rgba(131,111,255,0.22)',
    borderWidth: 1,
    borderColor: colors.lime,
  },
  pickerItemLabel: {
    color: colors.textSecondary,
    fontWeight: '600',
  },
  pickerItemLabelSelected: {
    color: colors.textPrimary,
  },
  pickerActions: {
    flexDirection: 'row',
    marginTop: 14,
    gap: 10,
  },
  pickerButton: {
    flex: 1,
  },
});

export default RegisterScreen;
