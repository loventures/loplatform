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

import { presenceAPI } from '../../services/presenceAPI.ts';

/**
 * Pure-TS singleton port of the AngularJS `SseEventStream`.
 *
 * `PresenceAPI` is now the native `presenceAPI` singleton (direct import).
 * `$timeout(fn)` (defer to next tick) becomes `setTimeout(fn, 0)`.
 */

export interface EventStreamStartOptions {
  presenceId?: any;
  onEvent: (event: any) => void;
  onError: (error: any) => void;
}

class SseEventStreamImpl {
  // The connected event source
  eventSource: any = null;

  // complete list of event types ever added
  addedEventTypes: Record<string, boolean> = {};

  // event types currently being listened to
  listeningEventTypes: Record<string, boolean> = {};

  presenceId: any = null;
  onEvent: (event: any) => void = () => {};
  onError: (error: any) => void = () => {};

  /**
   * Connect an polling event source to the server.
   */
  start = ({ presenceId, onEvent, onError }: EventStreamStartOptions = {} as EventStreamStartOptions): void => {
    this.onEvent = onEvent;
    this.onError = onError;

    // an event source with the same id is being registered
    if (this.presenceId === presenceId && this.eventSource) {
      return;
    }

    this.stop();

    this.presenceId = presenceId;

    this.eventSource = presenceAPI.createEventsSession(presenceId);

    this.eventSource.onopen = () => {
      // internal
      console.log('Event source open');
    };

    this.eventSource.onerror = (error: any) => {
      console.log('Event source error', error);
      this.onError(error);
    };

    each(this.addedEventTypes, (added, type) => {
      void added;
      // re-add the ones that are possibly added before init/restart
      this.addEventType(type);
    });
  };

  addEventType = (eventType: string): void => {
    this.addedEventTypes[eventType] = true;
    if (this.eventSource && !this.listeningEventTypes[eventType]) {
      this.eventSource.addEventListener(eventType, (event: any) => {
        // We don't need to filter events here, as it is handled downstream
        setTimeout(() => this.onEvent(event), 0);
      });
      this.listeningEventTypes[eventType] = true;
    }
  };

  /**
   * Close the event source and presence session.
   */
  stop = (_error?: any): void => {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
    if (this.presenceId) {
      this.presenceId = null;
    }
  };
}

export type SseEventStream = SseEventStreamImpl;

export const sseEventStream = new SseEventStreamImpl();
