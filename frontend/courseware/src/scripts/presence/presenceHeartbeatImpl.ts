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

import { idleService } from './idleServiceImpl.ts';
import { presenceSession } from './presenceSessionImpl.ts';
import { presenceAPI } from '../services/presenceAPI.ts';
import { request } from '../utilities/request.ts';

/**
 * Pure-TS singleton port of the AngularJS `PresenceHeartbeat`. Informs the server
 * of your activity on a fast (45s) / slow (600s) cadence.
 *
 * `PresenceAPI` is now the native `presenceAPI` singleton and the "no presence" log
 * POST goes through the native axios `request` (same interceptors + 403 guard as the
 * Angular `Request`, sans digest — the heartbeat only schedules the next beat; it never
 * drove `$scope`). `$timeout`/`$timeout.cancel` become `setTimeout`/`clearTimeout`.
 */

class PresenceHeartbeatImpl {
  heartbeatTimeoutPromise: ReturnType<typeof setTimeout> | null = null;
  inflightRequest: Promise<any> = Promise.resolve();
  fastBeatInterval = 45 * 1000;
  slowBeatInterval = 600 * 1000;
  activeMillisAlreadyPumped = 0;
  presenceId: any = null;
  onBeat: (res?: any) => void = () => {};
  onBeatError: (res?: any) => void = () => {};

  currentBeatInterval = this.fastBeatInterval;

  startHeartbeat = (presenceId: any, onBeat: (res?: any) => void, onBeatError: (res?: any) => void): void => {
    this.presenceId = presenceId;
    this.onBeat = onBeat;
    this.onBeatError = onBeatError;
    this.beatThenScheduleNext();
  };

  stopHeartbeat = (): void => {
    if (this.heartbeatTimeoutPromise) {
      clearTimeout(this.heartbeatTimeoutPromise);
    }
  };

  restartHeartbeatSchedule = (): void => {
    this.inflightRequest.then(() => {
      this.beatThenScheduleNext();
    });
  };

  beatFast = (): void => {
    this.currentBeatInterval = this.fastBeatInterval;
    this.restartHeartbeatSchedule();
  };

  beatSlow = (): void => {
    this.currentBeatInterval = this.slowBeatInterval;
    this.restartHeartbeatSchedule();
  };

  beatThenScheduleNext = (): void => {
    if (!this.presenceId) {
      try {
        request
          .http({
            url: '/api/v2/log/info',
            method: 'POST',
            data: {
              message: 'Heartbeat without presence',
              payload: {
                activeMillis: idleService.getTotalActiveTime(),
              },
            },
          })
          .then(() => console.log('ok'))
          .catch((e: any) => console.log(e));
      } catch (e) {
        console.log(e);
      }
      this.inflightRequest = Promise.resolve();
    } else {
      (this as any).inflightRequeust = this.executeHeartbeat().then(() => {
        this.stopHeartbeat();
        this.heartbeatTimeoutPromise = setTimeout(
          () => this.beatThenScheduleNext(),
          this.currentBeatInterval
        );
      });
    }
  };

  executeHeartbeat = (): Promise<any> => {
    const summary = presenceSession.getSummary();

    // if server is down then we don't record active time, intentional and simple.
    const activeMillis = idleService.getTotalActiveTime() - this.activeMillisAlreadyPumped;

    this.inflightRequest = presenceAPI
      .heartbeat(this.presenceId, {
        ...summary,
        activeMillis,
      })
      .then((res: any) => {
        presenceSession.scenesUpdated(summary.inScenes);
        this.activeMillisAlreadyPumped += activeMillis;
        this.onBeat(res);
      })
      .catch((response: any) => {
        if (response.status === 404 || response.status === 403) {
          console.log(`Shutting down presence due to heartbeat status: ${response.status}`);
          this.onBeatError(response);
          throw 'Heartbeat shutdown';
        } else {
          console.log('Heartbeat error', response.status);
        }
      });

    return this.inflightRequest;
  };
}

export type PresenceHeartbeat = PresenceHeartbeatImpl;

export const presenceHeartbeat = new PresenceHeartbeatImpl();
