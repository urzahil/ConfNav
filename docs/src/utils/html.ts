/** Escapes text for safe insertion into an HTML string (e.g. Leaflet popups and DivIcons). */
export function escapeHtml(value: unknown): string {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

/** Returns the value if it is a #RGB / #RRGGBB colour, otherwise the fallback. */
export function safeColor(value: unknown, fallback = '#D97706'): string {
  const v = String(value ?? '').trim();
  return /^#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6})$/.test(v) ? v : fallback;
}
