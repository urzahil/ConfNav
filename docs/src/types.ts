export interface ConferenceSession {
  id: string; // Internal identifier for React list management
  title: string;
  speaker: string;
  speakerRole?: string; // e.g. "Staff Engineer"
  date: string; // YYYY-MM-DD
  startTime: string; // HH:mm
  endTime: string; // HH:mm
  locationName: string;
  address: string;
  latitude: number;
  longitude: number;
  colorHex: string;
  track?: string; // e.g. "Keynote"
  description?: string;
  extraFields?: Record<string, unknown>; // Preserves any additional custom fields from JSON
}

export type ExportedSession = Omit<ConferenceSession, 'id' | 'extraFields'> & Record<string, unknown>;

/** Keys the app understands; anything else in an imported file is kept under `extraFields`. */
export const STANDARD_SESSION_KEYS = [
  'title',
  'speaker',
  'speakerRole',
  'date',
  'startTime',
  'endTime',
  'locationName',
  'address',
  'latitude',
  'longitude',
  'colorHex',
  'track',
  'description',
] as const;

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
