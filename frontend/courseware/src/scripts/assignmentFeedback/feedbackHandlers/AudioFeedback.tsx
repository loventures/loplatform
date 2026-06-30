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

import * as React from 'react';

import { AudioRecorder } from '../../multimedia/audio/recorder/ui/AudioRecorder.tsx';
import { type AudioRecording } from '../../multimedia/audio/recorder/ui/useAudioRecorder.ts';

/**
 * React port of the `audioFeedback` Angular directive: wires the recorder's accept/change/cancel callbacks
 * into the (still-Angular) `FeedbackManager`. Accept attaches the recording's data (with url/viewUrl/base64)
 * as a staged file; change signals tool status; cancel clears the active tool. Replaces the
 * `<audio-feedback>` angular2react bridge — rendered directly by the React `FeedbackToolbar`.
 */
export const AudioFeedback: React.FC<{ feedbackManager: any }> = ({ feedbackManager }) => {
  const recordingAccepted = (recording: AudioRecording) => {
    recording.data.url = recording.url;
    recording.data.viewUrl = recording.url;
    recording.data.base64 = recording.base64;
    feedbackManager.addFile(recording.data);
  };
  const recordingChanged = (recording?: AudioRecording) => {
    feedbackManager.signalToolStatus(!!recording);
  };
  const recordingCancelled = () => {
    feedbackManager.updateActiveTool(null);
  };

  return (
    // `feeback-type-inner` (original typo) preserved for CSS.
    <div className="feeback-type-inner">
      <AudioRecorder
        onAccept={recordingAccepted}
        onChange={recordingChanged}
        onCancel={recordingCancelled}
      />
    </div>
  );
};
