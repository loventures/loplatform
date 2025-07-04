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

import { each } from 'lodash';

import PresenceAPI from '../../services/PresenceAPI.js';

export default angular
  .module('lo.presence.SseEventStream', [PresenceAPI.name])
  .service('SseEventStream', [
    'PresenceAPI',
    '$timeout',
    function SseEventStream(PresenceAPI, $timeout) {
      const service = {
        // The connected event source
        eventSource: null,

        //complete list of event types ever added
        addedEventTypes: {},

        //event types currently being listened to
        listeningEventTypes: {},
      };

      /**
       * Connect an polling event source to the server.
       * Parameters:
       * @param{Object} config - initial configuration options for the event source
       * @param{string} presenceId - the server-side configured presence to listen to
       * @param{function} onEvent - the callback for when an event is received
       * @param{function} onError - the callback for when an error is encountered
       * @param{[string]} handledTypes - existing event types which the callback knows how to handle
       */
      service.start = ({ presenceId, onEvent, onError } = {}) => {
        service.onEvent = onEvent;
        service.onError = onError;

        // an event source with the same id is being registered
        if (service.presenceId === presenceId && service.eventSource) {
          return;
        }

        service.stop();

        service.presenceId = presenceId;

        service.eventSource = PresenceAPI.createEventsSession(presenceId);

        service.eventSource.onopen = () => {
          // internal
          console.log('Event source open');
        };

        service.eventSource.onerror = error => {
          console.log('Event source error', error);
          service.onError(error);
        };

        each(service.addedEventTypes, (added, type) => {
          //re-add the ones that are possibly added before init/restart
          service.addEventType(type);
        });
      };

      service.addEventType = eventType => {
        service.addedEventTypes[eventType] = true;
        if (service.eventSource && !service.listeningEventTypes[eventType]) {
          service.eventSource.addEventListener(eventType, event => {
            //We don't need to filter events here, as it is handled downstream
            $timeout(() => service.onEvent(event));
          });
          service.listeningEventTypes[eventType] = true;
        }
      };

      /**
       * Close the event source and presence session.
       */
      service.stop = () => {
        if (service.eventSource) {
          service.eventSource.close();
          service.eventSource = null;
        }
        if (service.presenceId) {
          service.presenceId = null;
        }
      };

      return service;
    },
  ]);
