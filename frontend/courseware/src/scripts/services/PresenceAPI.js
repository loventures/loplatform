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

import cookies from 'browser-cookies';
import { loConfig } from '../bootstrap/loConfig.js';
import UrlBuilder from '../utilities/UrlBuilder.js';
import { NO_CACHE, NO_SESSION_EXTENSION } from '../utilities/xhrHeaders.js';

export default angular.module('lo.services.PresenceAPI', []).service('PresenceAPI', [
  '$http',
  '$timeout',
  function PresenceAPI($http, $timeout) {
    const service = {};

    service.checkEventsSinceLastEvent = (presenceId, lastEventId = void 0) => {
      const url = new UrlBuilder(loConfig.presence.sessionPoll, {
        presenceId,
      });

      const headers = {
        ...NO_SESSION_EXTENSION.headers,
        'Last-Event-ID': lastEventId,
      };

      return $http({
        method: 'GET',
        url: url.toString(),
        headers: headers,
      }).then(response => response.data);
    };

    service.openSession = sessionInfo => {
      const url = new UrlBuilder(loConfig.presence.sessions);

      const loop = attempt =>
        $http
          .post(url.toString(), { ...sessionInfo, attempt }, { ...NO_CACHE })
          .then(res => {
            if (res.status === 204) throw Error('Create presence timed out');
            return res;
          })
          .catch(err => {
            if (attempt >= 5) throw err; // presence matters for student active timing so try really hard
            return $timeout(5000 * (attempt + 1)).then(() => loop(attempt + 1));
          });
      return loop(0);
    };

    service.reconnectSession = (presenceId, sessionInfo) => {
      let url = new UrlBuilder(loConfig.presence.session, {
        presenceId,
      });

      return $http.post(url.toString(), sessionInfo);
    };

    service.closeSession = presenceId => {
      const url = new UrlBuilder(loConfig.presence.session, {
        presenceId,
      });

      return $http.delete(url.toString());
    };

    service.createEventsSession = presenceId => {
      const url = new UrlBuilder(loConfig.presence.sessionEvents, {
        presenceId,
        'X-UserId':
          window.lo_platform.user.user_type === 'Preview' ? window.lo_platform.user.id : undefined,
      });
      return new EventSource(url.toString());
    };

    service.deleteSessionSynchronously = presenceId => {
      // Try to send a beacon to delete the sesssion first because onbeforeunload
      // synchronous XHR is not so worky these days..
      try {
        let deleteUrl = new UrlBuilder(loConfig.presence.sessionDelete, {
          presenceId: presenceId,
        });
        if (!navigator.sendBeacon || !navigator.sendBeacon(deleteUrl.toString())) {
          let url = new UrlBuilder(loConfig.presence.session, {
            presenceId: presenceId,
          });
          const xhr = new XMLHttpRequest();
          xhr.open('DELETE', url.toString(), false); // DO NOT REMOVE false
          xhr.setRequestHeader('X-CSRF', cookies.get('CSRF') || 'true');
          xhr.onload = () => {
            if (xhr.status >= 400) {
              console.error('delete session error', xhr.status, xhr.responseText);
            }
          };
          xhr.send();
        }
      } catch (error) {
        console.error('delete session error', error);
      }
    };

    service.heartbeat = (presenceId, presenceInfo) => {
      const url = new UrlBuilder(loConfig.presence.session, {
        presenceId: presenceId,
      }).toString();

      return $http.put(url, presenceInfo, NO_SESSION_EXTENSION);
    };

    return service;
  },
]);
