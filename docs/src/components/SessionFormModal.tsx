import React, { useState, useEffect } from 'react';
import { ConferenceSession, COLOR_PALETTE_PRESETS } from '../types';
import { MapLocationPicker } from './MapLocationPicker';
import { geocodeAddress } from '../utils/exportImport';
import { 
  X, 
  MapPin, 
  Clock, 
  Calendar, 
  User, 
  Search, 
  Crosshair, 
  Check, 
  AlertCircle,
  Palette,
  Eye
} from 'lucide-react';

interface SessionFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (session: ConferenceSession) => void;
  initialSession?: ConferenceSession | null;
}

export const SessionFormModal: React.FC<SessionFormModalProps> = ({
  isOpen,
  onClose,
  onSave,
  initialSession,
}) => {
  const [formData, setFormData] = useState<ConferenceSession>({
    id: '',
    title: '',
    speaker: '',
    date: new Date().toISOString().split('T')[0],
    startTime: '09:00',
    endTime: '10:00',
    locationName: '',
    address: '',
    latitude: 0,
    longitude: 0,
    colorHex: '#CA8A04',
  });

  const [showMapPicker, setShowMapPicker] = useState<boolean>(true);
  const [isGeocoding, setIsGeocoding] = useState<boolean>(false);
  const [geocodeMsg, setGeocodeMsg] = useState<{ text: string; isError: boolean } | null>(null);
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const [newExtraKey, setNewExtraKey] = useState<string>('');
  const [newExtraVal, setNewExtraVal] = useState<string>('');

  useEffect(() => {
    if (initialSession) {
      setFormData(initialSession);
    } else {
      setFormData({
        id: `session-${Date.now()}`,
        title: '',
        speaker: '',
        date: new Date().toISOString().split('T')[0],
        startTime: '09:00',
        endTime: '10:00',
        locationName: '',
        address: '',
        latitude: 0,
        longitude: 0,
        colorHex: '#CA8A04',
      });
    }
    setFormErrors({});
    setGeocodeMsg(null);
  }, [initialSession, isOpen]);

  if (!isOpen) return null;

  const handleLookupAddress = async () => {
    if (!formData.address.trim()) {
      setGeocodeMsg({ text: 'Please enter an address first', isError: true });
      return;
    }
    setIsGeocoding(true);
    setGeocodeMsg(null);
    try {
      const result = await geocodeAddress(formData.address);
      if (result) {
        setFormData((prev) => ({
          ...prev,
          latitude: Number(result.lat.toFixed(7)),
          longitude: Number(result.lng.toFixed(7)),
        }));
        setGeocodeMsg({ text: `Found location: ${result.displayName.slice(0, 45)}...`, isError: false });
      } else {
        setGeocodeMsg({ text: 'Address not found on OpenStreetMap. You can click on the map or type coordinates.', isError: true });
      }
    } catch {
      setGeocodeMsg({ text: 'Geocoding request failed. Please set coordinates manually.', isError: true });
    } finally {
      setIsGeocoding(false);
    }
  };

  const handleUseCurrentLocation = () => {
    if (!navigator.geolocation) {
      setGeocodeMsg({ text: 'Geolocation is not supported by your browser', isError: true });
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setFormData((prev) => ({
          ...prev,
          latitude: Number(pos.coords.latitude.toFixed(7)),
          longitude: Number(pos.coords.longitude.toFixed(7)),
        }));
        setGeocodeMsg({ text: 'Current GPS coordinates applied!', isError: false });
      },
      (err) => {
        setGeocodeMsg({ text: `GPS error: ${err.message}`, isError: true });
      }
    );
  };

  // Calculate duration
  const calculateDuration = () => {
    if (!formData.startTime || !formData.endTime) return '';
    const [startH, startM] = formData.startTime.split(':').map(Number);
    const [endH, endM] = formData.endTime.split(':').map(Number);
    if (isNaN(startH) || isNaN(startM) || isNaN(endH) || isNaN(endM)) return '';
    const diffMins = endH * 60 + endM - (startH * 60 + startM);
    if (diffMins <= 0) return 'Invalid duration';
    const hrs = Math.floor(diffMins / 60);
    const mins = diffMins % 60;
    if (hrs === 0) return `${mins} min`;
    if (mins === 0) return `${hrs} hr`;
    return `${hrs} hr ${mins} min`;
  };

  const validate = (): boolean => {
    const errors: Record<string, string> = {};
    if (!formData.title.trim()) errors.title = 'Title is required';
    if (!formData.speaker.trim()) errors.speaker = 'Speaker is required';
    if (!formData.date.trim()) errors.date = 'Date is required';
    if (!formData.startTime.trim()) errors.startTime = 'Start time is required';
    if (!formData.endTime.trim()) errors.endTime = 'End time is required';
    if (formData.startTime && formData.endTime && formData.startTime >= formData.endTime) {
      errors.endTime = 'End time must be after start time';
    }
    if (!formData.locationName.trim()) errors.locationName = 'Location or room name is required';
    if (isNaN(formData.latitude)) errors.latitude = 'Latitude must be a valid number';
    if (isNaN(formData.longitude)) errors.longitude = 'Longitude must be a valid number';
    if (!formData.colorHex.trim()) errors.colorHex = 'Color hex is required';

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;
    onSave({
      ...formData,
      id: formData.id || `session-${Date.now()}`,
    });
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-slate-900/60 backdrop-blur-xs overflow-y-auto">
      <div 
        className="relative w-full max-w-3xl max-h-[92vh] flex flex-col bg-white dark:bg-slate-900 rounded-2xl shadow-2xl border border-slate-200 dark:border-slate-800 overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/40">
          <div className="flex items-center gap-3">
            <div 
              className="w-4 h-8 rounded-full shadow-sm"
              style={{ backgroundColor: formData.colorHex }}
            />
            <div>
              <h2 className="text-xl font-bold text-slate-900 dark:text-white">
                {initialSession ? 'Edit Conference Session' : 'Add New Conference Session'}
              </h2>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Configure session schedule, speaker, venue location, and appearance
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6 space-y-6">
          {/* Title & Speaker */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-300 mb-1.5">
                Session Title <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                value={formData.title}
                onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                placeholder="e.g. Next-Gen Web Architectures"
                className={`w-full px-3.5 py-2.5 rounded-lg border text-sm font-medium transition bg-slate-50/50 dark:bg-slate-800/60 dark:text-white focus:outline-none focus:ring-2 focus:ring-amber-500 ${
                  formErrors.title ? 'border-rose-400 dark:border-rose-500' : 'border-slate-300 dark:border-slate-700'
                }`}
              />
              {formErrors.title && (
                <p className="mt-1 text-xs text-rose-500 flex items-center gap-1">
                  <AlertCircle className="w-3 h-3" /> {formErrors.title}
                </p>
              )}
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-300 mb-1.5 flex items-center gap-1.5">
                <User className="w-3.5 h-3.5 text-slate-400" />
                Speaker Name <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                value={formData.speaker}
                onChange={(e) => setFormData({ ...formData, speaker: e.target.value })}
                placeholder="e.g. Cristiano"
                className={`w-full px-3.5 py-2.5 rounded-lg border text-sm font-medium transition bg-slate-50/50 dark:bg-slate-800/60 dark:text-white focus:outline-none focus:ring-2 focus:ring-amber-500 ${
                  formErrors.speaker ? 'border-rose-400 dark:border-rose-500' : 'border-slate-300 dark:border-slate-700'
                }`}
              />
              {formErrors.speaker && (
                <p className="mt-1 text-xs text-rose-500 flex items-center gap-1">
                  <AlertCircle className="w-3 h-3" /> {formErrors.speaker}
                </p>
              )}
            </div>
          </div>

          {/* Date, Start Time, End Time */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-300 mb-1.5 flex items-center gap-1.5">
                <Calendar className="w-3.5 h-3.5 text-slate-400" />
                Date <span className="text-rose-500">*</span>
              </label>
              <input
                type="date"
                value={formData.date}
                onChange={(e) => setFormData({ ...formData, date: e.target.value })}
                className="w-full px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-700 text-sm font-medium bg-slate-50/50 dark:bg-slate-800/60 dark:text-white focus:outline-none focus:ring-2 focus:ring-amber-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-300 mb-1.5 flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5 text-slate-400" />
                Start Time <span className="text-rose-500">*</span>
              </label>
              <input
                type="time"
                value={formData.startTime}
                onChange={(e) => setFormData({ ...formData, startTime: e.target.value })}
                className="w-full px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-700 text-sm font-medium bg-slate-50/50 dark:bg-slate-800/60 dark:text-white focus:outline-none focus:ring-2 focus:ring-amber-500"
              />
            </div>

            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-300 flex items-center gap-1.5">
                  <Clock className="w-3.5 h-3.5 text-slate-400" />
                  End Time <span className="text-rose-500">*</span>
                </label>
                {calculateDuration() && (
                  <span className="text-xs px-2 py-0.5 rounded font-mono font-medium bg-amber-100 text-amber-800 dark:bg-amber-950/60 dark:text-amber-300">
                    {calculateDuration()}
                  </span>
                )}
              </div>
              <input
                type="time"
                value={formData.endTime}
                onChange={(e) => setFormData({ ...formData, endTime: e.target.value })}
                className={`w-full px-3 py-2 rounded-lg border text-sm font-medium bg-slate-50/50 dark:bg-slate-800/60 dark:text-white focus:outline-none focus:ring-2 focus:ring-amber-500 ${
                  formErrors.endTime ? 'border-rose-400' : 'border-slate-300 dark:border-slate-700'
                }`}
              />
              {formErrors.endTime && (
                <p className="mt-1 text-xs text-rose-500">{formErrors.endTime}</p>
              )}
            </div>
          </div>

          {/* Venue & Location Details */}
          <div className="border-t border-slate-200 dark:border-slate-800 pt-5">
            <h3 className="text-sm font-bold text-slate-900 dark:text-white mb-3 flex items-center gap-2">
              <MapPin className="w-4 h-4 text-amber-600" />
              Venue & Location Coordinates
            </h3>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-300 mb-1.5">
                  Room / Location Name <span className="text-rose-500">*</span>
                </label>
                <input
                  type="text"
                  value={formData.locationName}
                  onChange={(e) => setFormData({ ...formData, locationName: e.target.value })}
                  placeholder="e.g. 2 or Room 101 or Main Auditorium"
                  className="w-full px-3.5 py-2.5 rounded-lg border border-slate-300 dark:border-slate-700 text-sm font-medium bg-slate-50/50 dark:bg-slate-800/60 dark:text-white focus:outline-none focus:ring-2 focus:ring-amber-500"
                />
              </div>

              <div className="md:col-span-2">
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-300 mb-1.5">
                  Full Street Address
                </label>
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={formData.address}
                    onChange={(e) => setFormData({ ...formData, address: e.target.value })}
                    placeholder="e.g. Via VIII Febbraio, 2, 35122 Padova PD, Italy"
                    className="flex-1 px-3.5 py-2.5 rounded-lg border border-slate-300 dark:border-slate-700 text-sm font-medium bg-slate-50/50 dark:bg-slate-800/60 dark:text-white focus:outline-none focus:ring-2 focus:ring-amber-500"
                  />
                  <button
                    type="button"
                    onClick={handleLookupAddress}
                    disabled={isGeocoding}
                    className="px-3.5 py-2.5 rounded-lg bg-amber-500 hover:bg-amber-600 text-white text-xs font-semibold flex items-center gap-1.5 shrink-0 transition disabled:opacity-50 shadow-xs"
                    title="Geocode address coordinates via OpenStreetMap"
                  >
                    <Search className={`w-3.5 h-3.5 ${isGeocoding ? 'animate-spin' : ''}`} />
                    {isGeocoding ? 'Locating...' : 'Find Lat/Lng'}
                  </button>
                </div>
              </div>
            </div>

            {geocodeMsg && (
              <div
                className={`mb-4 px-3 py-2 rounded-lg text-xs font-medium flex items-center gap-2 ${
                  geocodeMsg.isError
                    ? 'bg-rose-50 text-rose-700 dark:bg-rose-950/50 dark:text-rose-300'
                    : 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/50 dark:text-emerald-300'
                }`}
              >
                {geocodeMsg.isError ? <AlertCircle className="w-4 h-4 shrink-0" /> : <Check className="w-4 h-4 shrink-0" />}
                <span>{geocodeMsg.text}</span>
              </div>
            )}

            {/* Coordinates Lat / Lng */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-3">
              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-300 mb-1.5">
                  Latitude <span className="text-rose-500">*</span>
                </label>
                <input
                  type="number"
                  step="any"
                  value={formData.latitude}
                  onChange={(e) => setFormData({ ...formData, latitude: parseFloat(e.target.value) || 0 })}
                  className="w-full px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-700 text-sm font-mono bg-slate-50/50 dark:bg-slate-800/60 dark:text-white focus:outline-none focus:ring-2 focus:ring-amber-500"
                />
              </div>

              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label className="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-300">
                    Longitude <span className="text-rose-500">*</span>
                  </label>
                  <button
                    type="button"
                    onClick={handleUseCurrentLocation}
                    className="text-xs text-amber-600 dark:text-amber-400 hover:underline flex items-center gap-1 font-medium"
                  >
                    <Crosshair className="w-3 h-3" /> Use GPS
                  </button>
                </div>
                <input
                  type="number"
                  step="any"
                  value={formData.longitude}
                  onChange={(e) => setFormData({ ...formData, longitude: parseFloat(e.target.value) || 0 })}
                  className="w-full px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-700 text-sm font-mono bg-slate-50/50 dark:bg-slate-800/60 dark:text-white focus:outline-none focus:ring-2 focus:ring-amber-500"
                />
              </div>
            </div>

            {/* Interactive Map Picker */}
            <div className="mt-3">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-semibold text-slate-500 dark:text-slate-400">
                  Interactive Coordinate Pin Picker
                </span>
                <button
                  type="button"
                  onClick={() => setShowMapPicker(!showMapPicker)}
                  className="text-xs font-medium text-amber-600 dark:text-amber-400 hover:underline"
                >
                  {showMapPicker ? 'Hide Map' : 'Show Map'}
                </button>
              </div>

              {showMapPicker && (
                <MapLocationPicker
                  latitude={formData.latitude}
                  longitude={formData.longitude}
                  colorHex={formData.colorHex}
                  locationName={formData.locationName}
                  onChange={(lat, lng) => setFormData((prev) => ({ ...prev, latitude: lat, longitude: lng }))}
                />
              )}
            </div>
          </div>

          {/* Color Selection & Preset Swatches */}
          <div className="border-t border-slate-200 dark:border-slate-800 pt-5">
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-300 mb-2 flex items-center gap-1.5">
              <Palette className="w-4 h-4 text-slate-400" />
              Session Badge Color (Hex Code) <span className="text-rose-500">*</span>
            </label>
            
            <div className="flex flex-wrap items-center gap-2 mb-3">
              {COLOR_PALETTE_PRESETS.map((preset) => (
                <button
                  key={preset.hex}
                  type="button"
                  onClick={() => setFormData({ ...formData, colorHex: preset.hex })}
                  className={`w-7 h-7 rounded-full flex items-center justify-center transition transform hover:scale-110 shadow-xs border-2 ${
                    formData.colorHex.toLowerCase() === preset.hex.toLowerCase()
                      ? 'border-slate-900 dark:border-white scale-110 ring-2 ring-amber-400/50'
                      : 'border-white dark:border-slate-800'
                  }`}
                  style={{ backgroundColor: preset.hex }}
                  title={`${preset.name} (${preset.hex})`}
                >
                  {formData.colorHex.toLowerCase() === preset.hex.toLowerCase() && (
                    <Check className="w-3.5 h-3.5 text-white drop-shadow-sm" />
                  )}
                </button>
              ))}
            </div>

            <div className="flex items-center gap-3">
              <div className="relative flex items-center">
                <input
                  type="color"
                  value={formData.colorHex}
                  onChange={(e) => setFormData({ ...formData, colorHex: e.target.value.toUpperCase() })}
                  className="w-10 h-10 p-1 rounded-lg border border-slate-300 dark:border-slate-700 cursor-pointer bg-white dark:bg-slate-800"
                />
              </div>
              <input
                type="text"
                value={formData.colorHex}
                onChange={(e) => setFormData({ ...formData, colorHex: e.target.value })}
                placeholder="#CA8A04"
                className="w-36 px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-700 text-sm font-mono uppercase bg-slate-50/50 dark:bg-slate-800/60 dark:text-white focus:outline-none focus:ring-2 focus:ring-amber-500"
              />
              <span className="text-xs text-slate-400">
                Custom HEX format (e.g. #CA8A04)
              </span>
            </div>
          </div>

          {/* Optional Extra Fields */}
          <div className="border-t border-slate-200 dark:border-slate-800 pt-5">
            <div className="flex items-center justify-between mb-2">
              <label className="text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-300">
                Additional / Custom Fields
              </label>
              <span className="text-xs text-slate-400">
                Preserved automatically in exported JSON
              </span>
            </div>

            {formData.extraFields && Object.keys(formData.extraFields).length > 0 && (
              <div className="space-y-2 mb-3">
                {Object.entries(formData.extraFields).map(([key, val]) => (
                  <div key={key} className="flex items-center gap-2 p-2 rounded-lg bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700 text-xs">
                    <span className="font-mono font-bold text-amber-700 dark:text-amber-400 min-w-28">{key}:</span>
                    <input
                      type="text"
                      value={typeof val === 'string' ? val : JSON.stringify(val)}
                      onChange={(e) => {
                        const updated = { ...(formData.extraFields || {}) };
                        updated[key] = e.target.value;
                        setFormData({ ...formData, extraFields: updated });
                      }}
                      className="flex-1 px-2 py-1 rounded border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 font-mono text-xs"
                    />
                    <button
                      type="button"
                      onClick={() => {
                        const updated = { ...(formData.extraFields || {}) };
                        delete updated[key];
                        setFormData({ ...formData, extraFields: updated });
                      }}
                      className="text-rose-500 hover:text-rose-700 p-1"
                      title="Remove field"
                    >
                      <X className="w-3.5 h-3.5" />
                    </button>
                  </div>
                ))}
              </div>
            )}

            {/* Add new extra field */}
            <div className="flex items-center gap-2">
              <input
                type="text"
                placeholder="Field name (e.g. track, bio)"
                value={newExtraKey}
                onChange={(e) => setNewExtraKey(e.target.value)}
                className="w-1/3 px-3 py-1.5 rounded-lg border border-slate-300 dark:border-slate-700 text-xs font-mono bg-slate-50/50 dark:bg-slate-800/60"
              />
              <input
                type="text"
                placeholder="Value"
                value={newExtraVal}
                onChange={(e) => setNewExtraVal(e.target.value)}
                className="flex-1 px-3 py-1.5 rounded-lg border border-slate-300 dark:border-slate-700 text-xs bg-slate-50/50 dark:bg-slate-800/60"
              />
              <button
                type="button"
                onClick={() => {
                  if (!newExtraKey.trim()) return;
                  const current = formData.extraFields || {};
                  setFormData({
                    ...formData,
                    extraFields: { ...current, [newExtraKey.trim()]: newExtraVal },
                  });
                  setNewExtraKey('');
                  setNewExtraVal('');
                }}
                disabled={!newExtraKey.trim()}
                className="px-3 py-1.5 rounded-lg bg-slate-200 hover:bg-slate-300 dark:bg-slate-700 dark:hover:bg-slate-600 text-slate-800 dark:text-slate-100 text-xs font-semibold disabled:opacity-40"
              >
                + Add Field
              </button>
            </div>
          </div>

          {/* Quick Preview Card */}
          <div className="border-t border-slate-200 dark:border-slate-800 pt-4">
            <div className="text-xs font-semibold text-slate-500 dark:text-slate-400 mb-2 flex items-center gap-1.5">
              <Eye className="w-3.5 h-3.5" />
              Session Preview
            </div>
            <div className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-800/50 flex items-start gap-3.5">
              <div
                className="w-3 self-stretch rounded-full shrink-0"
                style={{ backgroundColor: formData.colorHex }}
              />
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2 text-xs font-medium text-slate-500 dark:text-slate-400 mb-1">
                  <span>{formData.date || 'YYYY-MM-DD'}</span>
                  <span>•</span>
                  <span>{formData.startTime || '--:--'} - {formData.endTime || '--:--'}</span>
                  <span>•</span>
                  <span className="px-2 py-0.5 rounded font-semibold text-white text-xs" style={{ backgroundColor: formData.colorHex }}>
                    Room: {formData.locationName || 'N/A'}
                  </span>
                </div>
                <h4 className="text-base font-bold text-slate-900 dark:text-white truncate">
                  {formData.title || 'Untitled Session'}
                </h4>
                <p className="text-xs text-slate-600 dark:text-slate-300 font-medium mt-0.5">
                  Speaker: {formData.speaker || 'No speaker designated'}
                </p>
                {formData.address && (
                  <p className="text-xs text-slate-400 mt-1 truncate">
                    {formData.address} ({formData.latitude}, {formData.longitude})
                  </p>
                )}
              </div>
            </div>
          </div>
        </form>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/40 flex items-center justify-end gap-3">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 text-sm font-semibold text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={handleSubmit}
            className="px-5 py-2.5 rounded-xl bg-amber-600 hover:bg-amber-700 text-white text-sm font-semibold shadow-md transition hover:shadow-lg flex items-center gap-1.5"
          >
            <Check className="w-4 h-4" />
            {initialSession ? 'Save Changes' : 'Create Session'}
          </button>
        </div>
      </div>
    </div>
  );
};
