import { create } from 'zustand';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { AuthPayload, AuthService, UserProfileData } from '@services/api';

interface AuthState {
  isAuthenticated: boolean;
  user: UserProfileData | null;
  city: string;
  setAuthPayload: (payload: AuthPayload) => void;
  setUser: (user: UserProfileData) => void;
  logout: () => Promise<void>;
  checkAuth: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
  isAuthenticated: false,
  user: null,
  city: 'San Francisco',
  setAuthPayload: (payload) => set({ isAuthenticated: true, user: payload.user }),
  setUser: (user) => set({ user }),
  logout: async () => {
    await AsyncStorage.removeItem('accessToken');
    set({ isAuthenticated: false, user: null });
  },
  checkAuth: async () => {
    const token = await AsyncStorage.getItem('accessToken');
    if (!token) {
      set({ isAuthenticated: false, user: null });
      return;
    }
    try {
      const profile = await AuthService.getProfile();
      set({ isAuthenticated: true, user: profile });
    } catch (error) {
      await AsyncStorage.removeItem('accessToken');
      set({ isAuthenticated: false, user: null });
      throw error;
    }
  },
}));
