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

import { isFunction } from 'lodash';
import * as React from 'react';

import { mediaRecorderResolver } from '../mediaRecorderResolver.ts';

export interface AudioRecording {
  /** the WAV blob, augmented with fileName/mimeType/url/viewUrl/base64 by the recorder/feedback glue. */
  data: any;
  url: string;
  name: string;
  base64?: string;
}

interface UseAudioRecorderOptions {
  /** Fires with the recording on stop, and with `undefined` on clear (drives feedback tool status). */
  onChange?: (recording?: AudioRecording) => void;
}

/**
 * React port of the `audioRecorder` component controller: the recording lifecycle. Reuses the proven
 * browser-media services (`mediaRecorderResolver` → getUserMedia → `HtmlRecorder` → Recorder.js / WAV),
 * now pure-TS singletons imported directly — only the Angular UI glue is replaced. The `$interval` Stopwatch becomes a native
 * accumulate/resume timer; `$element.find('audio').attr('src', …)` becomes an `audioRef` (base64 is huge
 * — ~200K chars/sec — so it's set imperatively, never through React state/props).
 */
export const useAudioRecorder = ({ onChange }: UseAudioRecorderOptions) => {
  const [ready, setReady] = React.useState(false);
  const [audioSupported, setAudioSupported] = React.useState(false);
  const [isRecording, setIsRecording] = React.useState(false);
  const [isPaused, setIsPaused] = React.useState(false);
  const [recording, setRecording] = React.useState<AudioRecording | null>(null);
  const [elapsedMs, setElapsedMs] = React.useState(0);

  const recorderRef = React.useRef<any>(null);
  const audioRef = React.useRef<HTMLAudioElement>(null);
  const timer = React.useRef({ accumulated: 0, startedAt: 0, interval: null as any });
  const onChangeRef = React.useRef(onChange);
  onChangeRef.current = onChange;

  const stopTick = () => {
    if (timer.current.interval) clearInterval(timer.current.interval);
    timer.current.interval = null;
  };
  const startTick = () => {
    timer.current.startedAt = Date.now();
    timer.current.interval = setInterval(() => {
      setElapsedMs(timer.current.accumulated + (Date.now() - timer.current.startedAt));
    }, 1000);
  };
  const pauseTick = () => {
    stopTick();
    timer.current.accumulated += Date.now() - timer.current.startedAt;
    setElapsedMs(timer.current.accumulated);
  };
  const resetTick = () => {
    stopTick();
    timer.current = { accumulated: 0, startedAt: 0, interval: null };
    setElapsedMs(0);
  };

  React.useEffect(() => {
    const resolver = mediaRecorderResolver;
    try {
      resolver.resolveRecorder((err: any, recorder: any) => {
        if (err) {
          console.log("Could not resolve recorder; browser likely doesn't support audio recording: ", err);
          setAudioSupported(false);
          setReady(true);
        } else {
          recorderRef.current = recorder;
          recorder.initialize(() => setReady(true));
          setAudioSupported(true);
        }
      });
    } catch (e) {
      console.error('Error during audio init', e);
      setAudioSupported(false);
      setReady(true);
    }
    return () => {
      stopTick();
      resolver.releaseStream();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const startRecording = () => {
    if (isPaused) {
      recorderRef.current.resume();
      setIsPaused(false);
      setIsRecording(true);
      startTick();
    } else {
      resetTick();
      startTick();
      setIsPaused(false);
      setIsRecording(true);
      recorderRef.current.record();
    }
  };

  const stopRecording = () => {
    pauseTick();
    recorderRef.current.stop((rec: AudioRecording | undefined) => {
      if (rec) {
        rec.data.fileName = rec.name;
        rec.data.mimeType = rec.data.type;
        setRecording(rec);
        const reader = new window.FileReader();
        reader.readAsDataURL(rec.data);
        reader.onload = () => {
          rec.base64 = reader.result as string;
          if (audioRef.current) audioRef.current.src = rec.base64; // playBase64 (imperative — huge string)
          onChangeRef.current?.(rec);
        };
      }
    });
    setIsPaused(false);
    setIsRecording(false);
  };

  const pauseRecording = () => {
    pauseTick();
    setIsPaused(true);
    setIsRecording(false);
    recorderRef.current.pause();
  };

  const clear = () => {
    recorderRef.current.clear();
    resetTick();
    setRecording(null);
    onChangeRef.current?.(undefined);
  };

  return {
    ready,
    audioSupported,
    isRecording,
    isPaused,
    recording,
    elapsedMs,
    audioRef,
    startRecording,
    stopRecording,
    pauseRecording,
    clear,
    canRecord: !isRecording,
    canPause: isRecording,
    canStop: isRecording || isPaused,
    supportsPause: !!recorderRef.current && isFunction(recorderRef.current.pause),
  };
};
