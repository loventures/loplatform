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

import {
  EVENT_TYPE_CONTROL,
  EVENT_DATA_TYPE_START,
  EVENT_DATA_TYPE_HEARTBEAT,
  EVENT_DATA_TYPE_ENDED,
  EVENT_TYPE_LOGOUT,
  EVENT_TYPE_PRESENCE_OPENED,
  EVENT_TYPE_PRESENCE_CLOSED,
} from '../utilities/presenceEvents.js';

import SseEventStream from './streams/SseEventStream.js';
import PollingEventStream from './streams/PollingEventStream.js';
import PresenceHeartbeat from './PresenceHeartbeat.js';
import PresenceSession from './PresenceSession.js';
import PresenceAPI from '../services/PresenceAPI.js';
import {
  setInitializedAction,
  setErrorAction,
  receiveMessageAction,
} from '../events/eventsReducer.js';
import { disablePresence } from '../utilities/preferences.js';

export default angular
  .module('lo.presence.PresenceService', [
    PresenceAPI.name,
    SseEventStream.name,
    PollingEventStream.name,
    PresenceHeartbeat.name,
    PresenceSession.name,
  ])
  .service('PresenceService', [
    'SseEventStream',
    'PollingEventStream',
    'PresenceHeartbeat',
    'PresenceSession',
    'PresenceAPI',
    'Settings',
    '$ngRedux',
    '$q',
    '$http',
    function PresenceService(
      SseEventStream,
      PollingEventStream,
      PresenceHeartbeat,
      PresenceSession,
      PresenceAPI,
      Settings,
      $ngRedux,
      $q,
      $http
    ) {
      const EventStream = (() => {
        const { useSse } = Settings.getSettings('Presence') || { useSse: true };
        return useSse ? SseEventStream : PollingEventStream;
      })();

      const service = {
        openSessionRequest: null,
        listeners: {},
        state: {
          started: false, //whether the presence machinery is initialied
          online: false, //whether presence is successfully connected
          offline: false, //whether presene encountered errors that stopped connection
        },
      };

      const serverLog = (message, payload) => {
        void message;
        void payload;
        void $http;
        // try {
        //   $http
        //     .post('/api/v2/log/info', {
        //       message,
        //       payload: angular.fromJson(angular.toJson(payload)),
        //     })
        //     .then(() => {})
        //     .catch(e => console.log(e));
        // } catch (e) {
        //   console.log(e);
        // }
      };

      service.onIdleStart = () => {
        PresenceHeartbeat.beatSlow();
      };

      service.onIdleEnd = () => {
        PresenceHeartbeat.beatFast();
      };

      service.init = ({ presenceId, course }) => {
        PresenceSession.setScenes([{ context: course }]);
        service.openPresence(presenceId);
      };

      service.reconnectPresence = () => {
        service.openPresence(service.presenceId);
      };

      service.openPresence = presenceId => {
        if (disablePresence) {
          return $q.reject('Presence disabled');
        }

        if (service.openSessionRequest) {
          return service.openSessionRequest;
        }

        const sessionInfo = PresenceSession.getSummary();

        service.state.started = true;

        service.openSessionRequest = presenceId
          ? PresenceAPI.reconnectSession(presenceId, sessionInfo)
          : PresenceAPI.openSession(sessionInfo);

        service.openSessionRequest.then(
          response => {
            serverLog('Presence id', response.data);
            try {
              $ngRedux.dispatch(setInitializedAction());
              service.state.online = true;
              service.presenceId = response.data.presenceId;
              service.startServices(service.presenceId);
              service.handleEvent({
                type: EVENT_TYPE_PRESENCE_OPENED,
              });
            } catch (e) {
              serverLog('Presence init error', e);
            }
          },
          err => {
            $ngRedux.dispatch(setErrorAction({ err }));
            service.state.offline = true;
            service.openSessionRequest = null;
            serverLog('Presence open error', err);
          }
        );

        return service.openSessionRequest;
      };

      service.closePresence = () => {
        service.state.started = false;
        service.state.online = false;
        service.handleEvent({
          type: EVENT_TYPE_PRESENCE_CLOSED,
        });
        service.stopServices();
      };

      service.deleteSessionSynchronously = () => {
        if (service.presenceId) {
          PresenceAPI.deleteSessionSynchronously(service.presenceId);
        }
      };

      service.startServices = presenceId => {
        PresenceHeartbeat.startHeartbeat(
          presenceId,
          () => {},
          () => service.closePresence()
        );

        EventStream.start({
          presenceId: presenceId,
          onEvent: event => {
            PresenceSession.updateLastEventId(event.id);
            service.handleEvent(event);
          },
          onError: error => service.handleEventError(error),
        });

        service.on(EVENT_TYPE_CONTROL, service.controlChannelListener);
        service.on(EVENT_TYPE_LOGOUT, () => service.closePresence());

        window.addEventListener('beforeunload', service.deleteSessionSynchronously);
      };

      service.stopServices = error => {
        EventStream.stop(error);

        service.listeners = {};

        PresenceHeartbeat.stopHeartbeat();

        window.removeEventListener('beforeunload', service.deleteSessionSynchronously);
      };

      service.controlChannelListener = (data, event) => {
        switch (data.type) {
          case EVENT_DATA_TYPE_START:
          case EVENT_DATA_TYPE_HEARTBEAT:
            return;
          case EVENT_DATA_TYPE_ENDED:
            service.closePresence();
            return;
          default:
            console.log('Unknown control channel event', data, event);
            return;
        }
      };

      service.handleEvent = (event = {}) => {
        const { type, data = '{}' } = event;
        const parsedData = window.JSON.parse(data);
        $ngRedux.dispatch(receiveMessageAction(parsedData));
        console.log('Event: ', type, data);
        if (service.listeners[type] && service.listeners[type].length) {
          // ESlint is bonkers
          // eslint-disable-next-line no-unused-vars
          for (let listener of service.listeners[type]) {
            listener(parsedData, event);
          }
        }
      };

      service.handleEventError = err => {
        $ngRedux.dispatch(setErrorAction({ err }));
        console.error('Event Error: ', err);
        console.log('Shutting down presence due to event source error');
        service.closePresence();
      };

      /**
       * Add a listener for server events.
       * @param{string} type - the event type
       * @param{presenceEventCB} callback - the listener.
       * @param{scope} [scope] - if specified, the listener will deregister when the scope is destroyed
       * @return a deregistration function
       */
      service.on = (type, callback) => {
        if (!service.listeners[type]) {
          service.listeners[type] = [];
          EventStream.addEventType(type);
        }

        service.listeners[type].push(callback);

        const deregister = () => {
          const callbacks = service.listeners[type];
          if (callbacks) {
            const index = callbacks.indexOf(callback);
            if (index >= 0) {
              callbacks.splice(index, 1);
            }
          }
        };

        return deregister;
      };

      service.onForScope = (type, callback, scope) => {
        const deregister = service.on(type, callback);

        scope.$on('$destroy', deregister);
      };

      service.onForCtrl = (type, callback, ctrl) => {
        const deregister = service.on(type, callback);

        const oldDestroy = ctrl.$onDestroy || (() => {});
        ctrl.$onDestroy = () => {
          deregister();
          oldDestroy.apply(ctrl);
        };
      };

      service.setScenes = scene => {
        if (PresenceSession.setScenes(scene) && PresenceHeartbeat.presenceId) {
          PresenceHeartbeat.restartHeartbeatSchedule();
        }
      };

      service.followScene = scene => {
        if (PresenceSession.followScene(scene) && PresenceHeartbeat.presenceId) {
          PresenceHeartbeat.restartHeartbeatSchedule();
        }
      };

      service.setVisibleToOthers = visibility => {
        PresenceSession.setVisibleToOthers(visibility);
        if (PresenceHeartbeat.presenceId) {
          PresenceHeartbeat.restartHeartbeatSchedule();
        }
      };

      return service;
    },
  ]);
