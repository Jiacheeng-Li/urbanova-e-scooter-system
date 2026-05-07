import React, { useState } from 'react';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import {
  ActivityIndicator,
  Alert,
  KeyboardAvoidingView,
  Modal,
  Platform,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import { RootStackParamList } from '@models/index';
import ScreenContainer from '@components/ScreenContainer';
import PrimaryButton from '@components/PrimaryButton';
import { colors } from '@theme/colors';
import { AuthService } from '@services/api';
import { useAuthStore } from '@store/useAuthStore';

type Props = NativeStackScreenProps<RootStackParamList, 'Login'>;

const LoginScreen: React.FC<Props> = ({ navigation }) => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [resetEmail, setResetEmail] = useState('');
  const [resetCode, setResetCode] = useState('');
  const [resetPassword, setResetPassword] = useState('');
  const [resetPasswordConfirm, setResetPasswordConfirm] = useState('');
  const [resetVisible, setResetVisible] = useState(false);
  const [resetLoading, setResetLoading] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const setAuthPayload = useAuthStore((state) => state.setAuthPayload);

  const handleLogin = async () => {
    if (!email.trim()) {
      setError('Please enter your email');
      return;
    }
    if (!password.trim()) {
      setError('Please enter your password');
      return;
    }

    setError('');
    setLoading(true);
    try {
      const payload = await AuthService.login({
        email: email.trim(),
        password: password.trim(),
      });
      setAuthPayload(payload);
    } catch (err: any) {
      const msg = err?.response?.data?.error?.message || 'Login failed, please verify your credentials.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = () => {
    navigation.navigate('Register');
  };

  const handleForgotPassword = async () => {
    if (!resetEmail.trim()) {
      setError('Please enter your email first.');
      return;
    }
    setResetLoading(true);
    setError('');
    try {
      await AuthService.forgotPassword(resetEmail.trim().toLowerCase());
      Alert.alert('Reset code sent', 'Please check your email for the 6-digit reset code.');
    } catch (err: any) {
      setError(err?.response?.data?.error?.message || 'Unable to send reset code.');
    } finally {
      setResetLoading(false);
    }
  };

  const handleResetPassword = async () => {
    if (!resetEmail.trim() || resetCode.trim().length !== 6 || !resetPassword.trim()) {
      setError('Enter email, 6-digit code, and new password.');
      return;
    }
    if (resetPassword !== resetPasswordConfirm) {
      setError('Passwords do not match.');
      return;
    }
    setResetLoading(true);
    setError('');
    try {
      await AuthService.resetPassword(resetEmail.trim().toLowerCase(), resetCode.trim(), resetPassword.trim());
      setResetVisible(false);
      setPassword('');
      setResetCode('');
      setResetPassword('');
      setResetPasswordConfirm('');
      Alert.alert('Password reset', 'Your password has been updated. Please sign in with your new password.');
    } catch (err: any) {
      setError(err?.response?.data?.error?.message || 'Unable to reset password.');
    } finally {
      setResetLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScreenContainer contentStyle={styles.content}>
        <View style={styles.hero}>
          <Text style={styles.title}>Welcome back to URBANOVA</Text>
          <Text style={styles.subtitle}>Sign in to unlock vehicles and manage bookings.</Text>
        </View>
        {error ? <Text style={styles.errorText}>{error}</Text> : null}
        <View style={styles.inputGroup}>
          <Text style={styles.label}>Email</Text>
          <TextInput
            value={email}
            onChangeText={(text) => {
              setEmail(text);
              setError('');
            }}
            keyboardType="email-address"
            placeholder="you@email.com"
            placeholderTextColor={colors.textMuted}
            style={styles.input}
            autoCapitalize="none"
          />
        </View>
        <View style={styles.inputGroup}>
          <Text style={styles.label}>Password</Text>
          <TextInput
            value={password}
            onChangeText={(text) => {
              setPassword(text);
              setError('');
            }}
            secureTextEntry
            placeholder="Enter your password"
            placeholderTextColor={colors.textMuted}
            style={styles.input}
          />
        </View>
        <PrimaryButton label={loading ? '' : 'Sign in'} onPress={handleLogin} disabled={loading} />
        {loading && <ActivityIndicator color={colors.lime} style={styles.loader} />}
        <TouchableOpacity
          onPress={() => {
            setResetEmail(email.trim());
            setResetVisible(true);
            setError('');
          }}
          style={styles.forgotLink}
        >
          <Text style={styles.link}>Forgot password?</Text>
        </TouchableOpacity>
        <TouchableOpacity onPress={handleRegister} style={styles.registerLink}>
          <Text style={styles.helper}>
            No account yet?
            <Text style={styles.link}> Create one</Text>
          </Text>
        </TouchableOpacity>

        <Modal visible={resetVisible} transparent animationType="fade" onRequestClose={() => setResetVisible(false)}>
          <View style={styles.modalBackdrop}>
            <View style={styles.modalCard}>
              <Text style={styles.modalTitle}>Reset password</Text>
              <Text style={styles.modalSubtitle}>URBANOVA will send a 6-digit reset code to your email.</Text>
              <Text style={styles.label}>Email</Text>
              <TextInput
                value={resetEmail}
                onChangeText={setResetEmail}
                keyboardType="email-address"
                autoCapitalize="none"
                placeholder="you@email.com"
                placeholderTextColor={colors.textMuted}
                style={styles.input}
              />
              <PrimaryButton label={resetLoading ? 'Sending...' : 'Send reset code'} onPress={handleForgotPassword} disabled={resetLoading} />
              <Text style={styles.label}>Reset code</Text>
              <TextInput
                value={resetCode}
                onChangeText={(text) => setResetCode(text.replace(/\D/g, '').slice(0, 6))}
                keyboardType="number-pad"
                placeholder="6-digit code"
                placeholderTextColor={colors.textMuted}
                style={styles.input}
              />
              <Text style={styles.label}>New password</Text>
              <TextInput
                value={resetPassword}
                onChangeText={setResetPassword}
                secureTextEntry
                placeholder="New password"
                placeholderTextColor={colors.textMuted}
                style={styles.input}
              />
              <Text style={styles.label}>Confirm password</Text>
              <TextInput
                value={resetPasswordConfirm}
                onChangeText={setResetPasswordConfirm}
                secureTextEntry
                placeholder="Re-enter new password"
                placeholderTextColor={colors.textMuted}
                style={styles.input}
              />
              <PrimaryButton label={resetLoading ? 'Resetting...' : 'Reset password'} onPress={handleResetPassword} disabled={resetLoading} />
              <PrimaryButton label="Close" onPress={() => setResetVisible(false)} style={{ marginTop: 10 }} />
            </View>
          </View>
        </Modal>
      </ScreenContainer>
    </KeyboardAvoidingView>
  );
};

const styles = StyleSheet.create({
  content: {
    justifyContent: 'center',
  },
  hero: {
    marginBottom: 32,
  },
  title: {
    fontSize: 30,
    color: colors.textPrimary,
    fontWeight: '700',
  },
  subtitle: {
    color: colors.textSecondary,
    marginTop: 8,
  },
  inputGroup: {
    marginBottom: 16,
  },
  label: {
    color: colors.textSecondary,
    marginBottom: 8,
  },
  input: {
    borderRadius: 18,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.16)',
    padding: 16,
    color: colors.textPrimary,
    fontSize: 18,
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
  registerLink: {
    marginTop: 16,
  },
  forgotLink: {
    marginTop: 16,
    alignItems: 'center',
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
    paddingHorizontal: 24,
  },
  modalCard: {
    borderRadius: 22,
    padding: 20,
    backgroundColor: colors.graphite,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.12)',
  },
  modalTitle: {
    color: colors.textPrimary,
    fontSize: 20,
    fontWeight: '800',
  },
  modalSubtitle: {
    color: colors.textSecondary,
    marginTop: 6,
    marginBottom: 8,
  },
});

export default LoginScreen;
