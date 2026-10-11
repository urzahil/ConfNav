export interface ConferenceSession {
  id: string; // Internal identifier for React list management
  title: string;
  speaker: string;
  date: string; // YYYY-MM-DD
  startTime: string; // HH:mm
  endTime: string; // HH:mm
  locationName: string;
  address: string;
  latitude: number;
  longitude: number;
  colorHex: string;
  extraFields?: Record<string, unknown>; // Preserves any additional custom fields from JSON
}

export type ExportedSession = Omit<ConferenceSession, 'id' | 'extraFields'> & Record<string, unknown>;

export const COLOR_PALETTE_PRESETS = [
  { name: 'Gold / Amber', hex: '#CA8A04' },
  { name: 'Amber', hex: '#D97706' },
  { name: 'Blue', hex: '#2563EB' },
  { name: 'Emerald', hex: '#059669' },
  { name: 'Purple', hex: '#7C3AED' },
  { name: 'Rose', hex: '#E11D48' },
  { name: 'Sky', hex: '#0284C7' },
  { name: 'Violet', hex: '#4F46E5' },
  { name: 'Teal', hex: '#0D9488' },
  { name: 'Orange', hex: '#EA580C' },
  { name: 'Slate', hex: '#475569' },
];
