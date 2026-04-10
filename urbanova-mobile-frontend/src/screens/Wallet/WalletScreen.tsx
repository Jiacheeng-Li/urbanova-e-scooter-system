import React, { useState } from 'react';
import { Alert, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import ScreenContainer from '@components/ScreenContainer';
import { colors, radii } from '@theme/index';
import { usePasses } from '@hooks/usePasses';
import { useTransactions } from '@hooks/useTransactions';
import PassCard from '@components/PassCard';
import TransactionItem from '@components/TransactionItem';
import PrimaryButton from '@components/PrimaryButton';
import { formatCurrency } from '@utils/format';

const TOP_UP_AMOUNTS = [5, 10, 20, 35];
const PAYMENT_METHODS = [
  { id: 'apple-pay', title: 'Apple Pay', description: 'Instant checkout on iOS' },
  { id: 'visa-4242', title: 'Visa ending 4242', description: 'Expires 06/28' },
];

const WalletScreen = () => {
  const { passes, isLoading } = usePasses();
  const { transactions } = useTransactions();
  const [balance, setBalance] = useState(32.4);
  const [selectedPassId, setSelectedPassId] = useState<string | null>(null);
  const [selectedAmount, setSelectedAmount] = useState<number>(TOP_UP_AMOUNTS[0]);
  const [customAmount, setCustomAmount] = useState('');
  const [paymentMethod, setPaymentMethod] = useState(PAYMENT_METHODS[0].id);
  const [lastTopUp, setLastTopUp] = useState<{ amount: number; method: string } | null>(null);

  const amountFromCustom = customAmount ? parseFloat(customAmount) || 0 : 0;
  const amountToAdd = customAmount ? amountFromCustom : selectedAmount;
  const canTopUp = amountToAdd > 0;

  const paymentMethodLabel = PAYMENT_METHODS.find((method) => method.id === paymentMethod)?.title || 'selected method';

  const handleAmountPress = (amount: number) => {
    setSelectedAmount(amount);
    setCustomAmount('');
  };

  const handleCustomAmount = (value: string) => {
    const sanitized = value.replace(/[^0-9.]/g, '');
    setCustomAmount(sanitized);
    if (sanitized.length > 0) {
      setSelectedAmount(0);
    }
  };

  const handleAddFunds = () => {
    if (!canTopUp) {
      Alert.alert('Select amount', 'Pick a top-up amount before proceeding.');
      return;
    }
    const newBalance = Number((balance + amountToAdd).toFixed(2));
    setBalance(newBalance);
    setLastTopUp({ amount: amountToAdd, method: paymentMethodLabel });
    Alert.alert(
      'Top up successful',
      `${formatCurrency(amountToAdd)} was added with ${paymentMethodLabel}. New balance: ${formatCurrency(newBalance)}.`
    );
  };

  const handleSelectPass = (pass: any) => {
    setSelectedPassId(pass.id);
    Alert.alert('Pass ready', `${pass.name} is selected. You can apply it to your next reservation.`);
  };

  return (
    <ScreenContainer>
      <Text style={styles.title}>Wallet</Text>
      <Text style={styles.subtitle}>Top up URBANOVA Cash, pick a ride pass, and review your payments.</Text>
      <View style={styles.balanceCard}>
        <View>
          <Text style={styles.balanceLabel}>Available balance</Text>
          <Text style={styles.balanceValue}>{formatCurrency(balance)}</Text>
        </View>
        <View style={styles.balanceMeta}>
          <Text style={styles.metaLabel}>Payment method</Text>
          <Text style={styles.metaValue}>{paymentMethodLabel}</Text>
        </View>
      </View>
      <ScrollView contentContainerStyle={{ paddingBottom: 80 }} showsVerticalScrollIndicator={false}>
        <View style={styles.sectionCard}>
          <Text style={styles.sectionTitle}>Top up URBANOVA Cash</Text>
          <Text style={styles.sectionSubtitle}>Choose how much to add and which payment method to use.</Text>
          <View style={styles.paymentList}>
            {PAYMENT_METHODS.map((method) => (
              <Pressable
                key={method.id}
                style={[styles.paymentRow, paymentMethod === method.id && styles.paymentRowActive]}
                onPress={() => setPaymentMethod(method.id)}
              >
                <Text style={styles.paymentTitle}>{method.title}</Text>
                <Text style={styles.paymentSubtitle}>{method.description}</Text>
              </Pressable>
            ))}
          </View>
          <View style={styles.amountRow}>
            {TOP_UP_AMOUNTS.map((amount) => (
              <Pressable
                key={amount}
                style={[styles.amountChip, !customAmount && selectedAmount === amount && styles.amountChipActive]}
                onPress={() => handleAmountPress(amount)}
              >
                <Text style={styles.amountChipLabel}>{formatCurrency(amount)}</Text>
              </Pressable>
            ))}
          </View>
          <TextInput
            style={styles.input}
            placeholder="Or enter another amount"
            placeholderTextColor={colors.textMuted}
            keyboardType="decimal-pad"
            value={customAmount}
            onChangeText={handleCustomAmount}
          />
          <PrimaryButton
            label={canTopUp ? `Add ${formatCurrency(amountToAdd)}` : 'Select amount'}
            onPress={handleAddFunds}
            disabled={!canTopUp}
          />
          {lastTopUp ? (
            <Text style={styles.helper}>
              Last top up: {formatCurrency(lastTopUp.amount)} via {lastTopUp.method}
            </Text>
          ) : null}
        </View>
        <Text style={styles.sectionTitle}>Ride passes</Text>
        {isLoading ? (
          <Text style={styles.helper}>Loading passes...</Text>
        ) : (
          passes.map((pass) => (
            <PassCard key={pass.id} pass={pass} onSelect={handleSelectPass} isSelected={selectedPassId === pass.id} />
          ))
        )}
        {selectedPassId && (
          <Text style={styles.helper}>
            Current selection: {passes.find((p) => p.id === selectedPassId)?.name || selectedPassId}
          </Text>
        )}
        <Text style={styles.sectionTitle}>Transactions</Text>
        <View style={styles.transactions}>
          {transactions.length === 0 ? (
            <Text style={styles.helper}>No transactions yet. Complete a ride to see your history.</Text>
          ) : (
            transactions.map((tx) => <TransactionItem key={tx.id} tx={tx} />)
          )}
        </View>
      </ScrollView>
    </ScreenContainer>
  );
};

const styles = StyleSheet.create({
  title: {
    fontSize: 28,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  subtitle: {
    color: colors.textSecondary,
    marginBottom: 20,
  },
  balanceCard: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: colors.card,
    padding: 20,
    borderRadius: radii.lg,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.08)',
    marginBottom: 24,
  },
  balanceLabel: {
    color: colors.textSecondary,
  },
  balanceValue: {
    marginTop: 8,
    fontSize: 28,
    color: colors.textPrimary,
    fontWeight: '700',
  },
  balanceMeta: {
    alignItems: 'flex-end',
  },
  metaLabel: {
    color: colors.textSecondary,
    fontSize: 12,
  },
  metaValue: {
    color: colors.textPrimary,
    fontWeight: '600',
    marginTop: 4,
  },
  sectionCard: {
    backgroundColor: colors.card,
    borderRadius: radii.lg,
    padding: 20,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.08)',
    marginBottom: 24,
  },
  sectionTitle: {
    color: colors.textPrimary,
    fontSize: 18,
    fontWeight: '700',
    marginTop: 12,
    marginBottom: 12,
  },
  sectionSubtitle: {
    color: colors.textSecondary,
    marginBottom: 16,
  },
  paymentList: {
    marginBottom: 12,
  },
  paymentRow: {
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.08)',
    borderRadius: radii.md,
    padding: 14,
    marginBottom: 12,
  },
  paymentRowActive: {
    borderColor: colors.lime,
    backgroundColor: 'rgba(131,111,255,0.08)',
  },
  paymentTitle: {
    color: colors.textPrimary,
    fontWeight: '600',
  },
  paymentSubtitle: {
    color: colors.textSecondary,
    marginTop: 4,
    fontSize: 13,
  },
  amountRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginBottom: 12,
  },
  amountChip: {
    paddingVertical: 10,
    paddingHorizontal: 16,
    borderRadius: radii.md,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.12)',
    marginRight: 12,
    marginBottom: 12,
  },
  amountChipActive: {
    borderColor: colors.lime,
    backgroundColor: 'rgba(131,111,255,0.12)',
  },
  amountChipLabel: {
    color: colors.textPrimary,
    fontWeight: '600',
  },
  input: {
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.12)',
    borderRadius: radii.md,
    paddingHorizontal: 16,
    paddingVertical: 12,
    color: colors.textPrimary,
    marginBottom: 16,
  },
  transactions: {
    backgroundColor: colors.card,
    borderRadius: radii.lg,
    paddingHorizontal: 16,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.08)',
  },
  helper: {
    color: colors.textSecondary,
    marginBottom: 12,
  },
});

export default WalletScreen;
