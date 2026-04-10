import { colors } from './colors';
import { spacing } from './spacing';
import { typography } from './typography';

export const radii = {
  sm: 10,
  md: 16,
  lg: 24,
};

export const shadows = {
  default: {
    shadowColor: '#000',
    shadowOpacity: 0.1,
    shadowRadius: 12,
    shadowOffset: { width: 0, height: 8 },
    elevation: 6,
  },
};

export const theme = {
  colors,
  spacing,
  typography,
  radii,
  shadows,
};

export type Theme = typeof theme;

export { colors, spacing, typography };
