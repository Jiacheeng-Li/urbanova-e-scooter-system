import React from 'react';
import { NavigationContainer, DarkTheme } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';

import { colors } from '@theme/colors';
import { RootStackParamList } from '@models/index';
import { useAuthStore } from '@store/useAuthStore';

import PhoneEntryScreen from '@screens/Auth/PhoneEntryScreen';
import VerifyCodeScreen from '@screens/Auth/VerifyCodeScreen';
import MainTabs from './MainTabs';
import RideDetailScreen from '@screens/Ride/RideDetailScreen';

const Stack = createNativeStackNavigator<RootStackParamList>();

const navTheme = {
  ...DarkTheme,
  colors: {
    ...DarkTheme.colors,
    primary: colors.lime,
    background: colors.ink,
    card: colors.graphite,
    text: colors.textPrimary,
    border: colors.border,
    notification: colors.lime,
  },
};

const AppNavigator = () => {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

  return (
    <NavigationContainer theme={navTheme}>
      <Stack.Navigator screenOptions={{ headerShown: false }}>
        {!isAuthenticated ? (
          <>
            <Stack.Screen name="Login" component={PhoneEntryScreen} />
            <Stack.Screen
              name="Register"
              component={VerifyCodeScreen}
              options={{ presentation: 'modal', headerShown: false }}
            />
          </>
        ) : (
          <>
            <Stack.Screen name="Main" component={MainTabs} />
            <Stack.Screen
              name="RideDetail"
              component={RideDetailScreen}
              options={{
                headerShown: true,
                title: 'Booking Details',
                headerTransparent: true,
                headerTintColor: colors.textPrimary,
                headerStyle: { backgroundColor: 'rgba(5,9,5,0.8)' },
              }}
            />
          </>
        )}
      </Stack.Navigator>
    </NavigationContainer>
  );
};

export default AppNavigator;
