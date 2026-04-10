import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '@theme/colors';
import RideScreen from '@screens/Ride/RideScreen';
import TripsScreen from '@screens/Trips/TripsScreen';
import WalletScreen from '@screens/Wallet/WalletScreen';
import ProfileScreen from '@screens/Profile/ProfileScreen';
import { MainTabParamList } from '@models/index';

const Tab = createBottomTabNavigator<MainTabParamList>();

const MainTabs = () => (
  <Tab.Navigator
    screenOptions={({ route }) => ({
      headerShown: false,
      tabBarStyle: {
        backgroundColor: colors.graphite,
        borderTopColor: 'rgba(255,255,255,0.08)',
        paddingTop: 4,
      },
      tabBarActiveTintColor: colors.lime,
      tabBarInactiveTintColor: colors.textMuted,
      tabBarIcon: ({ color, size }) => {
        const icons: Record<keyof MainTabParamList, keyof typeof Ionicons.glyphMap> = {
          Ride: 'bicycle-outline',
          Trips: 'time-outline',
          Wallet: 'wallet-outline',
          Profile: 'person-circle-outline',
        };
        return <Ionicons name={icons[route.name as keyof MainTabParamList]} color={color} size={size} />;
      },
    })}
  >
    <Tab.Screen name="Ride" component={RideScreen} />
    <Tab.Screen name="Trips" component={TripsScreen} />
    <Tab.Screen name="Wallet" component={WalletScreen} />
    <Tab.Screen name="Profile" component={ProfileScreen} />
  </Tab.Navigator>
);

export default MainTabs;
