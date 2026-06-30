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

import { get } from 'lodash';

import navBlockerService from '../services/navBlockerService.ts';
import { sessionManager } from '../services/sessionManagerSingleton.ts';
import Course from './course.ts';

/**
 * React-side re-home of two AngularJS `.run` blocks that used to wire the session subsystem:
 *   - `bootstrap/logoutRedirects.js` — `SessionManager.start(logoutAction)`
 *   - the `.run` half of `services/NavBlockerService.js` — the `beforeunload` guard +
 *     `SessionManager.registerLogoutBlocker(blockLogout)`
 *
 * The backing services are pure-TS singletons (`sessionManager`, `navBlockerService`), so the wiring
 * is just direct calls. Called once from a React bootstrap effect (ERAppRoot), after
 * `startPresenceBootstrap()`. A module-level run-once guard makes a double mount (StrictMode / remount)
 * a no-op.
 */

let started = false;

export const startSessionBootstrap = (): void => {
  if (started) return;
  started = true;

  // (1) logout action, copied verbatim from logoutRedirects.js.
  sessionManager.start(function () {
    const ltiReturnLink =
      get(window, 'lo_platform.session.logoutReturnUrl') ||
      get(window, 'lo_platform.session.returnUrl');
    if (Course.LTI && ltiReturnLink) {
      window.location.href = ltiReturnLink as string;
    } else {
      (window as any).location = '/'; // .reload(true);
    }
  });

  // (2) NavBlocker bootstrap wiring (formerly the `.run` in NavBlockerService.js).
  window.addEventListener('beforeunload', navBlockerService.blockBeforeUnload);
  sessionManager.registerLogoutBlocker(navBlockerService.blockLogout);
};
