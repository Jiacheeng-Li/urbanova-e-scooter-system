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
        <TouchableOpacity onPress={handleRegister} style={styles.registerLink}>
          <Text style={styles.helper}>
            No account yet?
            <Text style={styles.link}> Create one</Text>
          </Text>
        </TouchableOpacity>
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

export default LoginScreen;
