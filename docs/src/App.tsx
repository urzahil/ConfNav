/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect, useMemo } from 'react';
import { ConferenceSession } from './types';
import { loadSavedSessions, saveSessions } from './utils/storage';
import { downloadJsonFile } from './utils/exportImport';
import { SessionCard } from './components/SessionCard';
import { SessionFormModal } from './components/SessionFormModal';
import { LoadSessionsModal } from './components/LoadSessionsModal';
import { JsonViewerModal } from './components/JsonViewerModal';
import { MapView } from './components/MapView';
import { TimelineAgendaView } from './components/TimelineAgendaView';
import { ConfirmDialogModal } from './components/ConfirmDialogModal';
import {
  Calendar,
  Plus,
  Upload,
  Download,
  FileCode,
  Search,
  MapPin,
  Clock,
  Layers,
  Trash2,
  CheckCircle2,
  CalendarDays,
  Users
} from 'lucide-react';

export default function App() {
  const [sessions, setSessions] = useState<ConferenceSession[]>(() => loadSavedSessions());
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [selectedDate, setSelectedDate] = useState<string>('all');
  const [activeView, setActiveView] = useState<'grid' | 'timeline' | 'map'>('grid');

  // Modals state
  const [isFormOpen, setIsFormOpen] = useState<boolean>(false);
  const [editingSession, setEditingSession] = useState<ConferenceSession | null>(null);
  const [isLoadModalOpen, setIsLoadModalOpen] = useState<boolean>(false);
  const [isJsonModalOpen, setIsJsonModalOpen] = useState<boolean>(false);
  const [focusedSessionId, setFocusedSessionId] = useState<string | null>(null);

  // In-app confirmation dialog state (avoiding blocked window.confirm in iframe)
  const [confirmDialog, setConfirmDialog] = useState<{
    isOpen: boolean;
    title: string;
    message: React.ReactNode;
    confirmLabel: string;
    variant: 'danger' | 'warning' | 'primary';
    onConfirm: () => void;
  }>({
    isOpen: false,
    title: '',
    message: '',
    confirmLabel: 'Confirm',
    variant: 'danger',
    onConfirm: () => {},
  });

  // Toast feedback
  const [toastMessage, setToastMessage] = useState<{ text: string; type: 'success' | 'info' } | null>(null);

  // Save to localStorage whenever sessions change
  useEffect(() => {
    saveSessions(sessions);
  }, [sessions]);

  const showToast = (text: string, type: 'success' | 'info' = 'success') => {
    setToastMessage({ text, type });
    setTimeout(() => {
      setToastMessage(null);
    }, 2800);
  };

  // Distinct dates for filter
  const uniqueDates = useMemo(() => {
    const set = new Set(sessions.map((s) => s.date).filter(Boolean));
    return Array.from(set).sort();
  }, [sessions]);

  // Distinct speakers count
  const speakerCount = useMemo(() => {
    return new Set(sessions.map((s) => s.speaker.trim().toLowerCase()).filter(Boolean)).size;
  }, [sessions]);

  // Distinct rooms count
  const roomCount = useMemo(() => {
    return new Set(sessions.map((s) => s.locationName.trim().toLowerCase()).filter(Boolean)).size;
  }, [sessions]);

  // Filtered sessions
  const filteredSessions = useMemo(() => {
    return sessions.filter((s) => {
      const matchesSearch =
        s.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
        s.speaker.toLowerCase().includes(searchQuery.toLowerCase()) ||
        s.locationName.toLowerCase().includes(searchQuery.toLowerCase()) ||
        s.address.toLowerCase().includes(searchQuery.toLowerCase());

      const matchesDate = selectedDate === 'all' || s.date === selectedDate;

      return matchesSearch && matchesDate;
    });
  }, [sessions, searchQuery, selectedDate]);

  // Handlers
  const handleCreateNew = () => {
    setEditingSession(null);
    setIsFormOpen(true);
  };

  const handleEdit = (session: ConferenceSession) => {
    setEditingSession(session);
    setIsFormOpen(true);
  };

  const handleDuplicate = (session: ConferenceSession) => {
    const duplicated: ConferenceSession = {
      ...session,
      id: `session-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`,
      title: `${session.title} (Copy)`,
    };
    setSessions((prev) => [duplicated, ...prev]);
    showToast(`Duplicated "${session.title}"`);
  };

  const handleDelete = (id: string) => {
    const sessionToDelete = sessions.find((s) => s.id === id);
    if (!sessionToDelete) return;

    setConfirmDialog({
      isOpen: true,
      title: 'Delete Session',
      message: (
        <div>
          <p>
            Are you sure you want to delete session{' '}
            <strong className="text-slate-900 dark:text-white">
              "{sessionToDelete.title}"
            </strong>{' '}
            by <span className="font-semibold">{sessionToDelete.speaker}</span>?
          </p>
          <p className="mt-2 text-[11px] text-slate-400">
            This action removes the event from your schedule immediately.
          </p>
        </div>
      ),
      confirmLabel: 'Delete Session',
      variant: 'danger',
      onConfirm: () => {
        setSessions((prev) => prev.filter((s) => s.id !== id));
        showToast(`Deleted "${sessionToDelete.title}"`);
      },
    });
  };

  const handleSaveSession = (savedSession: ConferenceSession) => {
    if (editingSession) {
      setSessions((prev) =>
        prev.map((s) => (s.id === savedSession.id ? savedSession : s))
      );
      showToast('Session updated successfully');
    } else {
      setSessions((prev) => [savedSession, ...prev]);
      showToast('New session created successfully');
    }
  };

  const handleImportSessions = (
    imported: ConferenceSession[],
    mode: 'replace' | 'append'
  ) => {
    if (mode === 'replace') {
      setSessions(imported);
      showToast(`Replaced schedule with ${imported.length} session(s)`);
    } else {
      setSessions((prev) => [...prev, ...imported]);
      showToast(`Appended ${imported.length} new session(s)`);
    }
  };

  const handleQuickDownloadJson = () => {
    downloadJsonFile(sessions, `conference-sessions-${new Date().toISOString().split('T')[0]}.json`);
    showToast('Downloaded conference-sessions.json');
  };

  const handleClearAll = () => {
    setConfirmDialog({
      isOpen: true,
      title: 'Clear All Sessions',
      message: (
        <div>
          <p>
            Are you sure you want to delete all <strong className="text-slate-900 dark:text-white">{sessions.length}</strong> conference sessions?
          </p>
          <p className="mt-2 text-[11px] text-rose-600 dark:text-rose-400">
            Make sure you have exported a JSON backup if you need to keep them.
          </p>
        </div>
      ),
      confirmLabel: 'Clear All',
      variant: 'danger',
      onConfirm: () => {
        setSessions([]);
        showToast('Cleared all sessions');
      },
    });
  };

  const handleViewOnMap = (session: ConferenceSession) => {
    setFocusedSessionId(session.id);
    setActiveView('map');
  };

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 font-sans selection:bg-amber-500 selection:text-white">
      {/* Toast Notification */}
      {toastMessage && (
        <div className="fixed bottom-6 right-6 z-50 animate-bounce duration-300">
          <div className="bg-slate-900 dark:bg-white text-white dark:text-slate-900 px-4 py-3 rounded-2xl shadow-xl flex items-center gap-2.5 text-xs font-semibold">
            <CheckCircle2 className="w-4 h-4 text-emerald-400 dark:text-emerald-600" />
            <span>{toastMessage.text}</span>
          </div>
        </div>
      )}

      {/* Main Top Navbar */}
      <header className="sticky top-0 z-40 bg-white/80 dark:bg-slate-900/80 backdrop-blur-md border-b border-slate-200/80 dark:border-slate-800">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16 gap-4">
            {/* Logo & Title */}
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-amber-500 to-amber-600 flex items-center justify-center text-white shadow-sm shadow-amber-500/20">
                <CalendarDays className="w-5 h-5" />
              </div>
              <div>
                <h1 className="text-base sm:text-lg font-bold tracking-tight text-slate-900 dark:text-white leading-none">
                  Conference Session Builder
                </h1>
                <p className="text-[11px] font-medium text-slate-500 dark:text-slate-400 hidden sm:block mt-0.5">
                  Visual session manager, map locator, and JSON exporter
                </p>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="flex items-center gap-2">
              <button
                onClick={() => setIsLoadModalOpen(true)}
                className="px-3 py-2 rounded-xl border border-slate-300 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-800 text-xs font-semibold text-slate-700 dark:text-slate-200 flex items-center gap-1.5 transition shadow-2xs"
                title="Load sessions from a JSON file or paste text"
              >
                <Upload className="w-3.5 h-3.5 text-amber-600" />
                <span className="hidden sm:inline">Load</span> JSON
              </button>

              <div className="relative group">
                <button
                  onClick={handleQuickDownloadJson}
                  className="px-3 py-2 rounded-xl border border-slate-300 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-800 text-xs font-semibold text-slate-700 dark:text-slate-200 flex items-center gap-1.5 transition shadow-2xs"
                  title="Export events as JSON file"
                >
                  <Download className="w-3.5 h-3.5 text-amber-600" />
                  <span className="hidden sm:inline">Export</span> JSON
                </button>
              </div>

              <button
                onClick={() => setIsJsonModalOpen(true)}
                className="px-3 py-2 rounded-xl border border-slate-300 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-800 text-xs font-semibold text-slate-700 dark:text-slate-200 flex items-center gap-1.5 transition shadow-2xs"
                title="Inspect formatted JSON code"
              >
                <FileCode className="w-3.5 h-3.5 text-slate-500" />
                <span className="hidden md:inline">Inspect</span>
              </button>

              <button
                onClick={handleCreateNew}
                className="px-4 py-2 rounded-xl bg-amber-600 hover:bg-amber-700 text-white text-xs font-bold flex items-center gap-1.5 transition shadow-sm hover:shadow-md"
              >
                <Plus className="w-4 h-4" />
                <span>New Session</span>
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Main Container */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 space-y-6">
        {/* Quick Stats Header Cards */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <div className="bg-white dark:bg-slate-900 p-4 rounded-2xl border border-slate-200/80 dark:border-slate-800 shadow-2xs flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-amber-50 dark:bg-amber-950/50 text-amber-600 dark:text-amber-400">
              <Layers className="w-5 h-5" />
            </div>
            <div>
              <div className="text-xl font-black text-slate-900 dark:text-white leading-tight">
                {sessions.length}
              </div>
              <div className="text-xs font-medium text-slate-500 dark:text-slate-400">
                Total Sessions
              </div>
            </div>
          </div>

          <div className="bg-white dark:bg-slate-900 p-4 rounded-2xl border border-slate-200/80 dark:border-slate-800 shadow-2xs flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-blue-50 dark:bg-blue-950/50 text-blue-600 dark:text-blue-400">
              <Users className="w-5 h-5" />
            </div>
            <div>
              <div className="text-xl font-black text-slate-900 dark:text-white leading-tight">
                {speakerCount}
              </div>
              <div className="text-xs font-medium text-slate-500 dark:text-slate-400">
                Speakers
              </div>
            </div>
          </div>

          <div className="bg-white dark:bg-slate-900 p-4 rounded-2xl border border-slate-200/80 dark:border-slate-800 shadow-2xs flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-emerald-50 dark:bg-emerald-950/50 text-emerald-600 dark:text-emerald-400">
              <Calendar className="w-5 h-5" />
            </div>
            <div>
              <div className="text-xl font-black text-slate-900 dark:text-white leading-tight">
                {uniqueDates.length}
              </div>
              <div className="text-xs font-medium text-slate-500 dark:text-slate-400">
                Conference Days
              </div>
            </div>
          </div>

          <div className="bg-white dark:bg-slate-900 p-4 rounded-2xl border border-slate-200/80 dark:border-slate-800 shadow-2xs flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-purple-50 dark:bg-purple-950/50 text-purple-600 dark:text-purple-400">
              <MapPin className="w-5 h-5" />
            </div>
            <div>
              <div className="text-xl font-black text-slate-900 dark:text-white leading-tight">
                {roomCount}
              </div>
              <div className="text-xs font-medium text-slate-500 dark:text-slate-400">
                Distinct Rooms
              </div>
            </div>
          </div>
        </div>

        {/* Toolbar: Search, Date Filter, and View Switcher */}
        <div className="bg-white dark:bg-slate-900 rounded-2xl p-4 border border-slate-200/80 dark:border-slate-800 shadow-2xs flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex flex-1 flex-col sm:flex-row items-stretch sm:items-center gap-3">
            {/* Search */}
            <div className="relative flex-1 max-w-md">
              <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search title, speaker, room, address..."
                className="w-full pl-9 pr-4 py-2 rounded-xl border border-slate-300 dark:border-slate-700 text-xs font-medium bg-slate-50/50 dark:bg-slate-800/60 dark:text-white focus:outline-none focus:ring-2 focus:ring-amber-500"
              />
              {searchQuery && (
                <button
                  onClick={() => setSearchQuery('')}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-slate-400 hover:text-slate-600"
                >
                  Clear
                </button>
              )}
            </div>

            {/* Date filter dropdown */}
            {uniqueDates.length > 0 && (
              <div className="flex items-center gap-2">
                <select
                  value={selectedDate}
                  onChange={(e) => setSelectedDate(e.target.value)}
                  className="px-3 py-2 rounded-xl border border-slate-300 dark:border-slate-700 text-xs font-medium bg-slate-50/50 dark:bg-slate-800/60 dark:text-white focus:outline-none focus:ring-2 focus:ring-amber-500"
                >
                  <option value="all">All Dates ({uniqueDates.length})</option>
                  {uniqueDates.map((date) => {
                    const count = sessions.filter((s) => s.date === date).length;
                    return (
                      <option key={date} value={date}>
                        {date} ({count} {count === 1 ? 'session' : 'sessions'})
                      </option>
                    );
                  })}
                </select>
              </div>
            )}
          </div>

          {/* View Mode switcher */}
          <div className="flex items-center justify-between sm:justify-end gap-2 border-t md:border-t-0 pt-3 md:pt-0 border-slate-100 dark:border-slate-800">
            <div className="flex p-1 rounded-xl bg-slate-100 dark:bg-slate-800">
              <button
                onClick={() => setActiveView('grid')}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition ${
                  activeView === 'grid'
                    ? 'bg-white dark:bg-slate-700 text-slate-900 dark:text-white shadow-2xs'
                    : 'text-slate-500 hover:text-slate-900 dark:hover:text-slate-200'
                }`}
              >
                <Layers className="w-3.5 h-3.5" />
                Cards
              </button>
              <button
                onClick={() => setActiveView('timeline')}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition ${
                  activeView === 'timeline'
                    ? 'bg-white dark:bg-slate-700 text-slate-900 dark:text-white shadow-2xs'
                    : 'text-slate-500 hover:text-slate-900 dark:hover:text-slate-200'
                }`}
              >
                <Clock className="w-3.5 h-3.5" />
                Agenda
              </button>
              <button
                onClick={() => setActiveView('map')}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition ${
                  activeView === 'map'
                    ? 'bg-white dark:bg-slate-700 text-slate-900 dark:text-white shadow-2xs'
                    : 'text-slate-500 hover:text-slate-900 dark:hover:text-slate-200'
                }`}
              >
                <MapPin className="w-3.5 h-3.5" />
                Map
              </button>
            </div>

            {/* Quick helper menu */}
            {sessions.length > 0 && (
              <div className="flex items-center gap-1">
                <button
                  onClick={handleClearAll}
                  className="p-2 rounded-xl text-slate-400 hover:text-rose-600 hover:bg-rose-50 dark:hover:bg-rose-950/40 transition"
                  title="Clear all sessions"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Content Views */}
        {sessions.length === 0 ? (
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200/80 dark:border-slate-800 p-12 text-center max-w-lg mx-auto shadow-sm">
            <div className="w-16 h-16 rounded-3xl bg-amber-100 dark:bg-amber-950/60 text-amber-600 dark:text-amber-400 flex items-center justify-center mx-auto mb-4">
              <CalendarDays className="w-8 h-8" />
            </div>
            <h3 className="text-lg font-bold text-slate-900 dark:text-white mb-1">
              No Conference Sessions Yet
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 mb-6 leading-relaxed">
              Create your first conference session or load an existing JSON file.
            </p>
            <div className="flex flex-wrap items-center justify-center gap-3">
              <button
                onClick={handleCreateNew}
                className="px-4 py-2.5 rounded-xl bg-amber-600 hover:bg-amber-700 text-white text-xs font-bold shadow-md transition flex items-center gap-1.5"
              >
                <Plus className="w-4 h-4" />
                Add Session
              </button>
              <button
                onClick={() => setIsLoadModalOpen(true)}
                className="px-4 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-800 text-xs font-semibold text-slate-700 dark:text-slate-300 transition flex items-center gap-1.5"
              >
                <Upload className="w-4 h-4" />
                Load JSON File
              </button>
            </div>
          </div>
        ) : (
          <>
            {/* View 1: Card Grid */}
            {activeView === 'grid' && (
              <>
                {filteredSessions.length === 0 ? (
                  <div className="bg-white dark:bg-slate-900 rounded-2xl p-10 text-center border border-slate-200 dark:border-slate-800">
                    <p className="text-sm font-semibold text-slate-600 dark:text-slate-400">
                      No sessions match your search & filter criteria.
                    </p>
                    <button
                      onClick={() => {
                        setSearchQuery('');
                        setSelectedDate('all');
                      }}
                      className="mt-3 text-xs font-semibold text-amber-600 hover:underline"
                    >
                      Clear search filters
                    </button>
                  </div>
                ) : (
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
                    {filteredSessions.map((session) => (
                      <SessionCard
                        key={session.id}
                        session={session}
                        onEdit={handleEdit}
                        onDuplicate={handleDuplicate}
                        onDelete={handleDelete}
                        onViewOnMap={handleViewOnMap}
                      />
                    ))}
                  </div>
                )}
              </>
            )}

            {/* View 2: Chronological Agenda Timeline */}
            {activeView === 'timeline' && (
              <TimelineAgendaView
                sessions={filteredSessions}
                onEdit={handleEdit}
                onDelete={handleDelete}
              />
            )}

            {/* View 3: Full Interactive Map */}
            {activeView === 'map' && (
              <div className="space-y-4">
                <MapView
                  sessions={filteredSessions}
                  onEditSession={handleEdit}
                  selectedSessionId={focusedSessionId}
                />
              </div>
            )}
          </>
        )}
      </main>

      {/* Modals */}
      <SessionFormModal
        isOpen={isFormOpen}
        onClose={() => {
          setIsFormOpen(false);
          setEditingSession(null);
        }}
        onSave={handleSaveSession}
        initialSession={editingSession}
      />

      <LoadSessionsModal
        isOpen={isLoadModalOpen}
        onClose={() => setIsLoadModalOpen(false)}
        onImport={handleImportSessions}
        currentCount={sessions.length}
      />

      <JsonViewerModal
        isOpen={isJsonModalOpen}
        onClose={() => setIsJsonModalOpen(false)}
        sessions={sessions}
      />

      {/* Confirmation Dialog */}
      <ConfirmDialogModal
        isOpen={confirmDialog.isOpen}
        title={confirmDialog.title}
        message={confirmDialog.message}
        confirmLabel={confirmDialog.confirmLabel}
        variant={confirmDialog.variant}
        onConfirm={confirmDialog.onConfirm}
        onCancel={() => setConfirmDialog((prev) => ({ ...prev, isOpen: false }))}
      />
    </div>
  );
}
