import React, { useEffect, useRef } from 'react';
import L from 'leaflet';
import { ConferenceSession } from '../types';
import { createCustomMarkerIcon } from './LeafletIconHelper';
import { Maximize2, MapPin } from 'lucide-react';

interface MapViewProps {
  sessions: ConferenceSession[];
  onEditSession: (session: ConferenceSession) => void;
  selectedSessionId?: string | null;
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

  // Update markers when sessions change
  useEffect(() => {
    const map = mapInstanceRef.current;
    const layer = markersLayerRef.current;
    if (!map || !layer) return;

    layer.clearLayers();

    if (sessions.length === 0) return;

    const bounds = L.latLngBounds([]);

    sessions.forEach((session) => {
      if (typeof session.latitude !== 'number' || typeof session.longitude !== 'number') return;
      if (session.latitude === 0 && session.longitude === 0) return;

      const marker = L.marker([session.latitude, session.longitude], {
        icon: createCustomMarkerIcon(session.colorHex, session.locationName),
      });

      const popupContent = `
        <div style="font-family: inherit; min-width: 180px; padding: 4px;">
          <div style="display: flex; align-items: center; gap: 6px; margin-bottom: 4px;">
            <span style="display: inline-block; width: 10px; height: 10px; border-radius: 50%; background-color: ${session.colorHex};"></span>
            <span style="font-size: 11px; font-weight: 700; color: ${session.colorHex}; text-transform: uppercase;">${session.locationName}</span>
          </div>
          <div style="font-weight: 700; font-size: 14px; color: #0f172a; margin-bottom: 2px;">${session.title}</div>
          <div style="font-size: 12px; color: #475569; margin-bottom: 6px;">Speaker: <b>${session.speaker}</b></div>
          <div style="font-size: 11px; color: #64748b; margin-bottom: 8px;">
            📅 ${session.date} | ⏰ ${session.startTime} - ${session.endTime}
          </div>
          <div style="font-size: 11px; color: #64748b; margin-bottom: 10px; font-style: italic;">
            📍 ${session.address || 'No address specified'}
          </div>
          <button id="edit-session-btn-${session.id}" style="width: 100%; padding: 6px 10px; font-size: 12px; font-weight: 600; background: #d97706; color: white; border: none; border-radius: 6px; cursor: pointer;">
            Edit Session
          </button>
        </div>
      `;

      marker.bindPopup(popupContent);

      marker.on('popupopen', () => {
        const btn = document.getElementById(`edit-session-btn-${session.id}`);
        if (btn) {
          btn.onclick = () => onEditSession(session);
        }
      });

      marker.addTo(layer);
      bounds.extend([session.latitude, session.longitude]);

      if (selectedSessionId === session.id) {
        map.setView([session.latitude, session.longitude], 16);
        marker.openPopup();
      }
    });

    if (bounds.isValid() && !selectedSessionId) {
      map.fitBounds(bounds, { padding: [50, 50], maxZoom: 16 });
    }
  }, [sessions, selectedSessionId, onEditSession]);

  const handleFitAll = () => {
    if (!mapInstanceRef.current || sessions.length === 0) return;
    const bounds = L.latLngBounds([]);
    sessions.forEach((s) => {
      if (s.latitude && s.longitude) {
        bounds.extend([s.latitude, s.longitude]);
      }
    });
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
        <span>{sessions.length} sessions plotted on map</span>
      </div>
    </div>
  );
};
