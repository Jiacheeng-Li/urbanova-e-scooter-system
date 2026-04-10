import React from 'react';
import { View, Text, StyleSheet } from 'react-native';

interface MapMarkerProps {
  coordinate: { latitude: number; longitude: number };
  children?: React.ReactNode;
}

export const Marker: React.FC<MapMarkerProps> = ({ children }) => {
  return <View>{children}</View>;
};

interface WebMapViewProps {
  style?: any;
  provider?: any;
  initialRegion?: {
    latitude: number;
    longitude: number;
    latitudeDelta: number;
    longitudeDelta: number;
  };
  showsUserLocation?: boolean;
  children?: React.ReactNode;
}

const WebMapView: React.FC<WebMapViewProps> = ({
  style,
  initialRegion,
  children,
}) => {
  return (
    <View style={[styles.container, style]}>
      <View style={styles.mapPlaceholder}>
        <Text style={styles.placeholderIcon}>MAP</Text>
        <Text style={styles.placeholderText}>Map View</Text>
        <Text style={styles.placeholderSubtext}>
          {initialRegion
            ? `${initialRegion.latitude.toFixed(4)}, ${initialRegion.longitude.toFixed(4)}`
            : 'San Francisco, CA'}
        </Text>
      </View>
      <View style={styles.childrenContainer}>{children}</View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0f1b0f',
  },
  mapPlaceholder: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#0f1b0f',
  },
  placeholderIcon: {
    fontSize: 64,
    marginBottom: 16,
  },
  placeholderText: {
    fontSize: 20,
    fontWeight: '600',
    color: '#ffffff',
  },
  placeholderSubtext: {
    fontSize: 14,
    color: '#888888',
    marginTop: 8,
  },
  childrenContainer: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
  },
});

export default WebMapView;
