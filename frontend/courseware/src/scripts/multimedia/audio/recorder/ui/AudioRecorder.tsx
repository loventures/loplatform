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

import classnames from 'classnames';
import * as React from 'react';

import LoadingSpinner from '../../../../directives/loadingSpinner/index.tsx';
import { formatDuration } from '../../../../filters/pure/formatDuration.ts';
import { useTranslation } from '../../../../i18n/translationContext.tsx';
import { type AudioRecording, useAudioRecorder } from './useAudioRecorder.ts';

interface AudioRecorderProps {
  onAccept: (recording: AudioRecording) => void;
  onChange?: (recording?: AudioRecording) => void;
  onCancel?: () => void;
}

/**
 * React port of the `audioRecorder` Angular component (multimedia/audio/recorder/ui). The recording
 * lifecycle lives in `useAudioRecorder`; this is the UI. DOM preserved from `audioRecorder.directive.html`:
 * `.audio-recorder`, the `.icon-controller-record/-paus/-stop` + `.icon-checkmark/-cross` buttons, the
 * `formatDuration` timer, and the `<audio.recording controls>` playback element (src set imperatively).
 */
export const AudioRecorder: React.FC<AudioRecorderProps> = ({ onAccept, onChange, onCancel }) => {
  const translate = useTranslation();
  const r = useAudioRecorder({ onChange });

  if (!r.ready) return <LoadingSpinner />;
  if (!r.audioSupported) return <span>{translate('AUDIO_RECORDER_NO_SUPPORT')}</span>;

  return (
    <div className="audio-recorder m-2">
      {r.recording && (
        <div className="flex-row-content">
          <button
            className="btn btn-sm btn-success"
            onClick={() => onAccept(r.recording!)}
          >
            <span className="icon icon-checkmark" />
            <span>{translate('AUDIO_RECORDER_ACCEPT')}</span>
          </button>
          <button
            className="btn btn-sm btn-danger"
            onClick={r.clear}
          >
            <span className="icon icon-cross" />
            <span>{translate('AUDIO_RECORDER_CLEAR')}</span>
          </button>
        </div>
      )}

      {!r.recording && (
        <div className="flex-row-content">
          {r.canRecord && (
            <button
              className="btn btn-sm btn-info"
              onClick={r.startRecording}
            >
              <span className={classnames('icon icon-controller-record', { recording: r.isRecording })} />
              <span>{translate('Record')}</span>
            </button>
          )}
          {r.supportsPause && r.canPause && (
            <button
              className="btn btn-sm btn-info"
              onClick={r.pauseRecording}
            >
              <span className={classnames('icon icon-controller-paus', { paused: r.isPaused })} />
              <span>{translate('AUDIO_RECORDER_PAUSE')}</span>
            </button>
          )}
          <button
            className="btn btn-sm btn-info"
            onClick={r.stopRecording}
            disabled={!r.canStop}
          >
            <span className="icon icon-controller-stop" />
            <span>{translate('AUDIO_RECORDER_STOP')}</span>
          </button>
          {onCancel && (
            <button
              className="btn btn-sm btn-danger"
              onClick={onCancel}
            >
              <span className="icon icon-cross" />
              <span>{translate('AUDIO_RECORDER_CANCEL')}</span>
            </button>
          )}
        </div>
      )}

      {!r.recording && <span className="m-2">{formatDuration(r.elapsedMs)}</span>}

      {/* base64 src is set imperatively in the hook (huge string); shown once a recording exists */}
      <audio
        ref={r.audioRef}
        className="recording"
        controls
        style={{ display: r.recording ? undefined : 'none' }}
      />
    </div>
  );
};
