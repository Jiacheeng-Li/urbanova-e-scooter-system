import { useQuery } from '@tanstack/react-query';
import { WalletService } from '@services/api';
import { WalletTransaction } from '@models/index';
import { useAuthStore } from '@store/useAuthStore';

export const useTransactions = () => {
  const userId = useAuthStore((state) => state.user?.userId);
  const query = useQuery({
    queryKey: ['wallet-transactions', userId ?? 'guest'],
    queryFn: async () => {
      const records = await WalletService.listTransactions();
      return records.map<WalletTransaction>((tx) => ({
        id: tx.walletTransactionId,
        title: tx.title,
        description: tx.description || tx.method?.replaceAll('_', ' ') || tx.type,
        date: tx.createdAt,
        amount: Number(tx.amount || 0),
        type: tx.direction === 'DEBIT' ? 'debit' : 'credit',
      }));
    },
    enabled: !!userId,
  });

  return {
    ...query,
    transactions: query.data ?? [],
  };
};
