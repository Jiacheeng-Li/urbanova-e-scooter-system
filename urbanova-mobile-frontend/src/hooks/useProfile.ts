import { useQuery } from '@tanstack/react-query';
import { AuthService } from '@services/api';

export const useProfile = () => {
  const query = useQuery({
    queryKey: ['profile'],
    queryFn: AuthService.getProfile,
    retry: false,
  });

  return {
    ...query,
    profile: query.data,
  };
};
