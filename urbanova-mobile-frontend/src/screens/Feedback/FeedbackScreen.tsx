import React, { useState } from 'react';
import { Alert, KeyboardAvoidingView, Platform, StyleSheet, Text, TextInput, View } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { LinearGradient } from 'expo-linear-gradient';
import { MaterialIcons } from '@expo/vector-icons';

import ScreenContainer from '@components/ScreenContainer';
import PrimaryButton from '@components/PrimaryButton';
import { IssueService } from '@services/api';
import { colors, radii } from '@theme/index';
import { RootStackParamList } from '@models/index';

const FeedbackScreen = () => {
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const [title, setTitle] = useState('');
  const [message, setMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async () => {
    const cleanTitle = title.trim();
    const cleanMessage = message.trim();

    if (cleanTitle.length < 3) {
      Alert.alert('Missing title', 'Please add a short feedback title.');
      return;
    }
    if (cleanMessage.length < 10) {
      Alert.alert('Missing details', 'Please describe your feedback in at least 10 characters.');
      return;
    }

    setSubmitting(true);
    try {
      const issue = await IssueService.create({
        issueType: 'COMPLAINT',
        title: cleanTitle,
        description: cleanMessage,
      });
      setTitle('');
      setMessage('');
      Alert.alert(
        'Feedback sent',
        `Thanks for helping improve URBANOVA. Reference: ${issue.issueId}`,
        [{ text: 'Done', onPress: () => navigation.goBack() }],
      );
    } catch (error: any) {
      Alert.alert('Send failed', error?.response?.data?.error?.message || 'Unable to submit feedback right now.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <ScreenContainer scrollable contentStyle={styles.content}>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <LinearGradient colors={['rgba(131,111,255,0.38)', 'rgba(28,35,27,0.96)']} style={styles.heroCard}>
          <View style={styles.iconWrap}>
            <MaterialIcons name="rate-review" size={28} color={colors.textPrimary} />
          </View>
          <Text style={styles.kicker}>User feedback</Text>
          <Text style={styles.title}>Tell us what could be better</Text>
          <Text style={styles.description}>
            Share app, booking, payment, or service feedback. Your message is sent directly to the URBANOVA admin dashboard.
          </Text>
        </LinearGradient>

        <View style={styles.card}>
          <Text style={styles.label}>Subject</Text>
          <TextInput
            style={styles.input}
            placeholder="Short feedback title"
            placeholderTextColor={colors.textMuted}
            value={title}
            onChangeText={setTitle}
            maxLength={120}
          />
          <Text style={styles.count}>{title.length}/120</Text>

          <Text style={[styles.label, styles.messageLabel]}>Message</Text>
          <TextInput
            style={[styles.input, styles.textarea]}
            placeholder="Write your feedback here..."
            placeholderTextColor={colors.textMuted}
            value={message}
            onChangeText={setMessage}
            multiline
            textAlignVertical="top"
            maxLength={2000}
          />
          <Text style={styles.count}>{message.length}/2000</Text>
        </View>

        <View style={styles.noteCard}>
          <MaterialIcons name="admin-panel-settings" size={20} color={colors.lime} />
          <Text style={styles.noteText}>Admins can review this under Issue Management as a Feedback ticket.</Text>
        </View>

        <PrimaryButton label="Send feedback" onPress={handleSubmit} isLoading={submitting} disabled={submitting} style={styles.submitButton} />
      </KeyboardAvoidingView>
    </ScreenContainer>
  );
};

const styles = StyleSheet.create({
  content: {
    paddingBottom: 36,
  },
  heroCard: {
    borderRadius: radii.lg,
    padding: 22,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.1)',
    marginBottom: 16,
  },
  iconWrap: {
    width: 52,
    height: 52,
    borderRadius: 26,
    backgroundColor: 'rgba(131,111,255,0.35)',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 14,
  },
  kicker: {
    color: colors.limeMuted,
    fontSize: 12,
    fontWeight: '800',
    letterSpacing: 0.8,
    textTransform: 'uppercase',
  },
  title: {
    color: colors.textPrimary,
    fontSize: 26,
    fontWeight: '800',
    marginTop: 6,
  },
  description: {
    color: colors.textSecondary,
    fontSize: 14,
    lineHeight: 21,
    marginTop: 10,
  },
  card: {
    backgroundColor: colors.card,
    borderRadius: radii.lg,
    padding: 18,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.08)',
  },
  label: {
    color: colors.textSecondary,
    fontSize: 12,
    fontWeight: '700',
    letterSpacing: 0.4,
    textTransform: 'uppercase',
  },
  messageLabel: {
    marginTop: 16,
  },
  input: {
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.14)',
    borderRadius: radii.md,
    color: colors.textPrimary,
    paddingHorizontal: 14,
    paddingVertical: 12,
    marginTop: 8,
    backgroundColor: 'rgba(255,255,255,0.04)',
  },
  textarea: {
    minHeight: 180,
    lineHeight: 21,
  },
  count: {
    color: colors.textMuted,
    fontSize: 11,
    textAlign: 'right',
    marginTop: 6,
  },
  noteCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(131,111,255,0.12)',
    borderColor: 'rgba(131,111,255,0.35)',
    borderWidth: 1,
    borderRadius: radii.md,
    padding: 14,
    marginTop: 14,
  },
  noteText: {
    color: colors.textSecondary,
    flex: 1,
    marginLeft: 10,
    lineHeight: 19,
  },
  submitButton: {
    marginTop: 18,
  },
});

export default FeedbackScreen;
