import React, { useEffect, useRef } from 'react';
import L from 'leaflet';
import { createCustomMarkerIcon } from './LeafletIconHelper';
import { MapPin } from 'lucide-react';

interface MapLocationPickerProps {
  latitude: number;
  longitude: number;
  colorHex: string;
  onChange: (lat: number, lng: number) => void;
  locationName?: string;
}

export const MapLocationPicker: React.FC<MapLocationPickerProps> = ({
  latitude,
  longitude,
  colorHex,
  onChange,
  locationName,
}) => {
  const mapContainerRef = useRef<HTMLDivElement | null>(null);
  const mapInstanceRef = useRef<L.Map | null>(null);
  const markerRef = useRef<L.Marker | null>(null);

  // Default fallback if (0, 0)
  const defaultLat = latitude && latitude !== 0 ? latitude : 45.4067393;
  const defaultLng = longitude && longitude !== 0 ? longitude : 11.8771509;

  useEffect(() => {
    if (!mapContainerRef.current) return;

    if (!mapInstanceRef.current) {
      const map = L.map(mapContainerRef.current, {
        center: [defaultLat, defaultLng],
        zoom: 14,
        attributionControl: false,
      });

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
      }).addTo(map);

      const marker = L.marker([defaultLat, defaultLng], {
        icon: createCustomMarkerIcon(colorHex, locationName),
        draggable: true,
      }).addTo(map);

      marker.on('dragend', () => {
        const pos = marker.getLatLng();
        onChange(Number(pos.lat.toFixed(7)), Number(pos.lng.toFixed(7)));
      });

      map.on('click', (e: L.LeafletMouseEvent) => {
        const { lat, lng } = e.latlng;
        const roundLat = Number(lat.toFixed(7));
        const roundLng = Number(lng.toFixed(7));
        marker.setLatLng([roundLat, roundLng]);
        onChange(roundLat, roundLng);
      });

      mapInstanceRef.current = map;
      markerRef.current = marker;

      // Invalidate size after render to prevent leaflet grey tiles issue
      setTimeout(() => {
        map.invalidateSize();
      }, 200);
    }

    return () => {
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove();
        mapInstanceRef.current = null;
        markerRef.current = null;
      }
    };
  }, []);

  // Update marker position and icon when props change
  useEffect(() => {
    if (mapInstanceRef.current && markerRef.current) {
      const currentPos = markerRef.current.getLatLng();
      if (
        Math.abs(currentPos.lat - latitude) > 0.00001 ||
        Math.abs(currentPos.lng - longitude) > 0.00001
      ) {
        if (latitude !== 0 || longitude !== 0) {
          markerRef.current.setLatLng([latitude, longitude]);
          mapInstanceRef.current.panTo([latitude, longitude]);
        }
      }
      markerRef.current.setIcon(createCustomMarkerIcon(colorHex, locationName));
    }
  }, [latitude, longitude, colorHex, locationName]);

  return (
    <div className="relative rounded-lg overflow-hidden border border-slate-300 dark:border-slate-700">
      <div ref={mapContainerRef} className="h-56 w-full z-0" />
      <div className="absolute top-2 left-2 z-10 bg-white/95 dark:bg-slate-900/95 px-2.5 py-1 rounded shadow text-xs font-medium text-slate-700 dark:text-slate-300 flex items-center gap-1.5 backdrop-blur-sm pointer-events-none">
        <MapPin className="w-3.5 h-3.5 text-amber-600" />
        Click or drag pin to position coordinates
      </div>
    </div>
  );
};
