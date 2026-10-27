import React from 'react';
import { Star } from 'lucide-react';

/** The gold star shown at the left of a starred session's title. */
export const SessionStar: React.FC<{ className?: string }> = ({ className = 'w-4 h-4' }) => (
  <Star
    className={`inline-block shrink-0 align-[-0.125em] mr-1 fill-amber-400 text-amber-400 ${className}`}
    aria-label="Starred"
  />
);

interface StarToggleButtonProps {
  starred: boolean;
  onToggle: () => void;
  className?: string;
}

/** Icon button that stars or unstars a session. */
export const StarToggleButton: React.FC<StarToggleButtonProps> = ({ starred, onToggle, className = 'p-1.5' }) => (
  <button
    type="button"
    onClick={onToggle}
    className={`${className} rounded-lg transition ${
      starred
        ? 'text-amber-400 hover:bg-amber-50 dark:hover:bg-amber-950/40'
        : 'text-slate-400 hover:text-amber-500 hover:bg-amber-50 dark:hover:bg-amber-950/40'
    }`}
    title={starred ? 'Unstar session' : 'Star session'}
    aria-pressed={starred}
  >
    <Star className={`w-4 h-4 ${starred ? 'fill-amber-400' : ''}`} />
  </button>
);
