import axios, { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import AsyncStorage from '@react-native-async-storage/async-storage';
import Constants from 'expo-constants';
import { Platform } from 'react-native';

const extra = (Constants?.expoConfig?.extra as { apiBaseUrl?: string } | undefined) ?? {};

const getDefaultBaseUrl = () => {
  if (Platform.OS === 'android') {
    return 'http://10.0.2.2:8080';
  }
  if (Platform.OS === 'ios') {
    return 'http://127.0.0.1:8080';
  }
  return 'http://localhost:8080';
};

const BASE_URL = (extra.apiBaseUrl || getDefaultBaseUrl()).replace(/\/$/, '');

export interface ApiErrorPayload {
  code: string;
  message: string;
  details?: unknown;
}

interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: ApiErrorPayload;
  meta?: {
    requestId: string;
    timestamp: string;
  };
}

const api: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 20000,
  headers: {
    'Content-Type': 'application/json',
  },
});

const unwrap = <T>(response: AxiosResponse<ApiResponse<T>>): T => {
  if (!response.data) {
    throw new Error('Empty response from server');
  }
  if (response.data.success === false) {
    const error = new Error(response.data.error?.message || 'Request failed');
    (error as any).apiError = response.data.error;
    throw error;
  }
  return response.data.data;
};

api.interceptors.request.use(
  async (config: InternalAxiosRequestConfig) => {
    const token = await AsyncStorage.getItem('accessToken');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      await AsyncStorage.removeItem('accessToken');
    }
    return Promise.reject(error);
  }
);

// ============ Auth ============

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
  phone?: string;
}

export interface UserProfileData {
  userId: string;
  email: string;
  fullName: string;
  phone: string | null;
  role: string;
  discountCategory: string | null;
  accountStatus: string;
  createdAt: string;
}

export interface AuthPayload {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  user: UserProfileData;
}

export const AuthService = {
  login: async (data: LoginRequest): Promise<AuthPayload> => {
    const response = await api.post<ApiResponse<AuthPayload>>('/api/v1/auth/login', data);
    const payload = unwrap(response);
    await AsyncStorage.setItem('accessToken', payload.accessToken);
    return payload;
  },
  register: async (data: RegisterRequest): Promise<AuthPayload> => {
    const response = await api.post<ApiResponse<AuthPayload>>('/api/v1/auth/register', data);
    const payload = unwrap(response);
    await AsyncStorage.setItem('accessToken', payload.accessToken);
    return payload;
  },
  getProfile: async (): Promise<UserProfileData> => {
    const response = await api.get<ApiResponse<UserProfileData>>('/api/v1/users/me');
    return unwrap(response);
  },
  logout: async () => {
    await AsyncStorage.removeItem('accessToken');
  },
};

// ============ Scooters ============

export interface ScooterMapPoint {
  scooterId: string;
  status: string;
  batteryPercent: number;
  lat: number;
  lng: number;
  zone: string | null;
}

export const ScooterService = {
  getMapPoints: async (): Promise<ScooterMapPoint[]> => {
    const response = await api.get<ApiResponse<ScooterMapPoint[]>>('/api/v1/scooters/map-points');
    return unwrap(response);
  },
  getByStatus: async (status: string): Promise<{ status: string; scooterIds: string[] }> => {
    const response = await api.get<ApiResponse<{ status: string; scooterIds: string[] }>>('/api/v1/scooters/ids', {
      params: { status },
    });
    return unwrap(response);
  },
};

// ============ Bookings ============

export interface CreateBookingRequest {
  scooterId: string;
  hireOptionId: string;
  plannedStartAt?: string;
}

export interface BookingDetail {
  bookingId: string;
  bookingRef: string;
  customerType: string;
  userId: string;
  scooterId: string;
  hireOptionId: string;
  status: string;
  startAt: string | null;
  endAt: string | null;
  priceBase: number;
  priceDiscount: number;
  priceFinal: number;
  paymentStatus: string;
  cancelReason?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface BookingListItem {
  bookingId: string;
  bookingRef: string;
  scooterId: string;
  hireOptionId: string;
  status: string;
  startAt: string | null;
  endAt: string | null;
  priceFinal: number;
  paymentStatus: string;
  updatedAt: string;
}

export const BookingService = {
  create: async (data: CreateBookingRequest): Promise<BookingDetail> => {
    const response = await api.post<ApiResponse<BookingDetail>>('/api/v1/bookings', data);
    return unwrap(response);
  },
  list: async (status?: string): Promise<BookingListItem[]> => {
    const response = await api.get<ApiResponse<BookingListItem[]>>('/api/v1/bookings', {
      params: status ? { status } : undefined,
    });
    return unwrap(response);
  },
  getDetail: async (bookingId: string): Promise<BookingDetail> => {
    const response = await api.get<ApiResponse<BookingDetail>>(`/api/v1/bookings/${bookingId}`);
    return unwrap(response);
  },
  cancel: async (bookingId: string, reason?: string): Promise<void> => {
    await api.post(`/api/v1/bookings/${bookingId}/cancel`, { reason });
  },
};

// ============ Hire Options ============

export interface HireOption {
  hireOptionId: string;
  code: string;
  durationMinutes: number;
  basePrice: number;
  active: boolean;
}

export const HireOptionService = {
  list: async (): Promise<HireOption[]> => {
    const response = await api.get<ApiResponse<HireOption[]>>('/api/v1/hire-options');
    return unwrap(response);
  },
};

export { api };
