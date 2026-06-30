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

import dayjs from 'dayjs';
import { get, isFunction, isNumber, map, min } from 'lodash';

import { sessionEvents } from './sessionEvents.ts';

export interface SessionManagerDeps {
  /** SessionService: fetchStatus / renew / logout / exit. */
  sessionService: any;
  /** IdleModal: open(expiry) → Promise<boolean>; loggedOutError(logoutAction). */
  idleModal: any;
}

/**
 * The AngularJS `SessionManager` keepalive, lifted out of the factory into a pure dependency-injected
 * function so it is unit-testable. It reschedules with native `setTimeout` — IdleModal is React now, so the
 * loop needs no Angular digest — and every delay is `Math.max(0, …)`-guarded: native setTimeout has no
 * digest to throttle a negative/undefined delay into a tight loop (cf. the presence poll CI hang). The
 * Angular factory in `SessionService.js` wires the real `SessionService` / `IdleModal`.
 */
export const makeSessionManager = ({ sessionService, idleModal }: SessionManagerDeps) => {
  const SessionManager: any = {
    defaultLogoutAction: function () {
      window.location.href = '/';
    },
    logoutBlockers: {},
    warnPeriod: 10 * 60 * 1000,
    initAt: dayjs().add(2, 'seconds'), //THIS IS BAD AND SHOULD NOT BE IN DEV.  CBLPROD-1925
  };

  /** do a session status check, take action if needed, and schedule another if session can continue */
  SessionManager.start = function (logoutAction: any) {
    SessionManager.logoutAction = logoutAction;
    // The keepalive poll below is the session-expiry mechanism; the old per-403 `doSessionListenerCheck`
    // re-check (driven by the now-retired $http `SessionListener`) is gone — every call is on axios `Request`.
    return SessionManager.test(false); //Loop on the session tests with a long timeout period
  };

  SessionManager.loggedOutError = function () {
    if (dayjs().isAfter(SessionManager.initAt)) {
      sessionEvents.emit('expired', undefined);
      idleModal.loggedOutError(function () {
        const returnUrl =
          get(window, 'lo_platform.session.logoutReturnUrl') ||
          get(window, 'lo_platform.session.returnUrl');
        if (returnUrl) {
          window.location.href = returnUrl as string;
        } else {
          (window.location as any).reload(true);
        }
      });
    } else {
      console.error('Logged out, but the session is not currently open long enough to throw a warn.');
    }
  };

  SessionManager.test = function (singleCheck: any) {
    return sessionService.fetchStatus().then(function (response: any) {
      const expiry = response.data.expiry || response.data.expires;

      if (!isNumber(expiry)) {
        SessionManager.loggedOutError(); //Probably correct?  Maybe
        return Promise.resolve({});
      }
      if (expiry < SessionManager.warnPeriod && dayjs().isAfter(SessionManager.initAt)) {
        return idleModal.open(expiry).then(function (result: any) {
          if (result) {
            return sessionService.renew().then(function () {
              // expiry < warnPeriod here, so this delay clamps to 0 → re-check ASAP after renew; the next
              // fetchStatus sees the renewed (larger) expiry and reschedules far out (no tight loop).
              setTimeout(SessionManager.start, Math.max(0, expiry - SessionManager.warnPeriod));
            });
          } else {
            sessionEvents.emit('expired', undefined);
            return sessionService.logout().then(SessionManager.logout);
          }
        });
      } else if (!singleCheck) {
        // some browsers blow up on delays that don't fit in 32 bits
        const safeDelay = Math.max(0, min([expiry - SessionManager.warnPeriod, 2147483647]) as number);
        setTimeout(function () {
          SessionManager.start(SessionManager.logoutAction, false);
        }, safeDelay);
      }
    }, SessionManager.loggedOutError);
  };

  SessionManager.logout = function (customRedirect: any) {
    const logoutAction = SessionManager.logoutAction || SessionManager.defaultLogoutAction;

    const blockerResults = map(SessionManager.logoutBlockers, function (blocker: any) {
      const result = isFunction(blocker) && blocker();

      if (isFunction(result.then)) {
        //is a promise
        return result;
      } else {
        return result ? Promise.resolve() : Promise.reject();
      }
    });

    return Promise.all(blockerResults)
      .then(sessionService.logout)
      .then(function (redirect: any) {
        if (customRedirect) {
          window.location.href = customRedirect;
        } else if (redirect) {
          window.location.href = redirect;
        } else {
          logoutAction();
        }
      });
  };

  SessionManager.exit = sessionService.exit;

  let idCounter = 0;
  SessionManager.registerLogoutBlocker = function (blocker: any) {
    const id = idCounter++;
    SessionManager.logoutBlockers[id] = blocker;
    return function () {
      delete SessionManager.logoutBlockers[id];
    };
  };

  return SessionManager;
};
