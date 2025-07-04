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

import PresenceAPI from '../services/PresenceAPI.js';
import PresenceSession from './PresenceSession.js';
import IdleServiceModule from './IdleService.js';

export default angular
  .module('lo.presence.PresenceHeartbeat', [
    IdleServiceModule.name,
    PresenceSession.name,
    PresenceAPI.name,
  ])
  /**
   * Heartbeat informs the server of your activity.
   */
  .service('PresenceHeartbeat', [
    'IdleService',
    'PresenceAPI',
    'PresenceSession',
    '$timeout',
    '$q',
    '$http',
    function PresenceHeartbeat(IdleService, PresenceAPI, PresenceSession, $timeout, $q, $http) {
      const service = {
        heartbeatTimeoutPromise: null,
        inflightRequest: $q.when(),
        fastBeatInterval: 45 * 1000,
        slowBeatInterval: 600 * 1000,
        activeMillisAlreadyPumped: 0,
        presenceId: null,
        onBeat: () => {},
        onBeatError: () => {},
      };

      service.currentBeatInterval = service.fastBeatInterval;

      service.startHeartbeat = (presenceId, onBeat, onBeatError) => {
        service.presenceId = presenceId;
        service.onBeat = onBeat;
        service.onBeatError = onBeatError;
        service.beatThenScheduleNext();
      };

      service.stopHeartbeat = () => {
        if (service.heartbeatTimeoutPromise) {
          $timeout.cancel(service.heartbeatTimeoutPromise);
        }
      };

      service.restartHeartbeatSchedule = () => {
        service.inflightRequest.then(() => {
          service.beatThenScheduleNext();
        });
      };

      service.beatFast = () => {
        service.currentBeatInterval = service.fastBeatInterval;
        service.restartHeartbeatSchedule();
      };

      service.beatSlow = () => {
        service.currentBeatInterval = service.slowBeatInterval;
        service.restartHeartbeatSchedule();
      };

      service.beatThenScheduleNext = () => {
        if (!service.presenceId) {
          try {
            $http
              .post('/api/v2/log/info', {
                message: 'Heartbeat without presence',
                payload: {
                  activeMillis: IdleService.getTotalActiveTime(),
                },
              })
              .then(() => console.log('ok'))
              .catch(e => console.log(e));
          } catch (e) {
            console.log(e);
          }
          service.inflightRequest = $q.when();
        } else {
          service.inflightRequeust = service.executeHeartbeat().then(() => {
            service.stopHeartbeat();
            service.heartbeatTimeoutPromise = $timeout(
              () => service.beatThenScheduleNext(),
              service.currentBeatInterval,
              false
            );
          });
        }
      };

      service.executeHeartbeat = () => {
        const summary = PresenceSession.getSummary();

        // if server is down then we don't record active time, intentional and simple.
        const activeMillis = IdleService.getTotalActiveTime() - service.activeMillisAlreadyPumped;

        service.inflightRequest = PresenceAPI.heartbeat(service.presenceId, {
          ...summary,
          activeMillis,
        })
          .then(res => {
            PresenceSession.scenesUpdated(summary.inScenes);
            service.activeMillisAlreadyPumped += activeMillis;
            service.onBeat(res);
          })
          .catch(response => {
            if (response.status === 404 || response.status === 403) {
              console.log(`Shutting down presence due to heartbeat status: ${response.status}`);
              service.onBeatError(response);
              throw 'Heartbeat shutdown';
            } else {
              console.log('Heartbeat error', response.status);
            }
          });

        return service.inflightRequest;
      };

      return service;
    },
  ]);
