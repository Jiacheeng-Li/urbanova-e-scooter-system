import { WalletTransaction } from '@models/index';

export const walletTransactions: WalletTransaction[] = [
  {
    id: 'txn-1',
    title: 'Trip #2041',
    description: 'Hayes Valley to Duboce Park',
    date: '2026-04-03T09:28:00Z',
    amount: -4.7,
    type: 'debit',
  },
  {
    id: 'txn-2',
    title: 'URBANOVA Credit Top Up',
    description: 'Apple Pay ending 4242',
    date: '2026-04-01T12:00:00Z',
    amount: 25,
    type: 'credit',
  },
  {
    id: 'txn-3',
    title: 'Trip #9380',
    description: 'Mission & 16th to South Park',
    date: '2026-04-05T14:38:00Z',
    amount: -5.3,
    type: 'debit',
  },
];
