export interface Coordinate {
  latitude: number;
  longitude: number;
}

const xPi = (Math.PI * 3000.0) / 180.0;
const a = 6378245.0;
const ee = 0.00669342162296594323;

const transformLat = (x: number, y: number) => {
  let ret =
    -100.0 +
    2.0 * x +
    3.0 * y +
    0.2 * y * y +
    0.1 * x * y +
    0.2 * Math.sqrt(Math.abs(x));
  ret +=
    ((20.0 * Math.sin(6.0 * x * Math.PI) +
      20.0 * Math.sin(2.0 * x * Math.PI)) *
      2.0) /
    3.0;
  ret +=
    ((20.0 * Math.sin(y * Math.PI) +
      40.0 * Math.sin((y / 3.0) * Math.PI)) *
      2.0) /
    3.0;
  ret +=
    ((160.0 * Math.sin((y / 12.0) * Math.PI) +
      320 * Math.sin((y * Math.PI) / 30.0)) *
      2.0) /
    3.0;
  return ret;
};

const transformLng = (x: number, y: number) => {
  let ret =
    300.0 +
    x +
    2.0 * y +
    0.1 * x * x +
    0.1 * x * y +
    0.1 * Math.sqrt(Math.abs(x));
  ret +=
    ((20.0 * Math.sin(6.0 * x * Math.PI) +
      20.0 * Math.sin(2.0 * x * Math.PI)) *
      2.0) /
    3.0;
  ret +=
    ((20.0 * Math.sin(x * Math.PI) +
      40.0 * Math.sin((x / 3.0) * Math.PI)) *
      2.0) /
    3.0;
  ret +=
    ((150.0 * Math.sin((x / 12.0) * Math.PI) +
      300.0 * Math.sin((x / 30.0) * Math.PI)) *
      2.0) /
    3.0;
  return ret;
};

const outOfChina = ({ latitude, longitude }: Coordinate) =>
  longitude < 72.004 ||
  longitude > 137.8347 ||
  latitude < 0.8293 ||
  latitude > 55.8271;

export const wgs84ToGcj02 = (coordinate: Coordinate): Coordinate => {
  if (outOfChina(coordinate)) {
    return coordinate;
  }

  let dLat = transformLat(coordinate.longitude - 105.0, coordinate.latitude - 35.0);
  let dLng = transformLng(coordinate.longitude - 105.0, coordinate.latitude - 35.0);
  const radLat = (coordinate.latitude / 180.0) * Math.PI;
  let magic = Math.sin(radLat);
  magic = 1 - ee * magic * magic;
  const sqrtMagic = Math.sqrt(magic);
  dLat =
    (dLat * 180.0) /
    (((a * (1 - ee)) / (magic * sqrtMagic)) * Math.PI);
  dLng =
    (dLng * 180.0) /
    ((a / sqrtMagic) * Math.cos(radLat) * Math.PI);

  return {
    latitude: coordinate.latitude + dLat,
    longitude: coordinate.longitude + dLng,
  };
};

export const gcj02ToBd09 = (coordinate: Coordinate): Coordinate => {
  const z =
    Math.sqrt(
      coordinate.longitude * coordinate.longitude +
        coordinate.latitude * coordinate.latitude
    ) + 0.00002 * Math.sin(coordinate.latitude * xPi);
  const theta =
    Math.atan2(coordinate.latitude, coordinate.longitude) +
    0.000003 * Math.cos(coordinate.longitude * xPi);

  return {
    longitude: z * Math.cos(theta) + 0.0065,
    latitude: z * Math.sin(theta) + 0.006,
  };
};

export const toBaiduCoordinate = (coordinate: Coordinate): Coordinate =>
  gcj02ToBd09(wgs84ToGcj02(coordinate));
