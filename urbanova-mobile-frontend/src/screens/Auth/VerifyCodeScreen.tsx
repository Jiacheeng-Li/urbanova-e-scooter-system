import React, { useState } from 'react';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import {
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
} from 'react-native';
import ScreenContainer from '@components/ScreenContainer';
import PrimaryButton from '@components/PrimaryButton';
import { RootStackParamList } from '@models/index';
import { colors } from '@theme/colors';
import { AuthService } from '@services/api';
import { useAuthStore } from '@store/useAuthStore';

type Props = NativeStackScreenProps<RootStackParamList, 'Register'>;

const RegisterScreen: React.FC<Props> = ({ navigation }) => {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
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
    if (!password.trim() || password.length < 8) {
      setError('Password must be at least 8 characters');
      return;
    }
    if (password !== confirmPassword) {
      setError('Passwords do not match');
      return;
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
    } catch (err: any) {
      const msg = err?.response?.data?.error?.message || 'Registration failed, please try again.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScreenContainer contentStyle={styles.content}>
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

        <Text style={styles.label}>Password (min 8 characters)</Text>
        <TextInput
          style={styles.input}
          placeholder="Enter your password"
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
