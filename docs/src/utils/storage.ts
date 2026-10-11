import { ConferenceSession } from '../types';

const STORAGE_KEY = 'conference_sessions_v3';

export function loadSavedSessions(): ConferenceSession[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return [];
    }
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed)) {
      return parsed.map((item, index) => ({
        ...item,
        id: item.id || `session-${Date.now()}-${index}`,
      }));
    }
    return [];
  } catch (err) {
    console.error('Error loading saved sessions:', err);
    return [];
  }
}

export function saveSessions(sessions: ConferenceSession[]): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(sessions));
  } catch (err) {
    console.error('Error saving sessions:', err);
  }
}
