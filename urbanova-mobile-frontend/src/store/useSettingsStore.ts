import { create } from 'zustand';

type ContrastMode = 'standard' | 'high';

interface SettingsState {
  fontScale: number;
  contrastMode: ContrastMode;
  setFontScale: (scale: number) => void;
  setContrastMode: (mode: ContrastMode) => void;
}

export const useSettingsStore = create<SettingsState>((set) => ({
  fontScale: 1,
  contrastMode: 'standard',
  setFontScale: (scale) => set({ fontScale: scale }),
  setContrastMode: (mode) => set({ contrastMode: mode }),
}));
