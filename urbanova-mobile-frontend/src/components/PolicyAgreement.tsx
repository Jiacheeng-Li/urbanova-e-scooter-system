import React from 'react';
import { Modal, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import PrimaryButton from '@components/PrimaryButton';
import { colors, radii } from '@theme/index';

interface Props {
  accepted: boolean;
  visible: boolean;
  onToggle: () => void;
  onOpen: () => void;
  onClose: () => void;
}

const PolicyAgreement: React.FC<Props> = ({ accepted, visible, onToggle, onOpen, onClose }) => (
  <>
    <View style={styles.row}>
      <Pressable style={[styles.checkbox, accepted && styles.checkboxChecked]} onPress={onToggle}>
        {accepted ? <Text style={styles.checkMark}>✓</Text> : null}
      </Pressable>
      <Text style={styles.copy}>
        I have read and agree to the{' '}
        <Text style={styles.link} onPress={onOpen}>
          URBANOVA Rider Policy
        </Text>
        .
      </Text>
    </View>

    <Modal visible={visible} transparent animationType="fade" onRequestClose={onClose}>
      <View style={styles.backdrop}>
        <View style={styles.card}>
          <Text style={styles.title}>URBANOVA Rider Policy</Text>
          <ScrollView style={styles.policyScroll} showsVerticalScrollIndicator>
            <PolicySection
              title="1. Safe riding"
              body="Use URBANOVA vehicles responsibly, follow local traffic rules, wear protective equipment where required, and do not ride under the influence of alcohol or drugs."
            />
            <PolicySection
              title="2. Age and eligibility"
              body="Riders must provide accurate account information. Riders under 12 are not allowed to book or start rides. Age-based offers are applied only when a valid birth date is saved."
            />
            <PolicySection
              title="3. Payments and deposits"
              body="Wallet balance, saved cards, simulated Apple Pay, and Alipay top-ups are used for app testing and booking flows. You are responsible for reviewing ride prices before confirming a booking."
            />
            <PolicySection
              title="4. Vehicle condition"
              body="Check the vehicle before riding. Report faults, damage, low battery, or unsafe conditions through the app and stop using a vehicle if it appears unsafe."
            />
            <PolicySection
              title="5. Parking and returns"
              body="Return vehicles only in valid return zones and avoid blocking roads, entrances, ramps, or pedestrian paths. Abnormal returns may be reviewed by URBANOVA staff."
            />
            <PolicySection
              title="6. Privacy"
              body="URBANOVA uses account, booking, location, payment, and support information to provide rides, apply discounts, improve safety, and manage customer support."
            />
          </ScrollView>
          <PrimaryButton label="I understand" onPress={onClose} />
        </View>
      </View>
    </Modal>
  </>
);

const PolicySection = ({ title, body }: { title: string; body: string }) => (
  <View style={styles.section}>
    <Text style={styles.sectionTitle}>{title}</Text>
    <Text style={styles.sectionBody}>{body}</Text>
  </View>
);

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    marginTop: 8,
    marginBottom: 16,
  },
  checkbox: {
    width: 22,
    height: 22,
    borderRadius: 6,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.42)',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 10,
    marginTop: 1,
  },
  checkboxChecked: {
    backgroundColor: colors.lime,
    borderColor: colors.lime,
  },
  checkMark: {
    color: colors.ink,
    fontWeight: '900',
    lineHeight: 18,
  },
  copy: {
    flex: 1,
    color: colors.textSecondary,
    lineHeight: 20,
  },
  link: {
    color: colors.lime,
    fontWeight: '800',
  },
  backdrop: {
    flex: 1,
    justifyContent: 'center',
    backgroundColor: 'rgba(0,0,0,0.68)',
    paddingHorizontal: 22,
  },
  card: {
    maxHeight: '82%',
    borderRadius: radii.lg,
    padding: 20,
    backgroundColor: colors.graphite,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.14)',
  },
  title: {
    color: colors.textPrimary,
    fontSize: 20,
    fontWeight: '900',
    marginBottom: 12,
  },
  policyScroll: {
    marginBottom: 16,
  },
  section: {
    marginBottom: 14,
  },
  sectionTitle: {
    color: colors.textPrimary,
    fontWeight: '800',
    marginBottom: 4,
  },
  sectionBody: {
    color: colors.textSecondary,
    lineHeight: 20,
  },
});

export default PolicyAgreement;
