/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import { useEffect, useRef } from 'react';

const AUTOSAVE_INTERVAL = 3 * 60 * 1000;
const AUTOSAVE_CHANGE_DELAY = 30 * 1000;

interface QuizAutosaveArgs {
  /** Whether to run the timer at all (the old `<quiz-autosave ng-if="enableAutosave">`). */
  enabled?: boolean;
  hasChanges: boolean;
  save: () => void;
  lastChange: number;
  lastSave: number;
}

/**
 * React port of the (UI-less) `quizAutosave` component: a debounced autosave timer. Schedules a save for
 * `min(lastSave + 3min, lastChange + 30s)`; when it fires it saves iff there are unsaved changes, then
 * re-schedules off "now" (settling into the 3-minute interval when idle). The timer is reset whenever
 * `hasChanges`/`lastChange`/`lastSave` change — exactly the old `$onChanges` → `estimateNextSave`. The
 * original's bogus `state.loading` guard (it read `quizAttemptAutoSaveState[undefined]`, always `{}`) is
 * dropped as a no-op. `$timeout` → `setTimeout`; the players already re-render off the redux store.
 */
export const useQuizAutosave = ({
  enabled = true,
  hasChanges,
  save,
  lastChange,
  lastSave,
}: QuizAutosaveArgs): void => {
  const ref = useRef({ hasChanges, save });
  ref.current = { hasChanges, save };

  useEffect(() => {
    if (!enabled) return;

    let timeout: ReturnType<typeof setTimeout>;
    let lc = lastChange;
    let ls = lastSave;

    const autoSave = () => {
      if (ref.current.hasChanges) {
        ref.current.save();
      }
      // prevent spamming if it fails
      ls = Date.now();
      lc = ls;
      schedule();
    };

    const schedule = () => {
      const nextSave = Math.min(ls + AUTOSAVE_INTERVAL, lc + AUTOSAVE_CHANGE_DELAY) - Date.now();
      clearTimeout(timeout);
      timeout = setTimeout(autoSave, Math.max(0, nextSave));
    };

    schedule();
    return () => clearTimeout(timeout);
  }, [enabled, hasChanges, lastChange, lastSave]);
};

export default useQuizAutosave;
