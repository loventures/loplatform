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

import { each } from 'lodash';

type StreamData = { stream: MediaStream | null; info: { audio: boolean; video: boolean } };
type StreamCb = (err: any, data?: StreamData) => void;

/**
 * Pure-TS port of the `userMediaStreamService` Angular factory: a single cached getUserMedia stream with a
 * video→audio-only fallback and a one-at-a-time acquisition guard. `$timeout(fn)` (defer) → `setTimeout(fn, 0)`
 * and `$timeout(fn, 100)` (re-poll while acquiring) → `setTimeout(fn, 100)` — no digest is needed now (the
 * React `useAudioRecorder` callback drives the re-render). Uses the legacy callback `navigator.getUserMedia`
 * (polyfilled by the recorder resolver), faithfully preserved.
 */
export class UserMediaStreamService {
  data: StreamData = { stream: null, info: { audio: false, video: false } };
  initialized = false;
  initializing = false;

  getStream(includeVideo: boolean, cb: StreamCb): void {
    if (!this.initializing) {
      if (this.initialized) {
        setTimeout(() => cb(null, this.data), 0);
      } else {
        this.initializing = true;
        const getUserMedia = (window.navigator as any).getUserMedia.bind(window.navigator);
        getUserMedia(
          { audio: true, video: includeVideo },
          (stream: MediaStream) => {
            this.data.info.audio = true;
            this.data.info.video = true;
            this.data.stream = stream;
            this.initialized = true;
            this.initializing = false;
            setTimeout(() => cb(null, this.data), 0);
          },
          (e: any) => {
            console.log('Could not initialize Audio and Video recorder, trying just audio...', e);
            getUserMedia(
              { audio: true },
              (stream: MediaStream) => {
                this.data.info.audio = true;
                this.data.info.video = false;
                this.data.stream = stream;
                this.initialized = true;
                this.initializing = false;
                setTimeout(() => cb(null, this.data), 0);
              },
              (err: any) => {
                this.initializing = false;
                setTimeout(() => cb(err), 0);
              }
            );
          }
        );
      }
    } else {
      // wait until the in-flight getUserMedia resolves
      setTimeout(() => this.getStream(includeVideo, cb), 100);
    }
  }

  reinitStream(includeVideo: boolean, cb: StreamCb): void {
    this.initialized = false;
    this.data = { stream: null, info: { audio: false, video: false } };
    this.getStream(includeVideo, cb);
  }

  releaseStream(): void {
    if (!this.initializing) {
      if (this.initialized && this.data.stream) {
        each(this.data.stream.getTracks(), track => track.stop());
        this.initialized = false;
      }
    } else {
      setTimeout(() => this.releaseStream(), 100);
    }
  }

  streamExists(): boolean {
    return this.data.stream !== null;
  }
}

export const userMediaStreamService = new UserMediaStreamService();
