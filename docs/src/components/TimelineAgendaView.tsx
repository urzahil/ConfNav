import React from 'react';
import { ConferenceSession } from '../types';
import { Clock, MapPin, User, Edit3, Trash2 } from 'lucide-react';

interface TimelineAgendaViewProps {
  sessions: ConferenceSession[];
  onEdit: (session: ConferenceSession) => void;
  onDelete: (id: string) => void;
}

export const TimelineAgendaView: React.FC<TimelineAgendaViewProps> = ({
  sessions,
  onEdit,
  onDelete,
}) => {
  // Group sessions by date
  const groupedByDate: Record<string, ConferenceSession[]> = {};
  
  // Sort all sessions by date then by startTime
  const sortedSessions = [...sessions].sort((a, b) => {
    if (a.date !== b.date) return a.date.localeCompare(b.date);
    return a.startTime.localeCompare(b.startTime);
  });

  sortedSessions.forEach((s) => {
    if (!groupedByDate[s.date]) {
      groupedByDate[s.date] = [];
    }
    groupedByDate[s.date].push(s);
  });

  const dates = Object.keys(groupedByDate).sort();

  if (dates.length === 0) {
    return (
      <div className="text-center py-12 text-slate-400">
        No sessions scheduled yet.
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {dates.map((date) => {
        const daySessions = groupedByDate[date];
        const formattedDate = new Date(date + 'T00:00:00').toLocaleDateString(undefined, {
          weekday: 'long',
          year: 'numeric',
          month: 'long',
          day: 'numeric',
        });

        return (
          <div key={date} className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200/80 dark:border-slate-800 p-6 shadow-xs">
            {/* Day Header */}
            <div className="flex items-center justify-between pb-4 mb-6 border-b border-slate-100 dark:border-slate-800">
              <div>
                <h3 className="text-lg font-bold text-slate-900 dark:text-white">
                  {formattedDate}
                </h3>
                <span className="text-xs font-medium text-slate-400">
                  {daySessions.length} session{daySessions.length !== 1 ? 's' : ''} scheduled
                </span>
              </div>
              <span className="text-xs font-mono font-semibold px-2.5 py-1 rounded-lg bg-amber-50 dark:bg-amber-950/40 text-amber-700 dark:text-amber-300 border border-amber-200 dark:border-amber-900/40">
                {date}
              </span>
            </div>

            {/* Timeline Stream */}
            <div className="relative pl-6 sm:pl-8 space-y-6 before:absolute before:left-3 before:top-2 before:bottom-2 before:w-0.5 before:bg-slate-200 dark:before:bg-slate-800">
              {daySessions.map((session) => (
                <div key={session.id} className="relative group">
                  {/* Timeline bullet dot */}
                  <div
                    className="absolute -left-6 sm:-left-8 top-1.5 w-4 h-4 rounded-full border-2 border-white dark:border-slate-900 shadow-xs transition-transform group-hover:scale-125"
                    style={{ backgroundColor: session.colorHex }}
                  />

                  {/* Session Row */}
                  <div className="bg-slate-50 dark:bg-slate-800/50 hover:bg-slate-100/80 dark:hover:bg-slate-800 rounded-xl p-4 transition border border-slate-100 dark:border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                    <div className="space-y-1.5 flex-1">
                      {/* Time & Room badges */}
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="text-xs font-bold font-mono text-slate-700 dark:text-slate-300 flex items-center gap-1">
                          <Clock className="w-3.5 h-3.5 text-amber-600" />
                          {session.startTime} - {session.endTime}
                        </span>
                        <span
                          className="px-2 py-0.5 rounded text-[11px] font-bold text-white flex items-center gap-1"
                          style={{ backgroundColor: session.colorHex }}
                        >
                          <MapPin className="w-3 h-3" /> {session.locationName}
                        </span>
                      </div>

                      {/* Title */}
                      <h4 className="text-base font-bold text-slate-900 dark:text-white">
                        {session.title}
                      </h4>

                      {/* Speaker & Address */}
                      <div className="flex flex-wrap items-center gap-3 text-xs text-slate-500 dark:text-slate-400">
                        <span className="flex items-center gap-1 font-medium text-slate-700 dark:text-slate-300">
                          <User className="w-3 h-3 text-slate-400" />
                          {session.speaker}
                        </span>
                        {session.address && (
                          <span className="truncate max-w-xs text-slate-400">
                            • {session.address}
                          </span>
                        )}
                      </div>
                    </div>

                    {/* Quick Actions */}
                    <div className="flex items-center gap-1 self-end sm:self-center shrink-0">
                      <button
                        onClick={() => onEdit(session)}
                        className="p-2 rounded-lg text-slate-400 hover:text-amber-600 hover:bg-white dark:hover:bg-slate-700 transition"
                        title="Edit session"
                      >
                        <Edit3 className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => onDelete(session.id)}
                        className="p-2 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-white dark:hover:bg-slate-700 transition"
                        title="Delete session"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        );
      })}
    </div>
  );
};
