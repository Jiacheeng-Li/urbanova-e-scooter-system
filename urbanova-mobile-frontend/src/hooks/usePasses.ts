import { useQuery } from '@tanstack/react-query';
import { HireOptionService } from '@services/api';
import { Pass } from '@models/index';

const CNY_TO_GBP = 0.11;
const GBP_THRESHOLD = 40;

const normalizePrice = (value: number) => {
  const numeric = Number(value || 0);
  if (!Number.isFinite(numeric)) {
    return 0;
  }
  if (numeric > GBP_THRESHOLD) {
    return Number((numeric * CNY_TO_GBP).toFixed(2));
  }
  return Number(numeric.toFixed(2));
};

export const usePasses = () => {
  const query = useQuery({
    queryKey: ['hire-options'],
    queryFn: HireOptionService.list,
  });

  const passes: Pass[] = (query.data ?? [])
    .filter((option) => option.active)
    .map((option) => ({
      id: option.hireOptionId,
      name: option.code,
      price: normalizePrice(option.basePrice),
      durationMinutes: option.durationMinutes,
      currency: 'GBP',
      highlight: option.code.toLowerCase().includes('day') ? 'Recommended' : undefined,
    }));

  return {
    ...query,
    passes,
  };
};
