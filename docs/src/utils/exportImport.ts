import { ConferenceSession, ExportedSession, STANDARD_SESSION_KEYS } from '../types';

export { geocodeAddress } from './geocode';

const DEFAULT_COLOR = '#CA8A04';

/**
 * Strips internal IDs and produces clean JSON adhering to the conference format
 * (see public/sessions.schema.json), preserving any custom extra fields.
 */
export function sanitizeForExport(sessions: ConferenceSession[]): ExportedSession[] {
  return sessions.map((s) => {
    const base: Record<string, unknown> = {
      title: String(s.title || '').trim(),
      speaker: String(s.speaker || '').trim(),
    };
    const speakerRole = String(s.speakerRole || '').trim();
    if (speakerRole) base.speakerRole = speakerRole;

    base.date = String(s.date || '').trim();
    base.startTime = String(s.startTime || '').trim();
    base.endTime = String(s.endTime || '').trim();
    base.locationName = String(s.locationName || '').trim();
    base.address = String(s.address || '').trim();
    base.latitude = typeof s.latitude === 'number' && !isNaN(s.latitude) ? s.latitude : Number(s.latitude) || 0;
    base.longitude = typeof s.longitude === 'number' && !isNaN(s.longitude) ? s.longitude : Number(s.longitude) || 0;
    base.colorHex = String(s.colorHex || DEFAULT_COLOR).trim();

    const track = String(s.track || '').trim();
    if (track) base.track = track;
    const description = String(s.description || '').trim();
    if (description) base.description = description;

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

/** Trimmed text for scalars; JSON null and objects become an empty string. */
function text(value: unknown): string {
  if (typeof value === 'string') return value.trim();
  if (typeof value === 'number' || typeof value === 'boolean') return String(value);
  return '';
}

/** A finite number, also accepting numeric strings; null when absent or invalid. */
function finiteNumber(value: unknown): number | null {
  if (typeof value === 'number') return Number.isFinite(value) ? value : null;
  if (typeof value === 'string' && value.trim() !== '') {
    const n = Number(value);
    return Number.isFinite(n) ? n : null;
  }
  return null;
}

/** Normalises YYYY-M-D to YYYY-MM-DD; null if it is not a real calendar date. */
export function normalizeDate(value: string): string | null {
  const m = /^(\d{4})-(\d{1,2})-(\d{1,2})$/.exec(value.trim());
  if (!m) return null;
  const [y, mo, d] = [Number(m[1]), Number(m[2]), Number(m[3])];
  const date = new Date(Date.UTC(y, mo - 1, d));
  if (date.getUTCFullYear() !== y || date.getUTCMonth() !== mo - 1 || date.getUTCDate() !== d) return null;
  return `${m[1]}-${String(mo).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
}

/** Normalises H:mm to HH:mm; null unless it is a valid 24-hour time. */
export function normalizeTime(value: string): string | null {
  const m = /^(\d{1,2}):(\d{2})$/.exec(value.trim());
  if (!m) return null;
  const [h, min] = [Number(m[1]), Number(m[2])];
  if (h > 23 || min > 59) return null;
  return `${String(h).padStart(2, '0')}:${m[2]}`;
}

/** Returns #RRGGBB (upper case) for #RGB / #RRGGBB input, otherwise null. */
export function normalizeColor(value: string): string | null {
  let v = value.trim();
  if (v && !v.startsWith('#')) v = `#${v}`;
  if (/^#[0-9A-Fa-f]{3}$/.test(v)) v = `#${v[1]}${v[1]}${v[2]}${v[2]}${v[3]}${v[3]}`;
  return /^#[0-9A-Fa-f]{6}$/.test(v) ? v.toUpperCase() : null;
}

/**
 * Validates and converts raw JSON text into ConferenceSession items.
 *
 * The rules match the Android app, so a file accepted here imports there too: rows without a
 * title, a real date, valid start/end times (end after start) are skipped and reported as
 * warnings; missing or out-of-range coordinates keep the row but leave it off the map.
 */
export function validateAndParseSessionsJson(rawText: string): ValidationResult {
  const errors: string[] = [];
  const warnings: string[] = [];

  let parsed: unknown;
  try {
    parsed = JSON.parse(rawText.replace(/^﻿/, ''));
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : String(err);
    return {
      valid: false,
      sessions: [],
      errors: [`Invalid JSON syntax: ${message}`],
      warnings: [],
    };
  }

  let list: unknown[] = [];
  if (Array.isArray(parsed)) {
    list = parsed;
  } else if (typeof parsed === 'object' && parsed !== null) {
    const root = parsed as Record<string, unknown>;
    if (Array.isArray(root.sessions)) {
      list = root.sessions;
    } else if ('title' in root) {
      warnings.push('Single session object detected instead of array. Wrapped into array.');
      list = [root];
    } else {
      return {
        valid: false,
        sessions: [],
        errors: ['JSON object must contain a "sessions" array.'],
        warnings: [],
      };
    }
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
    if (typeof item !== 'object' || item === null || Array.isArray(item)) {
      warnings.push(`Item #${itemNum} skipped: not a valid object.`);
      return;
    }

    const obj = item as Record<string, unknown>;
    const title = text(obj.title);
    if (!title) {
      warnings.push(`Item #${itemNum} skipped: missing title.`);
      return;
    }
    const skip = (reason: string) => {
      warnings.push(`Item #${itemNum} ("${title}") skipped: ${reason}`);
    };

    const date = normalizeDate(text(obj.date));
    if (!date) return skip(`invalid date "${text(obj.date)}" (expected YYYY-MM-DD).`);
    const startTime = normalizeTime(text(obj.startTime));
    if (!startTime) return skip(`invalid start time "${text(obj.startTime)}" (expected HH:mm).`);
    const endTime = normalizeTime(text(obj.endTime));
    if (!endTime) return skip(`invalid end time "${text(obj.endTime)}" (expected HH:mm).`);
    if (endTime <= startTime) return skip(`end time ${endTime} is not after start time ${startTime}.`);

    let latitude = finiteNumber(obj.latitude);
    let longitude = finiteNumber(obj.longitude);
    if (
      latitude === null ||
      longitude === null ||
      Math.abs(latitude) > 90 ||
      Math.abs(longitude) > 180
    ) {
      warnings.push(`Item #${itemNum} ("${title}") has missing or invalid coordinates; it will not appear on the map.`);
      latitude = 0;
      longitude = 0;
    }

    const rawColor = text(obj.colorHex);
    let colorHex = normalizeColor(rawColor);
    if (!colorHex) {
      if (rawColor) warnings.push(`Item #${itemNum} color "${rawColor}" is not standard hex; defaulted to ${DEFAULT_COLOR}.`);
      colorHex = DEFAULT_COLOR;
    }

    // Collect any extra properties not in standard fields
    const standardKeys = new Set<string>([...STANDARD_SESSION_KEYS, 'id', 'extraFields']);
    const extraFields: Record<string, unknown> = {};
    Object.keys(obj).forEach((key) => {
      if (!standardKeys.has(key)) {
        extraFields[key] = obj[key];
      }
    });

    const speakerRole = text(obj.speakerRole);
    const track = text(obj.track);
    const description = text(obj.description);

    validSessions.push({
      id: `session-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`,
      title,
      speaker: text(obj.speaker) || 'TBD',
      ...(speakerRole ? { speakerRole } : {}),
      date,
      startTime,
      endTime,
      locationName: text(obj.locationName) || 'Main Hall',
      address: text(obj.address),
      latitude,
      longitude,
      colorHex,
      ...(track ? { track } : {}),
      ...(description ? { description } : {}),
      ...(Object.keys(extraFields).length > 0 ? { extraFields } : {}),
    });
  });

  if (validSessions.length === 0) {
    errors.push('No valid sessions found in the file.');
  }

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
