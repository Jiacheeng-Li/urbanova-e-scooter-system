import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { WalletTransaction } from '@models/index';
import { colors, radii } from '@theme/index';
import { formatDate, formatCurrency } from '@utils/format';

interface Props {
  tx: WalletTransaction;
}

const TransactionItem: React.FC<Props> = ({ tx }) => (
  <View style={styles.row}>
    <View style={styles.left}>
      <Text style={styles.title}>{tx.title}</Text>
      <Text style={styles.subtitle}>{tx.description}</Text>
      <Text style={styles.date}>{formatDate(tx.date)}</Text>
    </View>
    <Text style={[styles.amount, tx.type === 'credit' ? styles.credit : styles.debit]}>
      {tx.type === 'credit' ? '+' : '-'}
      {formatCurrency(Math.abs(tx.amount))}
    </Text>
  </View>
);

const styles = StyleSheet.create({
  row: {
    paddingVertical: 16,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: 'rgba(255,255,255,0.08)',
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  left: {
    flex: 1,
    paddingRight: 12,
  },
  title: {
    color: colors.textPrimary,
    fontWeight: '600',
    fontSize: 15,
  },
  subtitle: {
    color: colors.textSecondary,
    marginTop: 4,
    fontSize: 12,
  },
  date: {
    color: colors.textMuted,
    marginTop: 4,
    fontSize: 12,
  },
  amount: {
    fontSize: 15,
    fontWeight: '600',
  },
  credit: {
    color: colors.lime,
  },
  debit: {
    color: colors.textPrimary,
  },
});

export default TransactionItem;
