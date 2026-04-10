import 'react-native-gesture-handler';
import { StatusBar } from 'expo-status-bar';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { useEffect, useState } from 'react';
import { LogBox, Platform, View, ActivityIndicator } from 'react-native';

import AppNavigator from '@navigation/AppNavigator';
import { useAuthStore } from '@store/useAuthStore';
import { colors } from '@theme/colors';

const queryClient = new QueryClient();

function AppContent() {
  const checkAuth = useAuthStore((state) => state.checkAuth);
  const [isReady, setIsReady] = useState(false);

  useEffect(() => {
    const init = async () => {
      try {
        await checkAuth();
      } catch (error) {
        console.warn('Failed to restore session', error);
      } finally {
        setIsReady(true);
      }
    };
    init();
  }, []);

  if (!isReady) {
    return (
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: colors.ink }}>
        <ActivityIndicator size="large" color={colors.lime} />
      </View>
    );
  }

  return <AppNavigator />;
}

export default function App() {
  useEffect(() => {
    LogBox.ignoreLogs([
      'Sending `onAnimatedValueUpdate` with no listeners registered.',
      'VirtualizedLists should never be nested',
    ]);
  }, []);

  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <SafeAreaProvider>
        <QueryClientProvider client={queryClient}>
          <StatusBar style={Platform.OS === 'ios' ? 'light' : 'auto'} />
          <AppContent />
        </QueryClientProvider>
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}
