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

import cookies from 'browser-cookies';
import { loConfig } from '../../bootstrap/loConfig.ts';
import UrlBuilder from '../../utilities/UrlBuilder.js';
import { NO_CACHE, NO_SESSION_EXTENSION } from '../../utilities/xhrHeaders.js';

/**
 * The $http-shaped client this service needs: callable (`http(config)`) with
 * `.post` / `.put` / `.delete` helpers — satisfied by Angular's `$http` (the
 * adapter, digest-integrated) or by axios (a future native caller).
 */
export interface HttpLike {
  (config: any): Promise<any>;
  post(url: string, data?: any, config?: any): Promise<any>;
  put(url: string, data?: any, config?: any): Promise<any>;
  delete(url: string, config?: any): Promise<any>;
}

/** Resolve after `ms` milliseconds ($timeout(ms) / a native setTimeout promise). */
export type DelayFn = (ms: number) => Promise<any>;

/**
 * Presence session API (poll/open/reconnect/close/heartbeat + SSE/beacon),
 * migrated verbatim from the AngularJS `PresenceAPI` service to plain TS taking
 * the injected $http-shaped client and a `delay` runtime. The EventSource,
 * sendBeacon and synchronous-XHR teardown paths use browser APIs directly.
 */
export const makePresenceAPI = (http: HttpLike, delay: DelayFn) => {
  const service: any = {};

  service.checkEventsSinceLastEvent = (presenceId: any, lastEventId: any = void 0) => {
    const url = new (UrlBuilder as any)(loConfig.presence.sessionPoll, {
      presenceId,
    });

    const headers = {
      ...NO_SESSION_EXTENSION.headers,
      'Last-Event-ID': lastEventId,
    };

    return http({
      method: 'GET',
      url: url.toString(),
      headers: headers,
    }).then((response: any) => response.data);
  };

  service.openSession = (sessionInfo: any) => {
    const url = new (UrlBuilder as any)(loConfig.presence.sessions);

    const loop = (attempt: any): PromiseLike<any> =>
      http
        .post(url.toString(), { ...sessionInfo, attempt }, { ...NO_CACHE })
        .then((res: any) => {
          if (res.status === 204) throw Error('Create presence timed out');
          return res;
        })
        .catch((err: any) => {
          if (attempt >= 5) throw err; // presence matters for student active timing so try really hard
          return delay(5000 * (attempt + 1)).then(() => loop(attempt + 1));
        });
    return loop(0);
  };

  service.reconnectSession = (presenceId: any, sessionInfo: any) => {
    const url = new (UrlBuilder as any)(loConfig.presence.session, {
      presenceId,
    });

    return http.post(url.toString(), sessionInfo);
  };

  service.closeSession = (presenceId: any) => {
    const url = new (UrlBuilder as any)(loConfig.presence.session, {
      presenceId,
    });

    return http.delete(url.toString());
  };

  service.createEventsSession = (presenceId: any) => {
    const url = new (UrlBuilder as any)(loConfig.presence.sessionEvents, {
      presenceId,
      'X-UserId':
        (window as any).lo_platform.user.user_type === 'Preview' ? (window as any).lo_platform.user.id : undefined,
    });
    return new EventSource(url.toString());
  };

  service.deleteSessionSynchronously = (presenceId: any) => {
    // Try to send a beacon to delete the sesssion first because onbeforeunload
    // synchronous XHR is not so worky these days..
    try {
      const deleteUrl = new (UrlBuilder as any)(loConfig.presence.sessionDelete, {
        presenceId: presenceId,
      });
      if (!navigator.sendBeacon || !navigator.sendBeacon(deleteUrl.toString())) {
        const url = new (UrlBuilder as any)(loConfig.presence.session, {
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

  service.heartbeat = (presenceId: any, presenceInfo: any) => {
    const url = new (UrlBuilder as any)(loConfig.presence.session, {
      presenceId: presenceId,
    }).toString();

    return http.put(url, presenceInfo, NO_SESSION_EXTENSION);
  };

  return service;
};

export type PresenceAPI = ReturnType<typeof makePresenceAPI>;
