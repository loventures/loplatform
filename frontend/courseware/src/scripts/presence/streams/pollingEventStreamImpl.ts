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

import { EVENT_TYPE_LOGOUT } from '../../utilities/presenceEvents.js';

import settings from '../../utilities/settingsService';
import { presenceAPI } from '../../services/presenceAPI.ts';
import type { EventStreamStartOptions } from './sseEventStreamImpl.ts';

/**
 * Pure-TS singleton port of the AngularJS `PollingEventStream`.
 *
 * `PresenceAPI` is now the native `presenceAPI` singleton (direct import).
 * `$timeout`/`$timeout.cancel` become `setTimeout`/`clearTimeout`.
 */

const Settings = {
  get: () => settings,
};

class PollingEventStreamImpl {
  presenceId: any = null;
  lastEventId: any = undefined;
  pollIntervalMillis = 5000;
  currentPoll: ReturnType<typeof setTimeout> | null = null;
  handledEventTypes: any[] = [];

  onEvent: (event: any) => void = () => {};
  onError: (error: any) => void = () => {};

  /**
   * Connect an polling event source to the server.
   */
  start = ({ presenceId, onEvent, onError }: EventStreamStartOptions = {} as EventStreamStartOptions): void => {
    /* After a session is acquired, kill previous session if exists and poll immediately. */
    if (this.presenceId) {
      this.stop();
    }

    // Default to 5s if the Presence settings exist but omit `pollIntervalMillis`. Critically, `$timeout`
    // (the original) digest-throttled an undefined delay, but native `setTimeout(fn, undefined)` fires at
    // 0ms — a tight poll loop that floods the renderer (the CI "Timed out receiving message from renderer"
    // hang). `|| 5000` guarantees a sane interval.
    this.pollIntervalMillis = Settings.get().getSettings('Presence')?.pollIntervalMillis || 5000;

    this.presenceId = presenceId;
    this.onEvent = onEvent;
    this.onError = onError;

    this.pollThenScheduleNext();
  };

  /**
   * Stop a polling event
   */
  stop = (error?: any): void => {
    if (this.currentPoll) {
      clearTimeout(this.currentPoll);
    }

    /* If we start getting 404s, we've been logged out elsewhere
     * and won't get the logout event. */
    if (error && error.status === 404) {
      this.onEvent({
        type: EVENT_TYPE_LOGOUT,
      });
    }

    this.presenceId = null;
    this.handledEventTypes = [];
  };

  pollThenScheduleNext = (): void => {
    this.doPoll().then(() => {
      // `doPoll` swallows errors (.catch → onError → closePresence → stop), so only reschedule while the
      // stream is still active (stop() nulls presenceId) — otherwise polling continues after shutdown.
      if (this.presenceId) {
        this.currentPoll = setTimeout(() => this.pollThenScheduleNext(), this.pollIntervalMillis);
      }
    });
  };

  doPoll = (): Promise<any> => {
    return presenceAPI
      .checkEventsSinceLastEvent(this.presenceId, this.lastEventId)
      .then((events: any[]) => {
        for (const event of events) {
          if (event.id) {
            this.lastEventId = parseInt(event.id, 10);
          }
          this.onEvent(event);
        }
      })
      .catch((error: any) => this.onError(error));
  };

  // We don't really care for polling, but to keep a consistent interface
  addEventType = (): void => {};
}

export type PollingEventStream = PollingEventStreamImpl;

export const pollingEventStream = new PollingEventStreamImpl();
