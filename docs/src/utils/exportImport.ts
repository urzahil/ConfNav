import { ConferenceSession, ExportedSession } from '../types';

/**
 * Strips internal IDs and produces clean JSON adhering to the conference format,
 * preserving any custom extra fields.
 */
export function sanitizeForExport(sessions: ConferenceSession[]): ExportedSession[] {
  return sessions.map((s) => {
    const base: Record<string, unknown> = {
      title: String(s.title || '').trim(),
      speaker: String(s.speaker || '').trim(),
      date: String(s.date || '').trim(),
      startTime: String(s.startTime || '').trim(),
      endTime: String(s.endTime || '').trim(),
      locationName: String(s.locationName || '').trim(),
      address: String(s.address || '').trim(),
      latitude: typeof s.latitude === 'number' && !isNaN(s.latitude) ? s.latitude : Number(s.latitude) || 0,
      longitude: typeof s.longitude === 'number' && !isNaN(s.longitude) ? s.longitude : Number(s.longitude) || 0,
      colorHex: String(s.colorHex || '#CA8A04').trim(),
    };

    if (s.extraFields && typeof s.extraFields === 'object') {
      Object.entries(s.extraFields).forEach(([key, val]) => {
        if (!(key in base) && key !== 'id') {
          base[key] = val;
        }
      });
    }

    return base as ExportedSession;
  });
}

/**
 * Downloads the current sessions as a JSON file.
 */
