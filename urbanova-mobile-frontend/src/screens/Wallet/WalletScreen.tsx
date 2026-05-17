import React, { useEffect, useMemo, useState } from 'react';
import { Alert, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import ScreenContainer from '@components/ScreenContainer';
import { colors, radii } from '@theme/index';
import { usePasses } from '@hooks/usePasses';
import { useTransactions } from '@hooks/useTransactions';
import PassCard from '@components/PassCard';
import TransactionItem from '@components/TransactionItem';
import PrimaryButton from '@components/PrimaryButton';
import { formatCurrency } from '@utils/format';
import {
  DiscountService,
  HireOptionService,
  PaymentMethod,
  PaymentMethodService,
  PriceQuote,
  WalletService,
} from '@services/api';
import { formatCardNumberForInput, maskCard, parseExpiry, sanitizeCardNumber } from '@utils/security';
import { getEligibilityHeadline, getQuoteDiscountSummary, getUserFacingEligibilityType } from '@utils/promotions';
import { useAuthStore } from '@store/useAuthStore';
import { getApiErrorMessage } from '@utils/apiError';

const TOP_UP_AMOUNTS = [5, 10, 20, 35];
const QUICK_PAYMENT_METHODS = [
  { id: 'apple-pay', title: 'Apple Pay', description: 'Instant checkout shell mode' },
  { id: 'alipay', title: 'Alipay', description: 'Quick top-up shell mode' },
  { id: 'saved-card', title: 'Saved Card', description: 'Use your bound credit/debit card' },
] as const;

type TopUpMethod = (typeof QUICK_PAYMENT_METHODS)[number]['id'];

const WalletScreen = () => {
  const userId = useAuthStore((state) => state.user?.userId);
  const queryClient = useQueryClient();
  const { passes, isLoading } = usePasses();
  const { transactions } = useTransactions();

  const walletQuery = useQuery({
    queryKey: ['wallet', userId ?? 'guest'],
    queryFn: WalletService.getWallet,
    enabled: !!userId,
  });

  const paymentMethodsQuery = useQuery({
    queryKey: ['payment-methods', userId ?? 'guest'],
    queryFn: PaymentMethodService.list,
    enabled: !!userId,
  });

  const discountEligibilityQuery = useQuery({
    queryKey: ['discount-eligibility', userId ?? 'guest'],
    queryFn: DiscountService.getEligibility,
    enabled: !!userId,
  });
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
  const balance = Number(walletQuery.data?.balance || 0);
  const defaultMethod = paymentMethods.find((method) => method.isDefault) || paymentMethods[0];
  const [selectedPaymentMethodId, setSelectedPaymentMethodId] = useState<string>('');
  const [cardFormVisible, setCardFormVisible] = useState(paymentMethods.length === 0);

  useEffect(() => {
    if (!selectedPaymentMethodId && defaultMethod) {
      setSelectedPaymentMethodId(defaultMethod.paymentMethodId);
    }
  }, [defaultMethod, selectedPaymentMethodId]);

  useEffect(() => {
    if (paymentMethods.length > 0) {
      setCardFormVisible(false);
    }
  }, [paymentMethods.length]);

  const selectedMethod = useMemo(
    () => paymentMethods.find((method) => method.paymentMethodId === selectedPaymentMethodId),
    [paymentMethods, selectedPaymentMethodId]
  );

  const quoteQuery = useQuery({
    queryKey: ['wallet-price-quotes', userId ?? 'guest', passes.map((pass) => pass.code || pass.name).join(',')],
    queryFn: async () => {
      const entries = await Promise.all(
        passes.map(async (pass) => {
          try {
            const quote = await HireOptionService.quote({ hireOptionCode: pass.code || pass.name });
            return [pass.id, quote] as const;
          } catch {
            return [pass.id, null] as const;
          }
        })
      );
      return Object.fromEntries(entries) as Record<string, PriceQuote | null>;
    },
    enabled: !!userId && passes.length > 0,
  });

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
      await queryClient.invalidateQueries({ queryKey: ['payment-methods', userId ?? 'guest'] });
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
      await queryClient.invalidateQueries({ queryKey: ['payment-methods', userId ?? 'guest'] });
    },
    onError: (error: any) => {
      Alert.alert('Update failed', error?.response?.data?.error?.message || 'Unable to set default card.');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (paymentMethodId: string) => PaymentMethodService.remove(paymentMethodId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['payment-methods', userId ?? 'guest'] });
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
  const userFacingPromotionType = getUserFacingEligibilityType(discountEligibilityQuery.data);

  const topUpMutation = useMutation({
    mutationFn: () =>
      WalletService.topUp({
        amount: amountToAdd,
        method: selectedTopUpMethod === 'saved-card' ? 'SAVED_CARD' : selectedTopUpMethod === 'alipay' ? 'ALIPAY' : 'APPLE_PAY',
        paymentMethodId: selectedTopUpMethod === 'saved-card' ? selectedPaymentMethodId : undefined,
      }),
    onSuccess: async (result) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['wallet', userId ?? 'guest'] }),
        queryClient.invalidateQueries({ queryKey: ['wallet-transactions', userId ?? 'guest'] }),
      ]);
      Alert.alert(
        'Top up successful',
        `${formatCurrency(amountToAdd)} was added with ${
          selectedTopUpMethod === 'saved-card' ? paymentMethodLabel : selectedTopUpLabel
        }. New balance: ${formatCurrency(Number(result.wallet.balance || 0))}.`
      );
    },
    onError: (error: any) => {
      Alert.alert('Top up failed', getApiErrorMessage(error, 'Unable to top up wallet.'));
    },
  });

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

    topUpMutation.mutate();
  };

  const handleSelectPass = (pass: any) => {
    setSelectedPassId(pass.id);
    const summary = getQuoteDiscountSummary(quoteQuery.data?.[pass.id]);
    Alert.alert(
      'Pass ready',
      summary
        ? `${pass.name} is selected with ${summary.label}: ${summary.percent}% off.`
        : `${pass.name} is selected. You can apply it to your next reservation.`
    );
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
      <Text style={styles.subtitle}>Bind cards securely, top up URBANOVA Cash, and review wallet transactions.</Text>
      <View style={styles.balanceCard}>
        <View>
          <Text style={styles.balanceLabel}>Available balance</Text>
          <Text style={styles.balanceValue}>{walletQuery.isLoading ? 'Loading...' : formatCurrency(balance)}</Text>
        </View>
        <View style={styles.balanceMeta}>
          <Text style={styles.metaLabel}>Selected top-up method</Text>
          <Text style={styles.metaValue}>
            {selectedTopUpMethod === 'saved-card' ? paymentMethodLabel : selectedTopUpLabel}
          </Text>
        </View>
      </View>

      <ScrollView style={styles.scrollArea} contentContainerStyle={{ paddingBottom: 220 }} showsVerticalScrollIndicator={false}>
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

          <View style={styles.cardActionsHeader}>
            <Text style={[styles.sectionTitle, { marginTop: 16, marginBottom: 0 }]}>Add card</Text>
            {!cardFormVisible ? (
              <Pressable onPress={() => setCardFormVisible(true)}>
                <Text style={styles.linkText}>Add</Text>
              </Pressable>
            ) : null}
          </View>
          {cardFormVisible ? (
            <>
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
            </>
          ) : (
            <Text style={styles.helper}>{paymentMethods.length > 0 ? 'Your saved card list is ready.' : 'No cards saved yet. Tap Add to bind one.'}</Text>
          )}
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
            label={topUpMutation.isPending ? 'Processing...' : canTopUp ? `Add ${formatCurrency(amountToAdd)}` : 'Select amount'}
            onPress={handleAddFunds}
            disabled={!canTopUp || topUpMutation.isPending}
          />
        </View>

        <View style={styles.sectionCard}>
          <Text style={styles.sectionTitle}>Automatic promotions</Text>
          {discountEligibilityQuery.isLoading ? <Text style={styles.helper}>Checking promotion eligibility...</Text> : null}
          {discountEligibilityQuery.data ? (
            <View style={styles.promoCard}>
              <Text style={styles.promoTitle}>{getEligibilityHeadline(discountEligibilityQuery.data)}</Text>
              <Text style={styles.promoSubtitle}>
                Age group {discountEligibilityQuery.data.ageGroup || 'not set'} | Completed bookings{' '}
                {discountEligibilityQuery.data.completedBookingCount ?? 0}
              </Text>
              <Text style={styles.promoSubtitle}>
                {userFacingPromotionType && discountEligibilityQuery.data.estimatedPercentage
                  ? `${discountEligibilityQuery.data.estimatedPercentage}% estimated discount. Best eligible promotion is applied automatically.`
                  : 'No active discount is available yet.'}
              </Text>
            </View>
          ) : null}

          <Text style={styles.sectionTitle}>Ride passes</Text>
          {isLoading ? (
            <Text style={styles.helper}>Loading passes...</Text>
          ) : (
            passes.map((pass) => {
              const quote = quoteQuery.data?.[pass.id];
              return (
                <PassCard
                  key={pass.id}
                  pass={{ ...pass, quote: quote || undefined }}
                  onSelect={handleSelectPass}
                  isSelected={selectedPassId === pass.id}
                />
              );
            })
          )}
        </View>

        <Text style={styles.sectionTitle}>Transactions</Text>
        <View style={styles.transactions}>
          {transactions.length === 0 ? (
            <Text style={styles.helper}>No wallet transactions yet. Add funds to create your first ledger entry.</Text>
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
  promoCard: {
    borderRadius: radii.md,
    borderWidth: 1,
    borderColor: 'rgba(131,111,255,0.32)',
    padding: 14,
    backgroundColor: 'rgba(131,111,255,0.1)',
    marginBottom: 12,
  },
  promoTitle: {
    color: colors.textPrimary,
    fontSize: 16,
    fontWeight: '800',
  },
  promoSubtitle: {
    color: colors.textSecondary,
    marginTop: 6,
    fontSize: 12,
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
  cardActionsHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
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


