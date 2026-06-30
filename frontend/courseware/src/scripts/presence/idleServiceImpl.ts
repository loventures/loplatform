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

import { presenceService } from './presenceServiceImpl.ts';
import { presenceSession } from './presenceSessionImpl.ts';

/**
 * Pure-TS singleton port of the AngularJS `IdleService`.
 *
 * Suspicious wrapper around react-idle-timer because react-idle-timer is too accurate.
 * react-idle-timer's getTotalActiveMillis is really just the "active time": the duration
 * you were "moving your mouse around" or "scrolling". But we give users a free 60 seconds
 * before we consider them LO-idle. So we maintain our own stopwatch.
 *
 * The original emitted `$rootScope.$emit('IdleService.LOIdleStart'|'LOIdleEnd')`, which the
 * presence bootstrap forwarded to `PresenceService.onIdleStart()`/`onIdleEnd()`. This port
 * decouples from `$rootScope` by calling the presenceService singleton directly.
 *
 * `$timeout`/`$interval` become `setTimeout`/`setInterval`.
 */

const IDLE_TIMEOUT = 60 * 1000;
const UPDATE_LAST_ACTIVE_INTERVAL = 1000;

class IdleServiceImpl {
  // for when document is opened in a background tab and never brought to foreground
  private readonly hiddenOnInit = document.visibilityState === 'hidden';

  stopwatch = {
    running: !this.hiddenOnInit, // is the stopwatch running or paused
    lastPausedValue: 0, // the elapsed() when the stopwatch was last paused
    lastResumeTime: new Date(),

    elapsed: (): number => {
      const now = new Date();
      const valueSinceResumed = this.stopwatch.running
        ? now.valueOf() - this.stopwatch.lastResumeTime.valueOf()
        : 0;
      return valueSinceResumed + this.stopwatch.lastPausedValue;
    },

    pause: (): void => {
      this.stopwatch.lastPausedValue = this.stopwatch.elapsed();
      this.stopwatch.running = false;
    },

    resume: (): void => {
      this.stopwatch.lastResumeTime = new Date();
      this.stopwatch.running = true;
    },
  };

  idleCountdown: ReturnType<typeof setTimeout> | null = null;
  lastActiveInterval: ReturnType<typeof setInterval> | null = null;

  /**
   * Gets the total active time in milliseconds since this service was instantiated.
   * Not the same as react-idle-timer's getTotalActiveTime. This one includes the
   * 60 second LO-idle countdown times as "active time".
   */
  getTotalActiveTime = (): number => this.stopwatch.elapsed();

  /**
   * Emits an LOIdleStart if there has been no activity in timeout period
   */
  private startIdleCountdown = (): ReturnType<typeof setTimeout> => {
    if (this.idleCountdown) clearTimeout(this.idleCountdown);
    return setTimeout(() => {
      this.stopwatch.pause();
      presenceService.onIdleStart();
    }, IDLE_TIMEOUT);
  };

  /**
   * Updates last active every second
   */
  private startLastActiveInterval = (): void => {
    if (!this.lastActiveInterval) {
      this.lastActiveInterval = setInterval(() => {
        presenceSession.updateLastActive(); // for the progress circles
      }, UPDATE_LAST_ACTIVE_INTERVAL);
    }
  };

  onIdleStart = (): void => {
    if (this.stopwatch.running) {
      if (document.visibilityState === 'hidden') {
        // only in the case of a hidden document, i.e. the tab went into the background
        // do we forgo the 60-seconds-active grace period. However, heartbeat and presence
        // circle shade continue to obey the grace period because dragging tabs
        // around sends the document hidden for a few microseconds. Too much trashing of
        // heartbeats in that case.
        this.stopwatch.pause();
      }

      this.idleCountdown = this.startIdleCountdown();
      if (this.lastActiveInterval) {
        clearInterval(this.lastActiveInterval);
        this.lastActiveInterval = null;
      }
    }
  };

  onIdleEnd = (): void => {
    if (!this.stopwatch.running) {
      this.stopwatch.resume();
      if (this.idleCountdown !== null) {
        clearTimeout(this.idleCountdown);
        this.idleCountdown = null;
      }
      presenceSession.updateLastActive();
      presenceService.onIdleEnd();
    }
    this.startLastActiveInterval();
  };

  constructor() {
    if (this.hiddenOnInit) {
      this.idleCountdown = this.startIdleCountdown();
    }
  }
}

export type IdleService = IdleServiceImpl;

export const idleService = new IdleServiceImpl();
