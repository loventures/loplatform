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

import { EVENT_TYPE_LOGOUT } from '../../utilities/presenceEvents.js';

import PresenceAPI from '../../services/PresenceAPI.js';

export default angular
  .module('lo.presence.PollingEventStream', [PresenceAPI.name])
  .service('PollingEventStream', [
    '$timeout',
    'Settings',
    'PresenceAPI',
    function PollingEventStream($timeout, Settings, PresenceAPI) {
      const service = {};

      /**
       * Connect an polling event source to the server.
       * Parameters:
       * @param{Object} config - initial configuration options for the event source
       * @param{string} presenceId - the server-side configured presence to listen to
       * @param{function} onEvent - the callback for when an event is received
       * @param{function} onError - the callback for when an error is encountered
       */
      service.start = ({ presenceId, onEvent, onError } = {}) => {
        /* After a session is acquired, kill previous session if exists and poll immediately. */
        if (service.presenceId) {
          service.stop();
        }

        service.pollIntervalMillis = (() => {
          let cfg = Settings.getSettings('Presence') || {
            pollIntervalMillis: 5000,
          };
          return cfg.pollIntervalMillis;
        })();

        service.presenceId = presenceId;
        service.onEvent = onEvent;
        service.onError = onError;

        service.pollThenScheduleNext();
      };

      /**
       * Stop a polling event
       * @param{Object} error - the error that caused the termination
       * @param{number} error.status - the http status code
       */
      service.stop = error => {
        if (service.currentPoll) {
          $timeout.cancel(service.currentPoll);
        }

        /* If we start getting 404s, we've been logged out elsewhere
         * and won't get the logout event. */
        if (error && error.status === 404) {
          service.onEvent({
            type: EVENT_TYPE_LOGOUT,
          });
        }

        service.presenceId = null;
        service.handledEventTypes = [];
      };

      service.pollThenScheduleNext = () => {
        service.doPoll().then(() => {
          service.currentPoll = $timeout(
            () => service.pollThenScheduleNext(),
            service.pollIntervalMillis
          );
        });
      };

      service.doPoll = () => {
        return PresenceAPI.checkEventsSinceLastEvent(service.presenceId, service.lastEventId)
          .then(events => {
            // ESlint is bonkers
            // eslint-disable-next-line no-unused-vars
            for (let event of events) {
              if (event.id) {
                service.lastEventId = parseInt(event.id, 10);
              }
              service.onEvent(event);
            }
          })
          .catch(error => service.onError(error));
      };

      //We don't really care for polling, but to keep a consistent interface
      service.addEventType = () => {};

      return service;
    },
  ]);
