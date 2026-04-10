import { Pass } from '@models/index';

export const passes: Pass[] = [
  {
    id: 'pass-lite',
    name: 'LITE 30',
    price: 5.99,
    durationMinutes: 30,
    currency: 'GBP',
    highlight: 'Trial'
  },
  {
    id: 'pass-commuter',
    name: 'COMMUTER 120',
    price: 18.99,
    durationMinutes: 120,
    currency: 'GBP',
    highlight: 'Commuter pick'
  },
  {
    id: 'pass-weekend',
    name: 'WEEKEND 600',
    price: 39.99,
    durationMinutes: 600,
    currency: 'GBP'
  },
];
