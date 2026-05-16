import { useQuery } from '@tanstack/react-query';
import { AuthService } from '@services/api';
import { useAuthStore } from '@store/useAuthStore';

export const useProfile = () => {
  const userId = useAuthStore((state) => state.user?.userId);
  const query = useQuery({
    queryKey: ['profile', userId ?? 'self'],
    queryFn: AuthService.getProfile,
    retry: false,
    enabled: !!userId,
  });

  return {
    ...query,
    profile: query.data,
  };
};
