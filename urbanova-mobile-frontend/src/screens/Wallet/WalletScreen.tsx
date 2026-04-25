import React, { useEffect, useMemo, useState } from 'react';
import { Alert, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { useMutation, useQuery } from '@tanstack/react-query';
import ScreenContainer from '@components/ScreenContainer';
import { colors, radii } from '@theme/index';
import { usePasses } from '@hooks/usePasses';
import { useTransactions } from '@hooks/useTransactions';
import PassCard from '@components/PassCard';
import TransactionItem from '@components/TransactionItem';
import PrimaryButton from '@components/PrimaryButton';
import { formatCurrency } from '@utils/format';
import { PaymentMethod, PaymentMethodService } from '@services/api';
import { formatCardNumberForInput, maskCard, parseExpiry, sanitizeCardNumber } from '@utils/security';

const TOP_UP_AMOUNTS = [5, 10, 20, 35];
const QUICK_PAYMENT_METHODS = [
  { id: 'apple-pay', title: 'Apple Pay', description: 'Instant checkout shell mode' },
  { id: 'alipay', title: 'Alipay', description: 'Quick top-up shell mode' },
  { id: 'saved-card', title: 'Saved Card', description: 'Use your bound credit/debit card' },
] as const;

type TopUpMethod = (typeof QUICK_PAYMENT_METHODS)[number]['id'];

const WalletScreen = () => {
  const { passes, isLoading } = usePasses();
  const { transactions } = useTransactions();

  const paymentMethodsQuery = useQuery({
    queryKey: ['payment-methods'],
    queryFn: PaymentMethodService.list,
  });

  const [balance, setBalance] = useState(32.4);
  const [selectedPassId, setSelectedPassId] = useState<string | null>(null);
  const [selectedAmount, setSelectedAmount] = useState<number>(TOP_UP_AMOUNTS[0]);
  const [customAmount, setCustomAmount] = useState('');
  const [selectedTopUpMethod, setSelectedTopUpMethod] = useState<TopUpMethod>('apple-pay');

  const [brand, setBrand] = useState('VISA');
  const [cardNumber, setCardNumber] = useState('');
  const [expiry, setExpiry] = useState('');
  const [label, setLabel] = useState('Primary card');
  const [setAsDefault, setSetAsDefault] = useState(true);

  const paymentMethods = paymentMethodsQuery.data ?? [];
  const defaultMethod = paymentMethods.find((method) => method.isDefault) || paymentMethods[0];
  const [selectedPaymentMethodId, setSelectedPaymentMethodId] = useState<string>('');

  useEffect(() => {
    if (!selectedPaymentMethodId && defaultMethod) {
      setSelectedPaymentMethodId(defaultMethod.paymentMethodId);
    }
  }, [defaultMethod, selectedPaymentMethodId]);

  const selectedMethod = useMemo(
    () => paymentMethods.find((method) => method.paymentMethodId === selectedPaymentMethodId),
    [paymentMethods, selectedPaymentMethodId]
  );

  const createCardMutation = useMutation({
    mutationFn: () => {
      const parsedExpiry = parseExpiry(expiry);
      const digits = sanitizeCardNumber(cardNumber);

      if (!brand.trim()) {
        throw new Error('Card brand is required.');
      }
      if (digits.length < 12 || digits.length > 19) {
        throw new Error('Card number must be between 12 and 19 digits.');
      }
      if (!parsedExpiry) {
        throw new Error('Expiry must be in MMYY format.');
      }

      return PaymentMethodService.create({
        brand: brand.trim().toUpperCase(),
        cardNumber: digits,
        expiryMonth: parsedExpiry.month,
        expiryYear: parsedExpiry.year,
        label: label.trim() || 'Saved card',
        isDefault: setAsDefault,
      });
    },
    onSuccess: async (created) => {
      setCardNumber('');
      setExpiry('');
      setLabel('Primary card');
      setSelectedPaymentMethodId(created.paymentMethodId);
      await paymentMethodsQuery.refetch();
      Alert.alert('Card added', 'Your card was saved successfully.');
    },
    onError: (error: any) => {
      Alert.alert('Card binding failed', error?.message || error?.response?.data?.error?.message || 'Unable to save card.');
    },
  });

  const setDefaultMutation = useMutation({
    mutationFn: (paymentMethodId: string) => PaymentMethodService.setDefault(paymentMethodId),
    onSuccess: async (updated) => {
      setSelectedPaymentMethodId(updated.paymentMethodId);
      await paymentMethodsQuery.refetch();
    },
    onError: (error: any) => {
      Alert.alert('Update failed', error?.response?.data?.error?.message || 'Unable to set default card.');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (paymentMethodId: string) => PaymentMethodService.remove(paymentMethodId),
    onSuccess: async () => {
      await paymentMethodsQuery.refetch();
      Alert.alert('Card removed', 'Payment method was removed from your account.');
    },
    onError: (error: any) => {
      Alert.alert('Delete failed', error?.response?.data?.error?.message || 'Unable to remove this card.');
    },
  });

  const amountFromCustom = customAmount ? parseFloat(customAmount) || 0 : 0;
  const amountToAdd = customAmount ? amountFromCustom : selectedAmount;
  const canTopUp = amountToAdd > 0;

  const paymentMethodLabel = selectedMethod ? maskCard(selectedMethod.brand, selectedMethod.last4) : 'No card selected';
  const selectedTopUpLabel = QUICK_PAYMENT_METHODS.find((item) => item.id === selectedTopUpMethod)?.title || 'Unknown method';

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
    if (selectedTopUpMethod === 'saved-card' && !selectedMethod) {
      Alert.alert('Select payment method', 'Please bind and select a card first.');
      return;
    }

    const newBalance = Number((balance + amountToAdd).toFixed(2));
    setBalance(newBalance);
    Alert.alert(
      'Top up successful',
      `${formatCurrency(amountToAdd)} was added with ${
        selectedTopUpMethod === 'saved-card' ? paymentMethodLabel : selectedTopUpLabel
      }. New balance: ${formatCurrency(newBalance)}.`
    );
  };

  const handleSelectPass = (pass: any) => {
    setSelectedPassId(pass.id);
    Alert.alert('Pass ready', `${pass.name} is selected. You can apply it to your next reservation.`);
  };

  const handleDeleteCard = (method: PaymentMethod) => {
    Alert.alert('Remove card', `Remove ${maskCard(method.brand, method.last4)}?`, [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Remove', style: 'destructive', onPress: () => deleteMutation.mutate(method.paymentMethodId) },
    ]);
  };

  return (
    <ScreenContainer>
      <Text style={styles.title}>Wallet</Text>
      <Text style={styles.subtitle}>Bind cards securely, top up URBANOVA Cash, and review transactions.</Text>
      <View style={styles.balanceCard}>
        <View>
          <Text style={styles.balanceLabel}>Available balance</Text>
          <Text style={styles.balanceValue}>{formatCurrency(balance)}</Text>
        </View>
        <View style={styles.balanceMeta}>
          <Text style={styles.metaLabel}>Selected top-up method</Text>
          <Text style={styles.metaValue}>
            {selectedTopUpMethod === 'saved-card' ? paymentMethodLabel : selectedTopUpLabel}
          </Text>
        </View>
      </View>

      <ScrollView style={styles.scrollArea} contentContainerStyle={{ paddingBottom: 140 }} showsVerticalScrollIndicator={false}>
        <View style={styles.sectionCard}>
          <Text style={styles.sectionTitle}>Saved cards</Text>
          {paymentMethodsQuery.isLoading ? <Text style={styles.helper}>Loading cards...</Text> : null}
          {paymentMethods.length === 0 && !paymentMethodsQuery.isLoading ? (
            <Text style={styles.helper}>No cards saved yet. Add one below.</Text>
          ) : null}

          {paymentMethods.map((method) => (
            <View key={method.paymentMethodId} style={styles.paymentRow}>
              <Pressable
                style={[styles.paymentInfo, selectedPaymentMethodId === method.paymentMethodId && styles.paymentInfoActive]}
                onPress={() => setSelectedPaymentMethodId(method.paymentMethodId)}
              >
                <Text style={styles.paymentTitle}>{maskCard(method.brand, method.last4)}</Text>
                <Text style={styles.paymentSubtitle}>
                  Exp {String(method.expiryMonth).padStart(2, '0')}/{String(method.expiryYear).slice(-2)}
                  {method.label ? ` • ${method.label}` : ''}
                  {method.isDefault ? ' • Default' : ''}
                </Text>
              </Pressable>
              <View style={styles.paymentActions}>
                {!method.isDefault ? (
                  <Pressable onPress={() => setDefaultMutation.mutate(method.paymentMethodId)}>
                    <Text style={styles.linkText}>Set default</Text>
                  </Pressable>
                ) : null}
                <Pressable onPress={() => handleDeleteCard(method)}>
                  <Text style={styles.dangerText}>Remove</Text>
                </Pressable>
              </View>
            </View>
          ))}

          <Text style={[styles.sectionTitle, { marginTop: 16 }]}>Bind new card</Text>
          <TextInput
            style={styles.input}
            placeholder="Brand (e.g. VISA)"
            placeholderTextColor={colors.textMuted}
            autoCapitalize="characters"
            value={brand}
            onChangeText={setBrand}
          />
          <TextInput
            style={styles.input}
            placeholder="Card number"
            placeholderTextColor={colors.textMuted}
            keyboardType="number-pad"
            value={cardNumber}
            onChangeText={(text) => setCardNumber(formatCardNumberForInput(text))}
          />
          <TextInput
            style={styles.input}
            placeholder="Expiry MMYY"
            placeholderTextColor={colors.textMuted}
            keyboardType="number-pad"
            value={expiry}
            onChangeText={(text) => setExpiry(text.replace(/\D/g, '').slice(0, 4))}
          />
          <TextInput
            style={styles.input}
            placeholder="Label (optional)"
            placeholderTextColor={colors.textMuted}
            value={label}
            onChangeText={setLabel}
          />
          <Pressable style={styles.checkboxRow} onPress={() => setSetAsDefault((prev) => !prev)}>
            <View style={[styles.checkbox, setAsDefault && styles.checkboxChecked]} />
            <Text style={styles.checkboxLabel}>Set as default card</Text>
          </Pressable>
          <PrimaryButton
            label={createCardMutation.isPending ? 'Binding...' : 'Bind card'}
            onPress={() => createCardMutation.mutate()}
            disabled={createCardMutation.isPending}
          />
          <Text style={styles.helper}>Card numbers are masked in the app and only last 4 digits are displayed.</Text>
        </View>

        <View style={styles.sectionCard}>
          <Text style={styles.sectionTitle}>Top up URBANOVA Cash</Text>
          <Text style={styles.sectionSubtitle}>Choose amount and payment method.</Text>
          <View style={styles.methodChooser}>
            {QUICK_PAYMENT_METHODS.map((method) => (
              <Pressable
                key={method.id}
                style={[styles.methodChip, selectedTopUpMethod === method.id && styles.methodChipActive]}
                onPress={() => setSelectedTopUpMethod(method.id)}
              >
                <Text style={styles.methodChipTitle}>{method.title}</Text>
                <Text style={styles.methodChipSubtitle}>{method.description}</Text>
              </Pressable>
            ))}
          </View>
          {selectedTopUpMethod === 'saved-card' && !selectedMethod ? (
            <Text style={styles.helper}>No saved card selected. Bind one above or switch to Apple Pay/Alipay shell mode.</Text>
          ) : null}
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
        </View>

        <Text style={styles.sectionTitle}>Ride passes</Text>
        {isLoading ? (
          <Text style={styles.helper}>Loading passes...</Text>
        ) : (
          passes.map((pass) => (
            <PassCard key={pass.id} pass={pass} onSelect={handleSelectPass} isSelected={selectedPassId === pass.id} />
          ))
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
    maxWidth: '52%',
  },
  metaLabel: {
    color: colors.textSecondary,
    fontSize: 12,
  },
  metaValue: {
    color: colors.textPrimary,
    fontWeight: '600',
    marginTop: 4,
    textAlign: 'right',
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
  scrollArea: {
    flex: 1,
    minHeight: 240,
  },
  paymentRow: {
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.08)',
    borderRadius: radii.md,
    padding: 12,
    marginBottom: 10,
  },
  paymentInfo: {
    borderRadius: radii.sm,
    padding: 10,
    borderWidth: 1,
    borderColor: 'transparent',
  },
  paymentInfoActive: {
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
  paymentActions: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    marginTop: 8,
  },
  methodChooser: {
    marginBottom: 14,
  },
  methodChip: {
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.12)',
    borderRadius: radii.md,
    paddingHorizontal: 12,
    paddingVertical: 10,
    marginBottom: 8,
    backgroundColor: 'rgba(255,255,255,0.03)',
  },
  methodChipActive: {
    borderColor: colors.lime,
    backgroundColor: 'rgba(131,111,255,0.14)',
  },
  methodChipTitle: {
    color: colors.textPrimary,
    fontWeight: '600',
  },
  methodChipSubtitle: {
    marginTop: 2,
    color: colors.textSecondary,
    fontSize: 12,
  },
  linkText: {
    color: colors.lime,
    fontWeight: '600',
    marginRight: 16,
  },
  dangerText: {
    color: colors.danger,
    fontWeight: '600',
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
    marginBottom: 12,
  },
  checkboxRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 14,
  },
  checkbox: {
    width: 18,
    height: 18,
    borderRadius: 4,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.4)',
    marginRight: 10,
  },
  checkboxChecked: {
    backgroundColor: colors.lime,
    borderColor: colors.lime,
  },
  checkboxLabel: {
    color: colors.textSecondary,
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
