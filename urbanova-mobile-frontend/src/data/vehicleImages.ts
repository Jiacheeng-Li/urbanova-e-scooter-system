const modelImages: Record<string, number> = {
  ANDROMEDA: require('../../assets/vehicle-models/andromeda.png'),
  GALAXY_SEAT: require('../../assets/vehicle-models/galaxy-seat.png'),
  LUNAR_LITE: require('../../assets/vehicle-models/lunar-lite.png'),
  NEBULA_FAMILY: require('../../assets/vehicle-models/nebula-family.png'),
  ORION_ULTRA: require('../../assets/vehicle-models/orion-ultra.png'),
};

const defaultImage = require('../../assets/vehicle-models/urbanova-default.png');

export const getVehicleModelImage = (modelCode?: string): number => {
  if (!modelCode) {
    return defaultImage;
  }
  return modelImages[modelCode] || defaultImage;
};
