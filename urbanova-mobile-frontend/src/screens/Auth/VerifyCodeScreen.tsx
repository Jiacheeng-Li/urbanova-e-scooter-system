import React, { useState } from 'react';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import {
  ActivityIndicator,
  Alert,
  KeyboardAvoidingView,
  Platform,
  Pressable,
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

const RegisterScreen: React.FC<Props> = ({ navigation }) => {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  const [bindCard, setBindCard] = useState(false);
  const [cardBrand, setCardBrand] = useState('VISA');
  const [cardNumber, setCardNumber] = useState('');
  const [cardExpiry, setCardExpiry] = useState('');
  const [cardLabel, setCardLabel] = useState('Primary card');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const setAuthPayload = useAuthStore((state) => state.setAuthPayload);

  const handleRegister = async () => {
    if (!fullName.trim()) {
      setError('Please provide your full name');
      return;
    }
    if (!email.trim()) {
      setError('Please provide your email');
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
        email: email.trim(),
        password: password.trim(),
        fullName: fullName.trim(),
        phone: phone.trim() || undefined,
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
            setError('');
          }}
        />

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

        <PrimaryButton label={loading ? '' : 'Sign up'} onPress={handleRegister} disabled={loading} />
        {loading && <ActivityIndicator color={colors.lime} style={styles.loader} />}

        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backLink}>
          <Text style={styles.helper}>
            Already have an account?
            <Text style={styles.link}> Back to sign in</Text>
          </Text>
        </TouchableOpacity>
      </ScreenContainer>
    </KeyboardAvoidingView>
  );
};

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
  cardBindHeader: {
    marginTop: 16,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
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
});

export default RegisterScreen;
