import axios, { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import AsyncStorage from '@react-native-async-storage/async-storage';
import Constants from 'expo-constants';
import { Platform } from 'react-native';

const PUBLIC_API_BASE_URL = 'http://${BackendIP}:8080';
const constants = Constants as typeof Constants & {
  manifest?: { extra?: { apiBaseUrl?: string } };
  manifest2?: {
    extra?: {
      apiBaseUrl?: string;
      expoClient?: { extra?: { apiBaseUrl?: string } };
    };
  };
};
const extra =
  (Constants?.expoConfig?.extra as { apiBaseUrl?: string } | undefined) ??
  constants.manifest?.extra ??
  constants.manifest2?.extra?.expoClient?.extra ??
  constants.manifest2?.extra ??
  {};

const getDefaultBaseUrl = () => {
  return PUBLIC_API_BASE_URL;
};

export const API_BASE_URL = (extra.apiBaseUrl || getDefaultBaseUrl()).replace(/\/$/, '');

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
  baseURL: API_BASE_URL,
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

const ACCESS_TOKEN_KEY = 'accessToken';
const REFRESH_TOKEN_KEY = 'refreshToken';

const saveSession = async (payload: AuthPayload) => {
  await AsyncStorage.setItem(ACCESS_TOKEN_KEY, payload.accessToken);
  if (payload.refreshToken) {
    await AsyncStorage.setItem(REFRESH_TOKEN_KEY, payload.refreshToken);
  }
};

const clearSession = async () => {
  await AsyncStorage.multiRemove([ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY]);
};

api.interceptors.request.use(
  async (config: InternalAxiosRequestConfig) => {
    const token = await AsyncStorage.getItem(ACCESS_TOKEN_KEY);
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
      await clearSession();
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
  birthDate?: string;
}

export interface UserProfileData {
  userId: string;
  email: string;
  fullName: string;
  phone: string | null;
  role: string;
  discountCategory: string | null;
  accountStatus: string;
  birthDate?: string | null;
  age?: number | null;
  ageGroup?: string | null;
  createdAt: string;
  updatedAt?: string;
}

export interface AuthPayload {
  sessionId?: string;
  accessToken: string;
  refreshToken?: string;
  tokenType: string;
  expiresInSeconds: number;
  refreshExpiresInSeconds?: number;
  user: UserProfileData;
}

export interface UsageSummaryData {
  userId: string;
  bookingCount: number;
  hoursUsed: number;
  totalSpent: number;
  hoursLast7Days: number;
  frequentUserThresholdHoursPerWeek?: number;
  discountEligibility: DiscountEligibility;
}

export interface EmailVerificationResult {
  email?: string;
  sent?: boolean;
  verified?: boolean;
  expiresAt?: string;
  message?: string;
}

export const AuthService = {
  sendEmailVerification: async (email: string): Promise<EmailVerificationResult> => {
    const response = await api.post<ApiResponse<EmailVerificationResult>>('/api/v1/auth/email-verification/send', {
      email,
    });
    return unwrap(response);
  },
  verifyEmailVerification: async (email: string, code: string): Promise<EmailVerificationResult> => {
    const response = await api.post<ApiResponse<EmailVerificationResult>>('/api/v1/auth/email-verification/verify', {
      email,
      code,
    });
    return unwrap(response);
  },
  login: async (data: LoginRequest): Promise<AuthPayload> => {
    const response = await api.post<ApiResponse<AuthPayload>>('/api/v1/auth/login', data);
    const payload = unwrap(response);
    await saveSession(payload);
    return payload;
  },
  register: async (data: RegisterRequest): Promise<AuthPayload> => {
    const response = await api.post<ApiResponse<AuthPayload>>('/api/v1/auth/register', data);
    const payload = unwrap(response);
    await saveSession(payload);
    return payload;
  },
  refresh: async (refreshToken?: string): Promise<AuthPayload> => {
    const token = refreshToken || (await AsyncStorage.getItem(REFRESH_TOKEN_KEY));
    const response = await api.post<ApiResponse<AuthPayload>>('/api/v1/auth/refresh', { refreshToken: token });
    const payload = unwrap(response);
    await saveSession(payload);
    return payload;
  },
  logout: async (revokeAll = false) => {
    const refreshToken = await AsyncStorage.getItem(REFRESH_TOKEN_KEY);
    try {
      await api.post('/api/v1/auth/logout', revokeAll ? {} : { refreshToken });
    } catch (error) {
      console.warn('Logout request failed, clearing local session anyway', error);
    } finally {
      await clearSession();
    }
  },
  forgotPassword: async (email: string): Promise<{ accepted: boolean; email: string }> => {
    const response = await api.post<ApiResponse<{ accepted: boolean; email: string }>>('/api/v1/auth/password/forgot', {
      email,
    });
    return unwrap(response);
  },
  resetPassword: async (email: string, code: string, newPassword: string) => {
    const response = await api.post<ApiResponse<{ resetAt: string }>>('/api/v1/auth/password/reset', {
      email,
      code,
      newPassword,
    });
    return unwrap(response);
  },
  getProfile: async (): Promise<UserProfileData> => {
    const response = await api.get<ApiResponse<UserProfileData>>('/api/v1/users/me');
    return unwrap(response);
  },
  updateProfile: async (
    payload: Partial<Pick<UserProfileData, 'fullName' | 'phone' | 'discountCategory' | 'birthDate'>>
  ): Promise<UserProfileData> => {
    const response = await api.patch<ApiResponse<UserProfileData>>('/api/v1/users/me', payload);
    return unwrap(response);
  },
  getUsageSummary: async (): Promise<UsageSummaryData> => {
    const response = await api.get<ApiResponse<UsageSummaryData>>('/api/v1/users/me/usage-summary');
    return unwrap(response);
  },
  clearLocalSession: async () => {
    await clearSession();
  },
};

// ============ Payment Methods ============

export interface PaymentMethod {
  paymentMethodId: string;
  userId: string;
  brand: string;
  last4: string;
  expiryMonth: number;
  expiryYear: number;
  label: string | null;
  isDefault: boolean;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePaymentMethodRequest {
  brand: string;
  cardNumber: string;
  expiryMonth: number;
  expiryYear: number;
  label?: string;
  isDefault?: boolean;
}

export const PaymentMethodService = {
  list: async (): Promise<PaymentMethod[]> => {
    const response = await api.get<ApiResponse<PaymentMethod[]>>('/api/v1/payment-methods');
    return unwrap(response);
  },
  create: async (data: CreatePaymentMethodRequest): Promise<PaymentMethod> => {
    const response = await api.post<ApiResponse<PaymentMethod>>('/api/v1/payment-methods', data);
    return unwrap(response);
  },
  update: async (
    paymentMethodId: string,
    data: Partial<Pick<PaymentMethod, 'expiryMonth' | 'expiryYear' | 'label' | 'isDefault'>>
  ): Promise<PaymentMethod> => {
    const response = await api.patch<ApiResponse<PaymentMethod>>(`/api/v1/payment-methods/${paymentMethodId}`, data);
    return unwrap(response);
  },
  remove: async (paymentMethodId: string): Promise<PaymentMethod> => {
    const response = await api.delete<ApiResponse<PaymentMethod>>(`/api/v1/payment-methods/${paymentMethodId}`);
    return unwrap(response);
  },
  setDefault: async (paymentMethodId: string): Promise<PaymentMethod> => {
    const response = await api.post<ApiResponse<PaymentMethod>>(`/api/v1/payment-methods/${paymentMethodId}/default`);
    return unwrap(response);
  },
};

// ============ Wallet ============

export interface WalletAccount {
  walletAccountId: string;
  userId: string;
  balance: number;
  currency: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface WalletTransactionRecord {
  walletTransactionId: string;
  walletAccountId: string;
  userId: string;
  type: string;
  direction: 'CREDIT' | 'DEBIT';
  title: string;
  amount: number;
  currency: string;
  method: string | null;
  paymentMethodId: string | null;
  description: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface WalletTopUpRequest {
  amount: number;
  method: 'APPLE_PAY' | 'ALIPAY' | 'SAVED_CARD';
  paymentMethodId?: string;
}

export const WalletService = {
  getWallet: async (): Promise<WalletAccount> => {
    const response = await api.get<ApiResponse<WalletAccount>>('/api/v1/wallet');
    return unwrap(response);
  },
  listTransactions: async (): Promise<WalletTransactionRecord[]> => {
    const response = await api.get<ApiResponse<WalletTransactionRecord[]>>('/api/v1/wallet/transactions');
    return unwrap(response);
  },
  topUp: async (payload: WalletTopUpRequest): Promise<{ wallet: WalletAccount; transaction: WalletTransactionRecord }> => {
    const response = await api.post<ApiResponse<{ wallet: WalletAccount; transaction: WalletTransactionRecord }>>(
      '/api/v1/wallet/top-ups',
      payload
    );
    return unwrap(response);
  },
};

// ============ Scooters ============

export interface ScooterMapPoint {
  id?: number;
  scooterId: string;
  typeCode?: string;
  typeDisplayName?: string;
  typeImageUrl?: string;
  status: string;
  batteryPercent: number;
  lat: number;
  lng: number;
  zone: string | null;
  distance?: number;
  color?: string | null;
  qrCodeId?: string | null;
  batteryUpdatedAt?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface NearbyScootersResponse {
  code?: number;
  message?: string;
  data?: ScooterMapPoint[];
  count?: number;
  centerLat?: number;
  centerLng?: number;
  radiusKm?: number;
}

export interface UserLocationPayload {
  lat: number;
  lng: number;
  source?: 'CLIENT_GPS' | 'MANUAL' | string;
}

export interface MapViewData {
  userLocation?: UserLocationPayload & { updatedAt?: string };
  scooters?: ScooterMapPoint[];
  [key: string]: unknown;
}

export const UserLocationService = {
  updateLocation: async (payload: UserLocationPayload): Promise<unknown> => {
    const response = await api.post<ApiResponse<unknown>>('/api/v1/users/me/location', payload);
    return unwrap(response);
  },
  getMapView: async (): Promise<MapViewData> => {
    const response = await api.get<ApiResponse<MapViewData>>('/api/v1/users/me/map-view');
    return unwrap(response);
  },
};

export interface ScooterAvailabilitySummary {
  available: number;
  reserved: number;
  inUse: number;
  lowBattery: number;
  maintenance: number;
}

export interface ScooterDetail {
  scooterId: string;
  typeCode?: string;
  typeDisplayName?: string;
  typeImageUrl?: string;
  status: string;
  batteryPercent?: number;
  lat?: number;
  lng?: number;
  zone?: string | null;
  [key: string]: unknown;
}

export interface ScooterQrMetadata {
  scooterId?: string;
  qrCodeId?: string;
  payload?: string;
  imageUrl?: string;
  [key: string]: unknown;
}

export interface QrResolveResult {
  payload?: string;
  qrCodeId?: string;
  canBook?: boolean;
  reason?: string | null;
  scooter?: ScooterDetail;
  scooterId?: string;
  status?: string;
  [key: string]: unknown;
}

export const ScooterService = {
  getMapPoints: async (): Promise<ScooterMapPoint[]> => {
    const response = await api.get<ApiResponse<ScooterMapPoint[]>>('/api/v1/scooters/map-points');
    return unwrap(response);
  },
  getNearby: async (lat: number, lng: number, radiusKm = 5): Promise<ScooterMapPoint[]> => {
    const response = await api.get<NearbyScootersResponse | ApiResponse<ScooterMapPoint[]>>('/location/nearby', {
      params: { lat, lng, radiusKm },
    });
    const body = response.data;
    if (Array.isArray((body as NearbyScootersResponse).data)) {
      return (body as NearbyScootersResponse).data ?? [];
    }
    return unwrap(response as AxiosResponse<ApiResponse<ScooterMapPoint[]>>);
  },
  getDetail: async (scooterId: string): Promise<ScooterDetail> => {
    const response = await api.get<ApiResponse<ScooterDetail>>(`/api/v1/scooters/${scooterId}`);
    return unwrap(response);
  },
  getByStatus: async (status: string): Promise<{ status: string; scooterIds: string[] }> => {
    const response = await api.get<ApiResponse<{ status: string; scooterIds: string[] }>>('/api/v1/scooters/ids', {
      params: { status },
    });
    return unwrap(response);
  },
  getAvailability: async (): Promise<ScooterAvailabilitySummary> => {
    const response = await api.get<ApiResponse<ScooterAvailabilitySummary>>('/api/v1/scooters/availability');
    return unwrap(response);
  },
  getQrMetadata: async (scooterId: string): Promise<ScooterQrMetadata> => {
    const response = await api.get<ApiResponse<ScooterQrMetadata>>(`/api/v1/scooters/${scooterId}/qr`);
    return unwrap(response);
  },
  getByQrCodeId: async (qrCodeId: string): Promise<ScooterDetail> => {
    const response = await api.get<ApiResponse<ScooterDetail>>(`/api/v1/scooters/qr/${qrCodeId}`);
    return unwrap(response);
  },
  resolveQrPayload: async (payload: string): Promise<QrResolveResult> => {
    const response = await api.post<ApiResponse<QrResolveResult>>('/api/v1/scooters/qr/resolve', { payload });
    return unwrap(response);
  },
};

// ============ Bookings ============

export interface CreateBookingRequest {
  scooterId: string;
  hireOptionId: string;
  plannedStartAt?: string;
}

export interface CreateBookingResponse {
  bookingId: string;
  status: string;
  paymentStatus: string;
  scooterStatusSnapshot: string;
  startAt: string | null;
  endAt: string | null;
  priceBreakdown: {
    base: number;
    discount: number;
    finalPrice: number;
  };
}

export interface UpdateBookingRequest {
  scooterId?: string;
  hireOptionId?: string;
  plannedStartAt?: string;
}

export interface BookingDetail {
  bookingId: string;
  bookingRef: string;
  customerType: string;
  userId: string | null;
  guestName?: string | null;
  guestEmail?: string | null;
  guestPhone?: string | null;
  scooterId: string;
  hireOptionId: string;
  status: string;
  startAt: string | null;
  endAt: string | null;
  actualStartAt?: string | null;
  actualEndAt?: string | null;
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
  customerType?: string;
  scooterId: string;
  hireOptionId: string;
  status: string;
  startAt: string | null;
  endAt: string | null;
  createdAt?: string;
  priceFinal: number;
  paymentStatus: string;
  updatedAt: string;
}

export interface CancelBookingResult {
  bookingId: string;
  status: string;
  cancelledAt: string;
}

export interface BookingTimelineEvent {
  eventId: string;
  bookingId: string;
  eventType: string;
  actorUserId: string | null;
  actorRole: string | null;
  details: string | null;
  createdAt: string;
}

export const BookingService = {
  create: async (data: CreateBookingRequest): Promise<CreateBookingResponse> => {
    const response = await api.post<ApiResponse<CreateBookingResponse>>('/api/v1/bookings', data);
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
  update: async (bookingId: string, data: UpdateBookingRequest): Promise<BookingDetail> => {
    const response = await api.patch<ApiResponse<BookingDetail>>(`/api/v1/bookings/${bookingId}`, data);
    return unwrap(response);
  },
  start: async (bookingId: string): Promise<BookingDetail> => {
    const response = await api.post<ApiResponse<BookingDetail>>(`/api/v1/bookings/${bookingId}/start`);
    return unwrap(response);
  },
  end: async (bookingId: string): Promise<BookingDetail> => {
    const response = await api.post<ApiResponse<BookingDetail>>(`/api/v1/bookings/${bookingId}/end`);
    return unwrap(response);
  },
  extend: async (
    bookingId: string,
    payload: { additionalHireOptionCode?: string; additionalHireOptionId?: string }
  ): Promise<{ bookingId: string; oldEndAt: string; newEndAt: string; additionalCharge: number; paymentStatus: string }> => {
    const response = await api.post<
      ApiResponse<{
        bookingId: string;
        oldEndAt: string;
        newEndAt: string;
        additionalCharge: number;
        paymentStatus: string;
      }>
    >(`/api/v1/bookings/${bookingId}/extend`, payload);
    return unwrap(response);
  },
  cancel: async (bookingId: string, reason?: string): Promise<CancelBookingResult> => {
    const response = await api.post<ApiResponse<CancelBookingResult>>(`/api/v1/bookings/${bookingId}/cancel`, { reason });
    return unwrap(response);
  },
  timeline: async (bookingId: string): Promise<BookingTimelineEvent[]> => {
    const response = await api.get<ApiResponse<BookingTimelineEvent[]>>(`/api/v1/bookings/${bookingId}/timeline`);
    return unwrap(response);
  },
};

// ============ Payments ============

export interface PaymentRecord {
  paymentId: string;
  bookingId: string;
  userId: string | null;
  amount: number;
  method: string;
  paymentMethodId: string | null;
  status: string;
  simulatedOutcome: string | null;
  refundedAmount: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePaymentRequest {
  method?: 'SAVED_CARD' | 'ONE_TIME_CARD';
  paymentMethodId?: string;
  amount?: number;
  deferSettlement?: boolean;
  simulatedOutcome?: 'SUCCESS' | 'FAILURE';
}

export const PaymentService = {
  create: async (bookingId: string, payload: CreatePaymentRequest): Promise<PaymentRecord> => {
    const response = await api.post<ApiResponse<PaymentRecord>>(`/api/v1/bookings/${bookingId}/payments`, payload);
    return unwrap(response);
  },
  listByBooking: async (bookingId: string): Promise<PaymentRecord[]> => {
    const response = await api.get<ApiResponse<PaymentRecord[]>>(`/api/v1/bookings/${bookingId}/payments`);
    return unwrap(response);
  },
  getDetail: async (paymentId: string): Promise<PaymentRecord> => {
    const response = await api.get<ApiResponse<PaymentRecord>>(`/api/v1/payments/${paymentId}`);
    return unwrap(response);
  },
};

// ============ Confirmations & Notifications ============

export interface BookingConfirmation {
  confirmationId: string;
  bookingId: string;
  userId: string | null;
  recipientEmail: string | null;
  channel: string;
  status: string;
  message: string;
  resendCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface NotificationRecord {
  notificationId: string;
  type: string;
  title: string;
  message: string;
  read: boolean;
  relatedBookingId: string | null;
  createdAt: string;
  updatedAt: string;
}

export const ConfirmationService = {
  getForBooking: async (bookingId: string): Promise<BookingConfirmation> => {
    const response = await api.get<ApiResponse<BookingConfirmation>>(`/api/v1/bookings/${bookingId}/confirmation`);
    return unwrap(response);
  },
  resend: async (bookingId: string): Promise<BookingConfirmation> => {
    const response = await api.post<ApiResponse<BookingConfirmation>>(`/api/v1/bookings/${bookingId}/confirmation/resend`);
    return unwrap(response);
  },
  listMine: async (): Promise<BookingConfirmation[]> => {
    const response = await api.get<ApiResponse<BookingConfirmation[]>>('/api/v1/confirmations');
    return unwrap(response);
  },
};

export const NotificationService = {
  listMine: async (): Promise<NotificationRecord[]> => {
    const response = await api.get<ApiResponse<NotificationRecord[]>>('/api/v1/notifications');
    return unwrap(response);
  },
  markRead: async (notificationId: string): Promise<NotificationRecord> => {
    const response = await api.patch<ApiResponse<NotificationRecord>>(`/api/v1/notifications/${notificationId}/read`);
    return unwrap(response);
  },
};

// ============ Issues ============

export interface IssueRecord {
  issueId: string;
  reporterUserId: string;
  bookingId: string | null;
  scooterId: string | null;
  issueType?: 'FAULT_REPORT' | 'COMPLAINT' | 'LOW_BATTERY' | 'OTHER';
  title: string;
  description: string;
  priority: 'MEDIUM' | 'HIGH' | 'URGENT' | 'LOW' | 'CRITICAL';
  status: string;
  managerFeedback: string | null;
  photoCount?: number;
  comments?: Array<{ commentId: string; message: string; authorRole?: string; createdAt: string }>;
  photos?: Array<{ photoId: string; originalFileName: string; contentType: string; downloadPath: string; createdAt: string }>;
  createdAt: string;
  updatedAt: string;
}

export interface CreateIssueRequest {
  bookingId?: string;
  scooterId?: string;
  issueType?: 'FAULT_REPORT' | 'COMPLAINT' | 'OTHER';
  title: string;
  description: string;
  priority?: 'MEDIUM' | 'HIGH' | 'URGENT';
}

export const IssueService = {
  create: async (payload: CreateIssueRequest): Promise<IssueRecord> => {
    const response = await api.post<ApiResponse<IssueRecord>>('/api/v1/issues', payload);
    return unwrap(response);
  },
  listMine: async (status?: string): Promise<IssueRecord[]> => {
    const response = await api.get<ApiResponse<IssueRecord[]>>('/api/v1/issues', {
      params: status ? { status } : undefined,
    });
    return unwrap(response);
  },
  getDetail: async (issueId: string): Promise<IssueRecord> => {
    const response = await api.get<ApiResponse<IssueRecord>>(`/api/v1/issues/${issueId}`);
    return unwrap(response);
  },
  addComment: async (issueId: string, message: string): Promise<IssueRecord> => {
    const response = await api.post<ApiResponse<IssueRecord>>(`/api/v1/issues/${issueId}/comments`, { message });
    return unwrap(response);
  },
  uploadPhotos: async (issueId: string, photos: Array<{ uri: string; name?: string; type?: string }>): Promise<unknown> => {
    const formData = new FormData();
    photos.slice(0, 5).forEach((photo, index) => {
      formData.append('files', {
        uri: photo.uri,
        name: photo.name || `issue-photo-${index + 1}.jpg`,
        type: photo.type || 'image/jpeg',
      } as any);
    });
    const response = await api.post<ApiResponse<unknown>>(`/api/v1/issues/${issueId}/photos`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return unwrap(response);
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

export interface AppliedDiscount {
  type: string;
  amount: number;
}

export interface PriceQuote {
  basePrice: number;
  appliedDiscounts: AppliedDiscount[];
  finalPrice: number;
  currency: string;
}

export interface PromotionPolicy {
  promotionPolicyId: string;
  policyCode: string;
  name: string;
  category: string;
  description?: string | null;
  percentage: number;
  minAge?: number | null;
  maxAge?: number | null;
  minCompletedBookings?: number | null;
  maxCompletedBookings?: number | null;
  holidayCampaign?: boolean;
  stackable?: boolean;
  priority?: number;
  active?: boolean;
  activeNow?: boolean;
}

export interface DiscountEligibility {
  userId?: string;
  birthDate?: string | null;
  age?: number | null;
  ageGroup?: string | null;
  completedBookingCount?: number;
  hoursLast7Days?: number;
  frequentUserThresholdHoursPerWeek?: number;
  eligibleTypes?: string[];
  estimatedPercentage?: number;
  activePolicies?: PromotionPolicy[];
}

export const HireOptionService = {
  list: async (): Promise<HireOption[]> => {
    const response = await api.get<ApiResponse<HireOption[]>>('/api/v1/hire-options');
    return unwrap(response);
  },
  quote: async (payload: { scooterId?: string; hireOptionCode: string }): Promise<PriceQuote> => {
    const response = await api.post<ApiResponse<PriceQuote>>('/api/v1/pricing/quotes', payload);
    return unwrap(response);
  },
};

export const DiscountService = {
  getEligibility: async (): Promise<DiscountEligibility> => {
    const response = await api.get<ApiResponse<DiscountEligibility>>('/api/v1/discounts/eligibility');
    return unwrap(response);
  },
};

export { api, ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY };
