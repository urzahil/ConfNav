import React, { useEffect, useMemo, useState } from 'react';
import { ConferenceSession } from '../types';
import { ChevronLeft, ChevronRight } from 'lucide-react';

interface CalendarViewProps {
  sessions: ConferenceSession[];
  onEdit: (session: ConferenceSession) => void;
}

const HOUR_HEIGHT = 64; // px per hour
const MIN_EVENT_MINUTES = 30; // keeps very short sessions readable
const INDENT_PERCENT = 12; // horizontal cascade step for overlapping sessions

const toMinutes = (time: string): number => {
  const [h, m] = time.split(':').map(Number);
  return (h || 0) * 60 + (m || 0);
};

interface PositionedSession {
  session: ConferenceSession;
  start: number;
  end: number;
  column: number;
}

/**
 * Lays out one day's sessions Google-Calendar style: overlapping sessions are
 * assigned to columns, and each column is indented so earlier sessions stay visible
 * underneath later ones.
 */
function layoutDay(daySessions: ConferenceSession[]): PositionedSession[] {
  const items = daySessions
    .map((session) => {
      const start = toMinutes(session.startTime);
      const end = Math.max(toMinutes(session.endTime), start + MIN_EVENT_MINUTES);
      return { session, start, end, column: 0 };
    })
    .sort((a, b) => a.start - b.start || b.end - a.end);

  const columnEnds: number[] = [];
  let clusterEnd = -1;
  items.forEach((item) => {
    if (item.start >= clusterEnd) {
      columnEnds.length = 0; // previous overlap cluster is over, restart columns
      clusterEnd = item.end;
    }
    let col = columnEnds.findIndex((end) => end <= item.start);
    if (col === -1) col = columnEnds.length;
    columnEnds[col] = item.end;
    item.column = col;
    clusterEnd = Math.max(clusterEnd, item.end);
  });
  return items;
}

export const CalendarView: React.FC<CalendarViewProps> = ({ sessions, onEdit }) => {
  const dates = useMemo(() => Array.from(new Set(sessions.map((s) => s.date))).sort(), [sessions]);
  const [activeDate, setActiveDate] = useState<string>(dates[0] ?? '');

  useEffect(() => {
    if (dates.length > 0 && !dates.includes(activeDate)) setActiveDate(dates[0]);
  }, [dates, activeDate]);

  const daySessions = useMemo(() => sessions.filter((s) => s.date === activeDate), [sessions, activeDate]);
  const positioned = useMemo(() => layoutDay(daySessions), [daySessions]);

  if (dates.length === 0) {
    return <div className="text-center py-12 text-slate-400">No sessions scheduled yet.</div>;
  }

  const firstHour = Math.max(0, Math.floor(Math.min(...positioned.map((p) => p.start)) / 60) - 1);
  const lastHour = Math.min(24, Math.ceil(Math.max(...positioned.map((p) => p.end)) / 60) + 1);
  const hours = Array.from({ length: lastHour - firstHour }, (_, i) => firstHour + i);
  const gridHeight = hours.length * HOUR_HEIGHT;

  const dateIndex = dates.indexOf(activeDate);
  const formattedDate = new Date(activeDate + 'T00:00:00').toLocaleDateString(undefined, {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });

  return (
    <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200/80 dark:border-slate-800 p-4 sm:p-6 shadow-xs">
      {/* Day navigation */}
      <div className="flex items-center justify-between gap-3 pb-4 mb-4 border-b border-slate-100 dark:border-slate-800">
        <button
          onClick={() => setActiveDate(dates[dateIndex - 1])}
          disabled={dateIndex <= 0}
          className="p-2 rounded-lg text-slate-500 hover:bg-slate-100 dark:hover:bg-slate-800 disabled:opacity-30 disabled:hover:bg-transparent transition"
          title="Previous day"
        >
          <ChevronLeft className="w-4 h-4" />
        </button>
        <div className="text-center">
          <h3 className="text-lg font-bold text-slate-900 dark:text-white">{formattedDate}</h3>
          <span className="text-xs font-medium text-slate-400">
            {daySessions.length} session{daySessions.length !== 1 ? 's' : ''}
          </span>
        </div>
        <button
          onClick={() => setActiveDate(dates[dateIndex + 1])}
          disabled={dateIndex >= dates.length - 1}
          className="p-2 rounded-lg text-slate-500 hover:bg-slate-100 dark:hover:bg-slate-800 disabled:opacity-30 disabled:hover:bg-transparent transition"
          title="Next day"
        >
          <ChevronRight className="w-4 h-4" />
        </button>
      </div>

      {dates.length > 1 && (
        <div className="flex gap-1.5 overflow-x-auto pb-3 mb-3">
          {dates.map((d) => (
            <button
              key={d}
              onClick={() => setActiveDate(d)}
              className={`px-2.5 py-1 rounded-lg text-xs font-mono font-semibold border shrink-0 transition ${
                d === activeDate
                  ? 'bg-amber-50 dark:bg-amber-950/40 text-amber-700 dark:text-amber-300 border-amber-200 dark:border-amber-900/40'
                  : 'text-slate-500 border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-800'
              }`}
            >
              {d}
            </button>
          ))}
        </div>
      )}

      {/* Time grid */}
      <div className="relative flex" style={{ height: gridHeight }}>
        <div className="w-12 shrink-0 relative">
          {hours.map((h, i) => (
            <span
              key={h}
              className="absolute right-2 -translate-y-1/2 text-[11px] font-mono text-slate-400"
              style={{ top: i * HOUR_HEIGHT, display: i === 0 ? 'none' : undefined }}
            >
              {String(h).padStart(2, '0')}:00
            </span>
          ))}
        </div>

        <div className="relative flex-1 border-l border-slate-200 dark:border-slate-800">
          {hours.map((h, i) => (
            <div
              key={h}
              className="absolute inset-x-0 border-t border-slate-100 dark:border-slate-800"
              style={{ top: i * HOUR_HEIGHT }}
            />
          ))}

          {positioned.map(({ session, start, end, column }) => {
            const top = ((start - firstHour * 60) / 60) * HOUR_HEIGHT;
            const height = Math.max(((end - start) / 60) * HOUR_HEIGHT - 2, 20);
            const left = Math.min(column * INDENT_PERCENT, 100 - INDENT_PERCENT * 2);
            const compact = height < 44;
            return (
              <button
                key={session.id}
                onClick={() => onEdit(session)}
                title={`${session.title}\n${session.startTime} - ${session.endTime}\n${session.locationName}`}
                className="absolute text-left rounded-lg border border-black/30 dark:border-white/20 px-2 py-1 overflow-hidden text-white shadow-sm hover:brightness-110 hover:z-50 transition"
                style={{
                  top,
                  height,
                  left: `${left}%`,
                  width: `${100 - left}%`,
                  zIndex: column + 1,
                  backgroundColor: session.colorHex,
                }}
              >
                {compact ? (
                  <div className="text-xs leading-tight truncate">
                    <span className="font-bold">{session.title}</span>, {session.startTime} · {session.locationName}
                  </div>
                ) : (
                  <>
                    <div className="text-xs font-bold leading-tight line-clamp-2">{session.title}</div>
                    <div className="text-[11px] leading-tight opacity-90">
                      {session.startTime} – {session.endTime}
                    </div>
                    <div className="text-[11px] leading-tight opacity-90 truncate">{session.locationName}</div>
                  </>
                )}
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
};
