/**
 * OpenStreetMap Nominatim lookup.
 *
 * Nominatim's usage policy allows at most one request per second, forbids bulk use and
 * requires attribution, so requests are serialised with a minimum gap, identical queries are
 * answered from a cache, and the UI shows NOMINATIM_ATTRIBUTION next to the lookup button.
 */
export const NOMINATIM_ATTRIBUTION = 'Address search by OpenStreetMap Nominatim · © OpenStreetMap contributors';

export interface GeocodeResult {
  lat: number;
  lng: number;
  displayName: string;
}

const MIN_INTERVAL_MS = 1100;
const cache = new Map<string, GeocodeResult | null>();
let queue: Promise<unknown> = Promise.resolve();
let lastRequestAt = 0;

const sleep = (ms: number) => new Promise<void>((resolve) => setTimeout(resolve, ms));

export class GeocodeRateLimitError extends Error {
  constructor() {
    super('The geocoding service is busy. Please wait a moment and try again.');
    this.name = 'GeocodeRateLimitError';
  }
}

async function fetchOne(query: string, signal?: AbortSignal): Promise<GeocodeResult | null> {
  const wait = lastRequestAt + MIN_INTERVAL_MS - Date.now();
  if (wait > 0) await sleep(wait);
  lastRequestAt = Date.now();

  const url = `https://nominatim.openstreetmap.org/search?format=json&limit=1&q=${encodeURIComponent(query)}`;
  const res = await fetch(url, { headers: { Accept: 'application/json' }, signal });
  if (res.status === 429) throw new GeocodeRateLimitError();
  if (!res.ok) return null;

  const data: unknown = await res.json();
  if (!Array.isArray(data) || data.length === 0) return null;
  const first = data[0] as { lat?: string; lon?: string; display_name?: string };
  const lat = parseFloat(first.lat ?? '');
  const lng = parseFloat(first.lon ?? '');
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) return null;
  return { lat, lng, displayName: first.display_name ?? query };
}

/** Looks up one address. Resolves to null when nothing is found; rejects on network errors. */
export function geocodeAddress(query: string, signal?: AbortSignal): Promise<GeocodeResult | null> {
  const key = query.trim().toLowerCase().replace(/\s+/g, ' ');
  if (key.length < 3) return Promise.resolve(null);
  if (cache.has(key)) return Promise.resolve(cache.get(key) ?? null);

  const run = queue.then(async () => {
    if (cache.has(key)) return cache.get(key) ?? null; // an identical request finished while queued
    const result = await fetchOne(key, signal);
    cache.set(key, result);
    return result;
  });
  // Keep the queue alive after a failure so later lookups still run.
  queue = run.catch(() => undefined);
  return run;
}
