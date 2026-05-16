import Constants from 'expo-constants';
import { Platform } from 'react-native';
import { BaiduMapManager } from 'react-native-baidu-map-yzg-wu';

type NativePlatform = 'android' | 'ios';

type BaiduMapConfig = {
  androidApiKey?: string;
  iosApiKey?: string;
};

type InitializationResult = {
  ready: boolean;
  error?: string;
};

const extra = (Constants?.expoConfig?.extra as { baiduMap?: BaiduMapConfig } | undefined) ?? {};

let initialized = false;

const getRuntimeApiKey = (platform: NativePlatform): string => {
  if (platform === 'android') {
    return (
      process.env.EXPO_PUBLIC_BAIDU_MAP_ANDROID_AK ||
      extra.baiduMap?.androidApiKey ||
      ''
    ).trim();
  }

  return (
    process.env.EXPO_PUBLIC_BAIDU_MAP_IOS_AK ||
    extra.baiduMap?.iosApiKey ||
    ''
  ).trim();
};

export const getBaiduMapApiKey = (): string => {
  if (Platform.OS !== 'android' && Platform.OS !== 'ios') {
    return '';
  }

  return getRuntimeApiKey(Platform.OS);
};

export const initializeBaiduMap = (): InitializationResult => {
  if (Platform.OS !== 'android' && Platform.OS !== 'ios') {
    return { ready: false };
  }

  if (initialized) {
    return { ready: true };
  }

  const apiKey = getBaiduMapApiKey();
  if (!apiKey) {
    return {
      ready: false,
      error:
        'Baidu Map key is missing. Set EXPO_PUBLIC_BAIDU_MAP_ANDROID_AK and EXPO_PUBLIC_BAIDU_MAP_IOS_AK before building the mobile app.',
    };
  }

  try {
    BaiduMapManager.initSDK(apiKey);
    initialized = true;
    return { ready: true };
  } catch (error) {
    return {
      ready: false,
      error:
        error instanceof Error
          ? error.message
          : 'Failed to initialize Baidu Map SDK.',
    };
  }
};
