import { useQuery } from '@tanstack/react-query';
import { walletTransactions } from '@data/transactions';
import { WalletTransaction } from '@models/index';

const simulateLatency = async <T>(payload: T, ms = 280): Promise<T> =>
  new Promise((resolve) => setTimeout(() => resolve(payload), ms));

export const useTransactions = () => {
  const query = useQuery({
    queryKey: ['wallet-transactions'],
    queryFn: () => simulateLatency<WalletTransaction[]>(walletTransactions),
  });

  return {
    ...query,
    transactions: query.data ?? [],
  };
};
