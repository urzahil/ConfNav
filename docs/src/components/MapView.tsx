import React, { useEffect, useMemo, useRef } from 'react';
import L from 'leaflet';
import { escapeHtml, safeColor } from '../utils/html';
import { ConferenceSession } from '../types';
import { createCustomMarkerIcon } from './LeafletIconHelper';
import { Maximize2, MapPin } from 'lucide-react';

interface MapViewProps {
  sessions: ConferenceSession[];
  onEditSession: (session: ConferenceSession) => void;
  selectedSessionId?: string | null;
}

interface VenueGroup {
  latitude: number;
  longitude: number;
  sessions: ConferenceSession[];
}

export const MapView: React.FC<MapViewProps> = ({
  sessions,
  onEditSession,
  selectedSessionId,
}) => {
  const mapContainerRef = useRef<HTMLDivElement | null>(null);
  const mapInstanceRef = useRef<L.Map | null>(null);
  const markersLayerRef = useRef<L.LayerGroup | null>(null);

  useEffect(() => {
    if (!mapContainerRef.current) return;

    if (!mapInstanceRef.current) {
      const map = L.map(mapContainerRef.current, {
        center: [45.4067393, 11.8771509],
        zoom: 13,
      });

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; OpenStreetMap contributors',
      }).addTo(map);

      const markersLayer = L.layerGroup().addTo(map);
      markersLayerRef.current = markersLayer;
      mapInstanceRef.current = map;

      setTimeout(() => {
        map.invalidateSize();
      }, 200);
    }

    return () => {
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove();
        mapInstanceRef.current = null;
        markersLayerRef.current = null;
      }
    };
  }, []);

  // One pin per venue: sessions at the same venue share a pin (and its popup) instead of stacking.
  // Sessions without real coordinates ((0, 0) means "not set") are left off the map.
  const plotted = useMemo(() => {
    const groups = new Map<string, VenueGroup>();
    sessions.forEach((session) => {
      if (typeof session.latitude !== 'number' || typeof session.longitude !== 'number') return;
      if (!Number.isFinite(session.latitude) || !Number.isFinite(session.longitude)) return;
      if (session.latitude === 0 && session.longitude === 0) return;
      const name = session.locationName.trim().toLowerCase().replace(/\s+/g, ' ');
      const key = name || `${session.latitude.toFixed(4)},${session.longitude.toFixed(4)}`;
      const group = groups.get(key);
      if (group) {
        group.sessions.push(session);
      } else {
        groups.set(key, { latitude: session.latitude, longitude: session.longitude, sessions: [session] });
      }
    });
    return Array.from(groups.values());
  }, [sessions]);
  const plottedSessionCount = plotted.reduce((n, g) => n + g.sessions.length, 0);

  // Update markers when sessions change
  useEffect(() => {
    const map = mapInstanceRef.current;
    const layer = markersLayerRef.current;
    if (!map || !layer) return;

    layer.clearLayers();

    if (plotted.length === 0) return;

    const bounds = L.latLngBounds([]);

    plotted.forEach((group) => {
      const first = group.sessions[0];
      const marker = L.marker([group.latitude, group.longitude], {
        icon: createCustomMarkerIcon(first.colorHex, first.locationName),
      });

      // Everything interpolated below comes from user-editable or imported JSON, so it is escaped.
      const color = safeColor(first.colorHex);
      const sessionRows = group.sessions
        .map(
          (session) => `
          <div style="border-top: 1px solid #e2e8f0; padding-top: 6px; margin-top: 6px;">
            <div style="font-weight: 700; font-size: 13px; color: #0f172a;">${session.starred ? '<span style="color: #fbbf24;" aria-label="Starred">★</span> ' : ''}${escapeHtml(session.title)}</div>
            <div style="font-size: 11px; color: #475569;">${escapeHtml(session.speaker)}</div>
            <div style="font-size: 11px; color: #64748b; margin-bottom: 4px;">📅 ${escapeHtml(session.date)} | ⏰ ${escapeHtml(session.startTime)} - ${escapeHtml(session.endTime)}</div>
            <button id="edit-session-btn-${escapeHtml(session.id)}" style="width: 100%; padding: 4px 10px; font-size: 12px; font-weight: 600; background: #d97706; color: white; border: none; border-radius: 6px; cursor: pointer;">
              Edit Session
            </button>
          </div>`
        )
        .join('');
      const popupContent = `
        <div style="font-family: inherit; min-width: 180px; max-height: 260px; overflow-y: auto; padding: 4px;">
          <div style="display: flex; align-items: center; gap: 6px; margin-bottom: 2px;">
            <span style="display: inline-block; width: 10px; height: 10px; border-radius: 50%; background-color: ${color};"></span>
            <span style="font-size: 11px; font-weight: 700; color: ${color}; text-transform: uppercase;">${escapeHtml(first.locationName)}</span>
          </div>
          <div style="font-size: 11px; color: #64748b; font-style: italic;">
            📍 ${escapeHtml(first.address || 'No address specified')}
          </div>
          ${sessionRows}
        </div>
      `;

      marker.bindPopup(popupContent);

      marker.on('popupopen', () => {
        group.sessions.forEach((session) => {
          const btn = document.getElementById(`edit-session-btn-${session.id}`);
          if (btn) {
            btn.onclick = () => onEditSession(session);
          }
        });
      });

      marker.addTo(layer);
      bounds.extend([group.latitude, group.longitude]);

      if (selectedSessionId && group.sessions.some((s) => s.id === selectedSessionId)) {
        map.setView([group.latitude, group.longitude], 16);
        marker.openPopup();
      }
    });

    if (bounds.isValid() && !selectedSessionId) {
      map.fitBounds(bounds, { padding: [50, 50], maxZoom: 16 });
    }
  }, [plotted, selectedSessionId, onEditSession]);

  const handleFitAll = () => {
    if (!mapInstanceRef.current || plotted.length === 0) return;
    const bounds = L.latLngBounds([]);
    plotted.forEach((g) => bounds.extend([g.latitude, g.longitude]));
    if (bounds.isValid()) {
      mapInstanceRef.current.fitBounds(bounds, { padding: [50, 50], maxZoom: 16 });
    }
  };

  return (
    <div className="relative w-full h-[600px] rounded-2xl overflow-hidden border border-slate-200 dark:border-slate-800 shadow-sm bg-white dark:bg-slate-900">
      <div ref={mapContainerRef} className="w-full h-full z-0" />
      
      {/* Overlay control */}
      <div className="absolute top-3 right-3 z-10 flex gap-2">
        <button
          onClick={handleFitAll}
          className="px-3 py-2 bg-white/95 dark:bg-slate-900/95 hover:bg-white text-slate-800 dark:text-slate-100 rounded-xl shadow-md border border-slate-200 dark:border-slate-800 text-xs font-semibold flex items-center gap-1.5 backdrop-blur-xs transition"
          title="Fit all markers in view"
        >
          <Maximize2 className="w-3.5 h-3.5 text-amber-600" />
          Fit All Venues
        </button>
      </div>

      <div className="absolute bottom-3 left-3 z-10 bg-white/95 dark:bg-slate-900/95 px-3 py-1.5 rounded-xl shadow-md border border-slate-200 dark:border-slate-800 text-xs font-medium text-slate-700 dark:text-slate-300 backdrop-blur-xs flex items-center gap-2">
        <MapPin className="w-4 h-4 text-amber-600" />
        <span>
          {plottedSessionCount} sessions at {plotted.length} venue{plotted.length === 1 ? '' : 's'} plotted on map
        </span>
      </div>
    </div>
  );
};
