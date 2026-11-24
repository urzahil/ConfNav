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

  // The map is created once, so its handlers read the latest props through refs.
  const onChangeRef = useRef(onChange);
  const colorHexRef = useRef(colorHex);
  const locationNameRef = useRef(locationName);
  onChangeRef.current = onChange;
  colorHexRef.current = colorHex;
  locationNameRef.current = locationName;

  // (0, 0) means "not set": the map starts on a default area and no pin is placed until the user picks a spot.
  const hasLocation = !(latitude === 0 && longitude === 0);
  const defaultLat = hasLocation ? latitude : 45.4067393;
  const defaultLng = hasLocation ? longitude : 11.8771509;

  useEffect(() => {
    if (!mapContainerRef.current) return;

    if (!mapInstanceRef.current) {
      const map = L.map(mapContainerRef.current, {
        center: [defaultLat, defaultLng],
        zoom: 14,
        attributionControl: true,
      });

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; OpenStreetMap contributors',
      }).addTo(map);

      const placeMarker = (lat: number, lng: number): L.Marker => {
        const marker = L.marker([lat, lng], {
          icon: createCustomMarkerIcon(colorHexRef.current, locationNameRef.current),
          draggable: true,
        }).addTo(map);
        marker.on('dragend', () => {
          const pos = marker.getLatLng();
          onChangeRef.current(Number(pos.lat.toFixed(7)), Number(pos.lng.toFixed(7)));
        });
        markerRef.current = marker;
        return marker;
      };

      if (hasLocation) placeMarker(defaultLat, defaultLng);

      map.on('click', (e: L.LeafletMouseEvent) => {
        const { lat, lng } = e.latlng;
        const roundLat = Number(lat.toFixed(7));
        const roundLng = Number(lng.toFixed(7));
        if (markerRef.current) {
          markerRef.current.setLatLng([roundLat, roundLng]);
        } else {
          placeMarker(roundLat, roundLng);
        }
        onChangeRef.current(roundLat, roundLng);
      });

      mapInstanceRef.current = map;

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
    const map = mapInstanceRef.current;
    if (!map) return;
    const isSet = !(latitude === 0 && longitude === 0);
    if (!isSet) {
      // Coordinates were cleared: take the pin off the map rather than show a fake location.
      if (markerRef.current) {
        markerRef.current.remove();
        markerRef.current = null;
      }
      return;
    }
    if (!markerRef.current) {
      markerRef.current = L.marker([latitude, longitude], {
        icon: createCustomMarkerIcon(colorHex, locationName),
        draggable: true,
      }).addTo(map);
      markerRef.current.on('dragend', () => {
        const pos = markerRef.current!.getLatLng();
        onChangeRef.current(Number(pos.lat.toFixed(7)), Number(pos.lng.toFixed(7)));
      });
      map.panTo([latitude, longitude]);
      return;
    }
    const currentPos = markerRef.current.getLatLng();
    if (Math.abs(currentPos.lat - latitude) > 0.00001 || Math.abs(currentPos.lng - longitude) > 0.00001) {
      markerRef.current.setLatLng([latitude, longitude]);
      map.panTo([latitude, longitude]);
    }
    markerRef.current.setIcon(createCustomMarkerIcon(colorHex, locationName));
  }, [latitude, longitude, colorHex, locationName]);

  return (
    <div className="relative rounded-lg overflow-hidden border border-slate-300 dark:border-slate-700">
      <div ref={mapContainerRef} className="h-56 w-full z-0" />
      <div className="absolute top-2 left-2 z-10 bg-white/95 dark:bg-slate-900/95 px-2.5 py-1 rounded shadow text-xs font-medium text-slate-700 dark:text-slate-300 flex items-center gap-1.5 backdrop-blur-sm pointer-events-none">
        <MapPin className="w-3.5 h-3.5 text-amber-600" />
        {hasLocation ? 'Click or drag pin to position coordinates' : 'Click the map to set the location'}
      </div>
    </div>
  );
};
