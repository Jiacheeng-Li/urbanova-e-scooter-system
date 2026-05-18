import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { HireOptionService } from '@services/api';
import { Pass } from '@models/index';

export const usePasses = () => {
  const query = useQuery({
    queryKey: ['hire-options'],
    queryFn: HireOptionService.list,
  });

  const passes: Pass[] = useMemo(
    () =>
      (query.data ?? [])
        .filter((option) => option.active)
        .map((option) => ({
          id: option.hireOptionId,
          name: option.code,
          code: option.code,
          price: Number(option.basePrice || 0),
          durationMinutes: option.durationMinutes,
          currency: 'GBP',
          highlight: option.code.toLowerCase().includes('day') ? 'Recommended' : undefined,
        })),
    [query.data]
  );

  return {
    ...query,
    passes,
  };
};
