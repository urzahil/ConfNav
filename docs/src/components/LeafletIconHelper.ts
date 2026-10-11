import L from 'leaflet';

/**
 * Creates a clean SVG DivIcon for Leaflet with custom color and optional text/icon.
 */
export function createCustomMarkerIcon(colorHex: string, label?: string) {
  const safeColor = colorHex || '#D97706';
  const labelText = label ? (label.length > 3 ? label.slice(0, 3) : label) : '';

  const svgHtml = `
    <div style="position: relative; width: 32px; height: 42px; transform: translate(-16px, -42px);">
      <svg viewBox="0 0 32 42" width="32" height="42" fill="none" xmlns="http://www.w3.org/2000/svg" style="filter: drop-shadow(0px 3px 4px rgba(0,0,0,0.35));">
        <path d="M16 0C7.163 0 0 7.163 0 16C0 26.5 16 42 16 42C16 42 32 26.5 32 16C32 7.163 24.837 0 16 0Z" fill="${safeColor}" />
        <circle cx="16" cy="15" r="9" fill="white" />
        ${labelText ? `<text x="16" y="19" font-size="10" font-weight="bold" font-family="sans-serif" text-anchor="middle" fill="${safeColor}">${labelText}</text>` : `<circle cx="16" cy="15" r="5" fill="${safeColor}" />`}
      </svg>
    </div>
  `;

  return L.divIcon({
    className: 'custom-session-marker',
    html: svgHtml,
    iconSize: [32, 42],
    iconAnchor: [16, 42],
    popupAnchor: [0, -38],
  });
}
