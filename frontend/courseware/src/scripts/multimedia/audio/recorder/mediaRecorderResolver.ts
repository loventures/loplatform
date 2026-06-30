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

import HtmlRecorder from './html/htmlRecorder.js';
import { userMediaStreamService } from './userMediaStreamService.ts';

type Recorder = any;
type ResolveCb = (err: Error | null, recorder?: Recorder) => void;

/**
 * Pure-TS port of the `mediaRecorderResolver` Angular service: detects HTML5 audio support (polyfilling the
 * legacy `navigator.getUserMedia`) and, if supported, acquires the shared stream and builds an `HtmlRecorder`
 * (MediaRecorder / Recorder.js / WAV). Imported directly by the React `useAudioRecorder` hook.
 */
const resolveRecorder = (cb: ResolveCb): void => {
  const nav = window.navigator as any;
  nav.getUserMedia =
    nav.getUserMedia || nav.webkitGetUserMedia || nav.mozGetUserMedia || nav.msGetUserMedia;
  const supportsHtmlAudio = typeof nav.getUserMedia === 'function';
  console.log('browser ' + (supportsHtmlAudio ? 'does support' : "doesn't support") + ' html audio');

  if (supportsHtmlAudio) {
    userMediaStreamService.getStream(false, (err, data) => {
      if (!err) {
        cb(null, new (HtmlRecorder as any)((window as any).audioUploadUrl, data!.stream));
      } else {
        console.error('could not initialize streamService due to: ', err);
        cb(new Error(err));
      }
    });
  } else {
    cb(new Error('html audio not supported'));
  }
};

const releaseStream = (): void => userMediaStreamService.releaseStream();

export const mediaRecorderResolver = { resolveRecorder, releaseStream };
