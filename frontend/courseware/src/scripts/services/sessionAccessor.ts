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

import { sessionManager as sessionManagerSingleton, sessionApi } from './sessionManagerSingleton.ts';

/** The SessionManager/SessionService methods React callers use. */
export interface SessionManager {
  exit(): void;
  logout(returnUrl?: string): void;
}
export interface SessionService {
  isSudo(): boolean;
}

/**
 * Typed accessors for the session services. They now return the pure-TS singletons from
 * `./sessionManagerSingleton.ts` directly (the same instances the Angular `.factory` adapters in
 * `SessionService.js` re-export), so the one React consumer (`AppLogout`) is drained off `lojector`
 * with no behavioural change — the function signatures + interfaces are unchanged.
 */
export const sessionManager = (): SessionManager => sessionManagerSingleton as SessionManager;
export const sessionService = (): SessionService => sessionApi as SessionService;
