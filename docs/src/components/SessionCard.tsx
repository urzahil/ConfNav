import React from 'react';
import { ConferenceSession } from '../types';
import { 
  Calendar, 
  Clock, 
  MapPin, 
  User, 
  Edit3, 
  Copy, 
  Trash2, 
  ExternalLink 
} from 'lucide-react';

interface SessionCardProps {
  session: ConferenceSession;
  onEdit: (session: ConferenceSession) => void;
  onDuplicate: (session: ConferenceSession) => void;
  onDelete: (id: string) => void;
  onViewOnMap?: (session: ConferenceSession) => void;
}

export const SessionCard: React.FC<SessionCardProps> = ({
  session,
  onEdit,
  onDuplicate,
  onDelete,
  onViewOnMap,
}) => {
  const formatDuration = (startTime: string, endTime: string) => {
    if (!startTime || !endTime) return '';
    const [sH, sM] = startTime.split(':').map(Number);
    const [eH, eM] = endTime.split(':').map(Number);
    if (isNaN(sH) || isNaN(sM) || isNaN(eH) || isNaN(eM)) return '';
    const diff = eH * 60 + eM - (sH * 60 + sM);
    if (diff <= 0) return '';
    const hours = Math.floor(diff / 60);
    const mins = diff % 60;
    if (hours === 0) return `${mins}m`;
    if (mins === 0) return `${hours}h`;
    return `${hours}h ${mins}m`;
  };

  const duration = formatDuration(session.startTime, session.endTime);
  const mapSearchUrl = `https://www.google.com/maps/search/?api=1&query=${session.latitude},${session.longitude}`;

  return (
    <div className="group relative bg-white dark:bg-slate-900 rounded-2xl border border-slate-200/80 dark:border-slate-800 shadow-sm hover:shadow-md transition-all duration-200 overflow-hidden flex flex-col justify-between">
      {/* Color Top Border Accent */}
      <div 
        className="h-2 w-full transition-all group-hover:h-2.5" 
        style={{ backgroundColor: session.colorHex || '#D97706' }} 
      />

      <div className="p-5 flex-1 flex flex-col justify-between">
        <div>
          {/* Header Metadata */}
          <div className="flex flex-wrap items-center justify-between gap-2 mb-3">
            <div className="flex items-center gap-1.5 text-xs font-semibold text-slate-500 dark:text-slate-400">
              <Calendar className="w-3.5 h-3.5 text-slate-400" />
              <span>{session.date}</span>
              <span className="text-slate-300 dark:text-slate-700">•</span>
              <Clock className="w-3.5 h-3.5 text-slate-400" />
              <span>{session.startTime} - {session.endTime}</span>
              {duration && (
                <span className="ml-1 px-1.5 py-0.2 rounded text-[11px] font-mono bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300">
                  {duration}
                </span>
              )}
            </div>

            {/* Room / Location badge */}
            <div 
              className="px-2.5 py-0.5 rounded-full text-xs font-bold text-white shadow-2xs flex items-center gap-1"
              style={{ backgroundColor: session.colorHex || '#CA8A04' }}
            >
              <MapPin className="w-3 h-3" />
              <span>{session.locationName}</span>
            </div>
          </div>

          {/* Session Title */}
          <h3 className="text-lg font-bold text-slate-900 dark:text-white leading-snug mb-1 group-hover:text-amber-600 dark:group-hover:text-amber-400 transition-colors">
            {session.title}
          </h3>

          {/* Speaker */}
          <div className="flex items-center gap-2 text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">
            <div className="w-6 h-6 rounded-full bg-slate-100 dark:bg-slate-800 flex items-center justify-center text-slate-500 dark:text-slate-400">
              <User className="w-3.5 h-3.5" />
            </div>
            <span>{session.speaker}</span>
          </div>

          {/* Extra Custom Fields Tags */}
          {session.extraFields && Object.keys(session.extraFields).length > 0 && (
            <div className="flex flex-wrap gap-1 mb-3">
              {Object.entries(session.extraFields).map(([key, val]) => (
                <span
                  key={key}
                  className="px-2 py-0.5 rounded-md text-[10px] font-mono bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-slate-700"
                >
                  <strong className="text-slate-700 dark:text-slate-200">{key}:</strong> {String(val)}
                </span>
              ))}
            </div>
          )}

          {/* Venue & Coordinates */}
          <div className="bg-slate-50 dark:bg-slate-800/60 rounded-xl p-3 text-xs text-slate-600 dark:text-slate-300 space-y-1 mb-4 border border-slate-100 dark:border-slate-800">
            {session.address && (
              <p className="line-clamp-2 text-slate-600 dark:text-slate-300 font-medium">
                {session.address}
              </p>
            )}
            <div className="flex items-center justify-between text-[11px] text-slate-400 font-mono pt-1 border-t border-slate-200/50 dark:border-slate-700/50">
              <span>
                {session.latitude.toFixed(6)}, {session.longitude.toFixed(6)}
              </span>
              <a
                href={mapSearchUrl}
                target="_blank"
                rel="noreferrer"
                className="text-amber-600 dark:text-amber-400 hover:underline flex items-center gap-0.5 font-sans font-medium"
                title="View coordinates on Google Maps"
              >
                Maps <ExternalLink className="w-3 h-3" />
              </a>
            </div>
          </div>
        </div>

        {/* Card Footer Actions */}
        <div className="flex items-center justify-between pt-3 border-t border-slate-100 dark:border-slate-800">
          {onViewOnMap ? (
            <button
              onClick={() => onViewOnMap(session)}
              className="text-xs font-semibold text-slate-600 dark:text-slate-400 hover:text-amber-600 dark:hover:text-amber-400 flex items-center gap-1.5 transition"
            >
              <MapPin className="w-3.5 h-3.5" />
              Locate on Map
            </button>
          ) : <div />}

          <div className="flex items-center gap-1">
            <button
              onClick={() => onDuplicate(session)}
              className="p-1.5 rounded-lg text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition"
              title="Duplicate session"
            >
              <Copy className="w-4 h-4" />
            </button>
            <button
              onClick={() => onEdit(session)}
              className="p-1.5 rounded-lg text-slate-400 hover:text-amber-600 dark:hover:text-amber-400 hover:bg-amber-50 dark:hover:bg-amber-950/40 transition"
              title="Edit session"
            >
              <Edit3 className="w-4 h-4" />
            </button>
            <button
              onClick={() => onDelete(session.id)}
              className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 dark:hover:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/40 transition"
              title="Delete session"
            >
              <Trash2 className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
