interface LatLng {
  latitude: number;
  longitude: number;
}

export interface ReturnZoneCheckResult {
  isValid: boolean;
  blockedZoneName?: string;
}

const isFiniteNumber = (value: number) => Number.isFinite(value) && !Number.isNaN(value);

export const isPointInPolygon = (point: LatLng, polygon: LatLng[]): boolean => {
  if (polygon.length < 3) {
    return false;
  }

  let inside = false;
  for (let i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
    const xi = polygon[i].longitude;
    const yi = polygon[i].latitude;
    const xj = polygon[j].longitude;
    const yj = polygon[j].latitude;

    const intersects = yi > point.latitude !== yj > point.latitude &&
      point.longitude < ((xj - xi) * (point.latitude - yi)) / (yj - yi + Number.EPSILON) + xi;

    if (intersects) {
      inside = !inside;
    }
  }
  return inside;
};

export const validateReturnLocation = (
  latitude: number | undefined,
  longitude: number | undefined,
  zones: Array<{ name: string; type: string; vertices: LatLng[] }>
): ReturnZoneCheckResult => {
  if (!isFiniteNumber(latitude ?? NaN) || !isFiniteNumber(longitude ?? NaN)) {
    return {
      isValid: false,
      blockedZoneName: 'Location unavailable',
    };
  }

  const point = { latitude: latitude as number, longitude: longitude as number };

  for (const zone of zones) {
    if (zone.type !== 'no-parking') {
      continue;
    }
    if (isPointInPolygon(point, zone.vertices)) {
      return {
        isValid: false,
        blockedZoneName: zone.name,
      };
    }
  }

  return { isValid: true };
};
