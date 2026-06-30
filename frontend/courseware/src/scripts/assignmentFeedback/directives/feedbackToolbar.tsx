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

import browser from 'jquery.browser';
import React, { useEffect, useReducer, useRef } from 'react';

import { AudioFeedback } from '../feedbackHandlers/AudioFeedback.tsx';
import { useTranslation } from '../../i18n/translationContext';
import settings from '../../utilities/settingsService';

interface FeedbackToolbarProps {
  /** The Angular `FeedbackManager` model (stays Angular) — mutated in place. */
  feedbackManager: any;
}

// Audio recording is only offered on browsers known to support it (mirrors the old directive's check).
const checkAudioSupport = () =>
  !browser.unknown &&
  !browser.msie &&
  !browser.safari &&
  settings.isFeatureEnabled('AllowAudioRecording');

// Unique per-mount id so the file input can be located as `#ngf-<toolId>` — the courseware
// DiscussionPage Selenide helper reads the `.attachment-tool` id and uploads to `#ngf-<id>` (the id
// shape ng-file-upload's `ngf-select` used to generate). Avoid React `useId()` here: its `:r0:` form is
// not a valid CSS selector for `#ngf-…`.
let uploadSeq = 0;

const ACTIVE_TOOL_POLL_MS = 250;

/**
 * React port of the `feedbackToolbar` directive (assignmentFeedback): the attachment toolbar under a
 * feedback/answer editor — an upload-file tool and (where supported) a record-audio tool, plus the
 * live audio recorder once that tool is active. Was an Angular directive; now native React, rendered
 * directly by the now-React `feedbackTools` (`<FeedbackToolbar>`). The
 * `FeedbackManager` model stays Angular (mutated in place); the live audio recorder is now native React
 * too (`<AudioFeedback>` → `<AudioRecorder>`). `ngf-select` is replaced by a native hidden file input.
 *
 * DOM preserved for Selenide: `.attachment-toolbar`, `.attachment-tool` (upload = first), the upload
 * tool's `id="feedback-upload-<n>"` + its `#ngf-<id>` file input, `.icon-upload`/`.icon-mic`,
 * `.attachment-active-tool` + `.feedback-type` (audio).
 */
export const FeedbackToolbar: React.FC<FeedbackToolbarProps> = ({ feedbackManager }) => {
  const translate = useTranslation();
  const [, forceRender] = useReducer((c: number) => c + 1, 0);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const uploadId = useRef('feedback-upload-' + ++uploadSeq).current;

  const audioSupported = checkAudioSupport();

  // `feedbackManager.activeTool` is mutated outside React's knowledge — the `AudioFeedback` callbacks
  // (cancel / accept call `updateActiveTool(null)`) and `updateActiveTool` below mutate it in place. This
  // component is a LEARNER-context leaf (not under the grader's GraderProvider), so poll the active tool
  // and re-render when it changes to toggle the recorder ↔ tools view (the mutable-model poll pattern
  // used by EssayQuestionBaseView for feedbackManager.files).
  const lastToolRef = useRef<any>(feedbackManager && feedbackManager.activeTool);
  useEffect(() => {
    if (!feedbackManager) return;
    const id = window.setInterval(() => {
      if (feedbackManager.activeTool !== lastToolRef.current) {
        lastToolRef.current = feedbackManager.activeTool;
        forceRender();
      }
    }, ACTIVE_TOOL_POLL_MS);
    return () => window.clearInterval(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [feedbackManager]);

  const updateActiveTool = (type: string) => {
    if (feedbackManager.activeTool === type || (type === 'audio' && !audioSupported)) return;
    feedbackManager.updateActiveTool(type);
    lastToolRef.current = feedbackManager.activeTool;
    forceRender();
  };

  const onFileSelect: React.ChangeEventHandler<HTMLInputElement> = e => {
    const files = e.target.files;
    if (files && files[0]) {
      feedbackManager.addFile(files[0]);
    }
    // Clear so re-selecting the same file still fires a change event (copied from the old loUpload).
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  if (!feedbackManager) return null;

  return (
    <div className="attachment-toolbar">
      <div className="attachment-active-tool">
        {feedbackManager.activeTool === 'audio' && <AudioFeedback feedbackManager={feedbackManager} />}
      </div>

      {feedbackManager.activeTool !== 'audio' && (
        <div
          className="attachment-all-tools"
          role="group"
          aria-label={translate('MEDIA_FEEDBACK_TOOLBAR')}
        >
          {/* eslint-disable-next-line jsx-a11y/no-static-element-interactions, jsx-a11y/click-events-have-key-events */}
          <span
            className="attachment-tool"
            id={uploadId}
            onClick={() => {
              updateActiveTool('file');
              fileInputRef.current?.click();
            }}
          >
            <span
              className="icon icon-upload"
              aria-hidden="true"
            />
            <span>{translate('MEDIA_FEEDBACK_UPLOAD_FILE')}</span>
            <input
              ref={fileInputRef}
              id={'ngf-' + uploadId}
              type="file"
              style={{ display: 'none' }}
              onChange={onFileSelect}
              onClick={e => e.stopPropagation()}
            />
          </span>

          {audioSupported && (
            // eslint-disable-next-line jsx-a11y/no-static-element-interactions, jsx-a11y/click-events-have-key-events
            <span
              className="attachment-tool"
              onClick={() => updateActiveTool('audio')}
            >
              <span className="icon icon-mic" />
              <span>{translate('MEDIA_FEEDBACK_RECORD_AUDIO')}</span>
            </span>
          )}
        </div>
      )}
    </div>
  );
};