export function downloadJsonFile(sessions: ConferenceSession[], fileName = 'conference-sessions.json') {
  const cleanData = sanitizeForExport(sessions);
  const jsonString = JSON.stringify(cleanData, null, 2);
  const blob = new Blob([jsonString], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

/**
 * Copies formatted JSON to clipboard.
 */
export async function copyJsonToClipboard(sessions: ConferenceSession[]): Promise<boolean> {
  const cleanData = sanitizeForExport(sessions);
  const jsonString = JSON.stringify(cleanData, null, 2);
  try {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      await navigator.clipboard.writeText(jsonString);
      return true;
    }
    // Fallback for older browsers
    const textArea = document.createElement('textarea');
    textArea.value = jsonString;
    document.body.appendChild(textArea);
    textArea.select();
    const success = document.execCommand('copy');
    document.body.removeChild(textArea);
    return success;
  } catch (err) {
    console.error('Failed to copy JSON:', err);
    return false;
  }
}

export interface ValidationResult {
  valid: boolean;
  sessions: ConferenceSession[];
  errors: string[];
  warnings: string[];
}

/**
 * Validates and converts raw JSON text into ConferenceSession items.
 */
export function validateAndParseSessionsJson(rawText: string): ValidationResult {
  const errors: string[] = [];
  const warnings: string[] = [];

  let parsed: unknown;
  try {
    parsed = JSON.parse(rawText);
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : String(err);
    return {
      valid: false,
      sessions: [],
      errors: [`Invalid JSON syntax: ${message}`],
      warnings: [],
    };
  }

  // Check if it's an array or a single object
  let list: unknown[] = [];
  if (Array.isArray(parsed)) {
    list = parsed;
  } else if (typeof parsed === 'object' && parsed !== null) {
    warnings.push('Single session object detected instead of array. Wrapped into array.');
    list = [parsed];
  } else {
    return {
      valid: false,
      sessions: [],
      errors: ['Input must be a JSON array of session objects or a single session object.'],
      warnings: [],
    };
  }

  if (list.length === 0) {
    return {
      valid: true,
      sessions: [],
      errors: [],
      warnings: ['JSON array is empty.'],
    };
  }

  const validSessions: ConferenceSession[] = [];

  list.forEach((item, index) => {
    const itemNum = index + 1;
    if (typeof item !== 'object' || item === null) {
      errors.push(`Item #${itemNum} is not a valid object.`);
      return;
    }

    const obj = item as Record<string, unknown>;

    // Check properties
    const title = typeof obj.title === 'string' ? obj.title.trim() : String(obj.title || '').trim();
    const speaker = typeof obj.speaker === 'string' ? obj.speaker.trim() : String(obj.speaker || '').trim();
    const date = typeof obj.date === 'string' ? obj.date.trim() : String(obj.date || '').trim();
    const startTime = typeof obj.startTime === 'string' ? obj.startTime.trim() : String(obj.startTime || '').trim();
    const endTime = typeof obj.endTime === 'string' ? obj.endTime.trim() : String(obj.endTime || '').trim();
    const locationName = typeof obj.locationName === 'string' ? obj.locationName.trim() : String(obj.locationName || '').trim();
    const address = typeof obj.address === 'string' ? obj.address.trim() : String(obj.address || '').trim();

    let latitude = Number(obj.latitude);
    if (isNaN(latitude)) {
      warnings.push(`Item #${itemNum} ("${title || 'Untitled'}") has invalid or missing latitude; defaulted to 0.`);
      latitude = 0;
    }

    let longitude = Number(obj.longitude);
    if (isNaN(longitude)) {
      warnings.push(`Item #${itemNum} ("${title || 'Untitled'}") has invalid or missing longitude; defaulted to 0.`);
      longitude = 0;
    }

    let colorHex = typeof obj.colorHex === 'string' ? obj.colorHex.trim() : '#CA8A04';
    if (!colorHex.startsWith('#')) {
      colorHex = `#${colorHex}`;
    }
    if (!/^#[0-9A-Fa-f]{6}$/.test(colorHex) && !/^#[0-9A-Fa-f]{3}$/.test(colorHex)) {
      warnings.push(`Item #${itemNum} color "${colorHex}" is not standard hex; defaulted to #CA8A04.`);
      colorHex = '#CA8A04';
    }

    if (!title) {
      warnings.push(`Item #${itemNum} is missing title.`);
    }

    // Collect any extra properties not in standard fields
    const standardKeys = new Set([
      'title', 'speaker', 'date', 'startTime', 'endTime', 
      'locationName', 'address', 'latitude', 'longitude', 'colorHex', 'id', 'extraFields'
    ]);
    const extraFields: Record<string, unknown> = {};
    Object.keys(obj).forEach((key) => {
      if (!standardKeys.has(key)) {
        extraFields[key] = obj[key];
      }
    });

    validSessions.push({
      id: `session-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`,
      title: title || 'Untitled Session',
      speaker: speaker || 'TBD',
      date: date || new Date().toISOString().split('T')[0],
      startTime: startTime || '09:00',
      endTime: endTime || '10:00',
      locationName: locationName || 'Main Hall',
      address: address || '',
      latitude,
      longitude,
      colorHex,
      ...(Object.keys(extraFields).length > 0 ? { extraFields } : {}),
    });
  });

  return {
    valid: errors.length === 0,
    sessions: validSessions,
    errors,
    warnings,
  };
}

/**
 * Reads a File object as text.
 */
export function readFileAsText(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      resolve(String(e.target?.result || ''));
    };
    reader.onerror = (e) => {
      reject(new Error('Failed to read file: ' + e));
    };
    reader.readAsText(file);
  });
}

/**
 * OpenStreetMap Nominatim Geocoding Lookup helper.
 */
export async function geocodeAddress(query: string): Promise<{ lat: number; lng: number; displayName: string } | null> {
  if (!query || query.trim().length < 3) return null;
  try {
    const url = `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(query)}&limit=1`;
    const res = await fetch(url, {
      headers: {
        'Accept': 'application/json',
      },
    });
    if (!res.ok) return null;
    const data = await res.json();
    if (Array.isArray(data) && data.length > 0) {
      return {
        lat: parseFloat(data[0].lat),
        lng: parseFloat(data[0].lon),
        displayName: data[0].display_name,
      };
    }
    return null;
  } catch (err) {
    console.warn('Geocoding request failed:', err);
    return null;
  }
}
