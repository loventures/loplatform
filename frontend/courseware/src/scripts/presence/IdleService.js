/*
 * LO Platform copyright (C) 2007–2025 LO Ventures LLC.
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

import PresenceSessionModule from './PresenceSession.js';

/**
 * Suspicious wrapper around react-idle-timer because react-idle-timer is too accurate.
 * react-idle-timer's getTotalActiveMillis is really just the "active time": the duration
 * you were "moving your mouse around" or "scrolling". But we give users a free 60 seconds
 * before we consider them LO-idle. So we maintain our own stopwatch. Also, I am too
 * busylazy to explore complete removal of this service. And so are you. (Even if RIT's
 * timeout is 60s, getTotalActiveMillis is still just the mouse-moving time).
 */
export default angular
  .module('lo.presence.IdleService', [PresenceSessionModule.name])
  .service('IdleService', [
    '$rootScope',
    '$timeout',
    '$interval',
    'PresenceSession',
    function IdleService($rootScope, $timeout, $interval, PresenceSession) {
      const IDLE_TIMEOUT = 60 * 1000;
      const UPDATE_LAST_ACTIVE_INTERVAL = 1000;

      // for when document is opened in a background tab and never brought to foreground
      const hiddenOnInit = document.visibilityState === 'hidden';

      const stopwatch = {
        running: !hiddenOnInit, // is the stopwatch running or paused
        lastPausedValue: 0, // the elapsed() when the stopwatch was last paused
        lastResumeTime: new Date(),

        elapsed: () => {
          const now = new Date();
          const valueSinceResumed = stopwatch.running
            ? now.valueOf() - stopwatch.lastResumeTime.valueOf()
            : 0;
          return valueSinceResumed + stopwatch.lastPausedValue;
        },

        pause: () => {
          stopwatch.lastPausedValue = stopwatch.elapsed();
          stopwatch.running = false;
        },

        resume: () => {
          stopwatch.lastResumeTime = new Date();
          stopwatch.running = true;
        },
      };

      const service = {
        stopwatch,
      };

      /**
       * Gets the total active time in milliseconds since this service was instantiated.
       * Not the same as react-idle-timer's getTotalActiveTime. This one includes the
       * 60 second LO-idle countdown times as "active time".
       */
      service.getTotalActiveTime = stopwatch.elapsed;

      /**
       * Emits an LOIdleStart if there has been no activity in timeout period
       */
      const startIdleCountdown = () => {
        $timeout.cancel(service.idleCountdown);
        return $timeout(() => {
          service.stopwatch.pause();
          $rootScope.$emit('IdleService.LOIdleStart');
        }, IDLE_TIMEOUT);
      };

      /**
       * Updates last active every second
       */
      const startLastActiveInterval = () => {
        if (!service.lastActiveInterval) {
          service.lastActiveInterval = $interval(() => {
            PresenceSession.updateLastActive(); // for the progress circles
          }, UPDATE_LAST_ACTIVE_INTERVAL);
        }
      };

      service.onIdleStart = () => {
        if (service.stopwatch.running) {
          if (document.visibilityState === 'hidden') {
            // only in the case of a hidden document, i.e. the tab went into the background
            // do we forgo the 60-seconds-active grace period. However, heartbeat and presence
            // circle shade continue to obey the grace period because dragging tabs
            // around sends the document hidden for a few microseconds. Too much trashing of
            // heartbeats in that case.
            service.stopwatch.pause();
          }

          service.idleCountdown = startIdleCountdown();
          if (service.lastActiveInterval) {
            $interval.cancel(service.lastActiveInterval);
            service.lastActiveInterval = null;
          }
        }
      };

      service.onIdleEnd = () => {
        if (!service.stopwatch.running) {
          service.stopwatch.resume();
          if (service.idleCountdown !== null) {
            $timeout.cancel(service.idleCountdown);
            service.idleCountdown = null;
          }
          PresenceSession.updateLastActive();
          $rootScope.$emit('IdleService.LOIdleEnd');
        }
        startLastActiveInterval();
      };

      if (hiddenOnInit) {
        service.idleCountdown = startIdleCountdown();
      }

      return service;
    },
  ]);
