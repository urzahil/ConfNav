import React, { useState, useRef } from 'react';
import { ConferenceSession } from '../types';
import { 
  validateAndParseSessionsJson, 
  readFileAsText, 
  ValidationResult 
} from '../utils/exportImport';
import { 
  X, 
  Upload, 
  FileText, 
  AlertCircle, 
  CheckCircle, 
  FileUp, 
  RotateCcw
} from 'lucide-react';

interface LoadSessionsModalProps {
  isOpen: boolean;
  onClose: () => void;
  onImport: (sessions: ConferenceSession[], mode: 'replace' | 'append') => void;
  currentCount: number;
}

export const LoadSessionsModal: React.FC<LoadSessionsModalProps> = ({
  isOpen,
  onClose,
  onImport,
  currentCount,
}) => {
  const [activeTab, setActiveTab] = useState<'upload' | 'paste'>('upload');
  const [importMode, setImportMode] = useState<'replace' | 'append'>('replace');
  const [rawText, setRawText] = useState<string>('');
  const [uploadedFileName, setUploadedFileName] = useState<string>('');
  const [validation, setValidation] = useState<ValidationResult | null>(null);
  const [isDragging, setIsDragging] = useState<boolean>(false);
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  if (!isOpen) return null;

  const handleTextChange = (text: string) => {
    setRawText(text);
    if (!text.trim()) {
      setValidation(null);
      return;
    }
    const result = validateAndParseSessionsJson(text);
    setValidation(result);
  };

  const handleFile = async (file: File) => {
    if (!file) return;
    setUploadedFileName(file.name);
    try {
      const content = await readFileAsText(file);
      setRawText(content);
      const result = validateAndParseSessionsJson(content);
      setValidation(result);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : String(err);
      setValidation({
        valid: false,
        sessions: [],
        errors: [`Could not read file: ${message}`],
        warnings: [],
      });
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      handleFile(e.dataTransfer.files[0]);
    }
  };

  const handleConfirm = () => {
    if (!validation || !validation.valid || validation.sessions.length === 0) return;
    onImport(validation.sessions, importMode);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-slate-900/60 backdrop-blur-xs">
      <div 
        className="relative w-full max-w-2xl max-h-[92vh] flex flex-col bg-white dark:bg-slate-900 rounded-2xl shadow-2xl border border-slate-200 dark:border-slate-800 overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/40">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-amber-100 dark:bg-amber-950/60 text-amber-700 dark:text-amber-300">
              <Upload className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-slate-900 dark:text-white">
                Load Conference Sessions
              </h2>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Import existing sessions from a .json file or paste raw JSON
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

        {/* Tab Selector */}
        <div className="flex border-b border-slate-200 dark:border-slate-800 px-6 pt-3 gap-4 bg-slate-50/30 dark:bg-slate-900">
          <button
            onClick={() => setActiveTab('upload')}
            className={`pb-3 text-sm font-semibold flex items-center gap-2 border-b-2 transition ${
              activeTab === 'upload'
                ? 'border-amber-600 text-amber-600 dark:text-amber-400'
                : 'border-transparent text-slate-500 hover:text-slate-800 dark:hover:text-slate-200'
            }`}
          >
            <FileUp className="w-4 h-4" />
            Upload JSON File
          </button>
          <button
            onClick={() => setActiveTab('paste')}
            className={`pb-3 text-sm font-semibold flex items-center gap-2 border-b-2 transition ${
              activeTab === 'paste'
                ? 'border-amber-600 text-amber-600 dark:text-amber-400'
                : 'border-transparent text-slate-500 hover:text-slate-800 dark:hover:text-slate-200'
            }`}
          >
            <FileText className="w-4 h-4" />
            Paste JSON Text
          </button>
        </div>

        {/* Body Content */}
        <div className="flex-1 overflow-y-auto p-6 space-y-5">
          {activeTab === 'upload' ? (
            <div>
              <div
                onDragOver={(e) => {
                  e.preventDefault();
                  setIsDragging(true);
                }}
                onDragLeave={() => setIsDragging(false)}
                onDrop={handleDrop}
                onClick={() => fileInputRef.current?.click()}
                className={`border-2 border-dashed rounded-2xl p-8 text-center cursor-pointer transition ${
                  isDragging
                    ? 'border-amber-500 bg-amber-50/50 dark:bg-amber-950/20'
                    : 'border-slate-300 dark:border-slate-700 hover:border-amber-500 dark:hover:border-amber-500 bg-slate-50/50 dark:bg-slate-800/20'
                }`}
              >
                <input
                  type="file"
                  ref={fileInputRef}
                  accept=".json,application/json"
                  className="hidden"
                  onChange={(e) => {
                    if (e.target.files && e.target.files[0]) {
                      handleFile(e.target.files[0]);
                    }
                  }}
                />
                <div className="mx-auto w-12 h-12 rounded-full bg-amber-100 dark:bg-amber-950/50 text-amber-600 dark:text-amber-400 flex items-center justify-center mb-3">
                  <Upload className="w-6 h-6" />
                </div>
                <h4 className="text-sm font-bold text-slate-900 dark:text-white mb-1">
                  Click to browse or drag and drop your .json file here
                </h4>
                <p className="text-xs text-slate-400">
                  Accepts conference session JSON arrays
                </p>
                {uploadedFileName && (
                  <div className="mt-3 inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-amber-100 dark:bg-amber-950 text-amber-800 dark:text-amber-300 text-xs font-semibold">
                    <FileText className="w-3.5 h-3.5" />
                    {uploadedFileName}
                  </div>
                )}
              </div>
            </div>
          ) : (
            <div>
              <div className="flex items-center justify-between mb-2">
                <label className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                  Raw JSON Array
                </label>
              </div>
              <textarea
                value={rawText}
                onChange={(e) => handleTextChange(e.target.value)}
                placeholder="Paste JSON array here, e.g. [{ &quot;title&quot;: &quot;Test&quot;, ... }]"
                className="w-full h-44 p-3.5 rounded-xl border border-slate-300 dark:border-slate-700 font-mono text-xs bg-slate-50 dark:bg-slate-950 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-amber-500 resize-none"
              />
            </div>
          )}

          {/* Validation Feedback */}
          {validation && (
            <div className="space-y-2">
              {validation.valid ? (
                <div className="p-3.5 rounded-xl bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-900/50 flex items-start gap-3">
                  <CheckCircle className="w-5 h-5 text-emerald-600 dark:text-emerald-400 shrink-0 mt-0.5" />
                  <div>
                    <h5 className="text-sm font-bold text-emerald-800 dark:text-emerald-300">
                      Successfully detected {validation.sessions.length} session{validation.sessions.length !== 1 ? 's' : ''}!
                    </h5>
                    <p className="text-xs text-emerald-700 dark:text-emerald-400 mt-0.5">
                      Ready to import with full schedule, coordinate, and color data.
                    </p>
                  </div>
                </div>
              ) : (
                <div className="p-3.5 rounded-xl bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-900/50 flex items-start gap-3">
                  <AlertCircle className="w-5 h-5 text-rose-600 dark:text-rose-400 shrink-0 mt-0.5" />
                  <div className="text-xs text-rose-700 dark:text-rose-300 space-y-1">
                    <h5 className="font-bold text-rose-800 dark:text-rose-200 text-sm">
                      JSON Validation Error
                    </h5>
                    {validation.errors.map((err, i) => (
                      <p key={i}>• {err}</p>
                    ))}
                  </div>
                </div>
              )}

              {validation.warnings.length > 0 && (
                <div className="p-3 rounded-xl bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-900/50 text-xs text-amber-800 dark:text-amber-300">
                  <span className="font-bold">Notices: </span>
                  {validation.warnings.map((w, i) => (
                    <span key={i} className="block mt-0.5">• {w}</span>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* Import Mode: Replace vs Append */}
          <div className="border-t border-slate-200 dark:border-slate-800 pt-4">
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-500 mb-2">
              Import Strategy
            </label>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <label className={`p-3 rounded-xl border cursor-pointer flex items-start gap-3 transition ${
                importMode === 'replace'
                  ? 'border-amber-500 bg-amber-50/40 dark:bg-amber-950/30 text-amber-900 dark:text-amber-200 ring-1 ring-amber-500'
                  : 'border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-800/40 text-slate-700 dark:text-slate-300'
              }`}>
                <input
                  type="radio"
                  name="importMode"
                  checked={importMode === 'replace'}
                  onChange={() => setImportMode('replace')}
                  className="mt-1 text-amber-600 focus:ring-amber-500"
                />
                <div>
                  <div className="text-xs font-bold">Replace Current List</div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
                    Clear the current {currentCount} session(s) and use imported sessions
                  </div>
                </div>
              </label>

              <label className={`p-3 rounded-xl border cursor-pointer flex items-start gap-3 transition ${
                importMode === 'append'
                  ? 'border-amber-500 bg-amber-50/40 dark:bg-amber-950/30 text-amber-900 dark:text-amber-200 ring-1 ring-amber-500'
                  : 'border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-800/40 text-slate-700 dark:text-slate-300'
              }`}>
                <input
                  type="radio"
                  name="importMode"
                  checked={importMode === 'append'}
                  onChange={() => setImportMode('append')}
                  className="mt-1 text-amber-600 focus:ring-amber-500"
                />
                <div>
                  <div className="text-xs font-bold">Append to Current List</div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
                    Keep current {currentCount} session(s) and add new imported ones
                  </div>
                </div>
              </label>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/40 flex items-center justify-between">
          <button
            type="button"
            onClick={() => {
              setRawText('');
              setUploadedFileName('');
              setValidation(null);
            }}
            className="text-xs font-medium text-slate-500 hover:text-slate-800 dark:hover:text-slate-200 flex items-center gap-1"
          >
            <RotateCcw className="w-3.5 h-3.5" />
            Clear Input
          </button>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 text-xs font-semibold text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition"
            >
              Cancel
            </button>
            <button
              type="button"
              disabled={!validation || !validation.valid || validation.sessions.length === 0}
              onClick={handleConfirm}
              className="px-5 py-2.5 rounded-xl bg-amber-600 hover:bg-amber-700 disabled:opacity-50 text-white text-xs font-bold transition shadow-sm flex items-center gap-1.5"
            >
              <Upload className="w-4 h-4" />
              Import {validation?.sessions?.length ? `${validation.sessions.length} Session(s)` : 'Sessions'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
