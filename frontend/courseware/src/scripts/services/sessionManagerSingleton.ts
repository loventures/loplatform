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

import { makeSessionManager } from './sessionManagerCore.ts';
import { sessionApi } from './pure/sessionApi.ts';
import { idleModal } from './pure/idleModal.ts';

/**
 * The single source of truth for the session subsystem singletons. Module-level consts (ES modules are
 * cached) guarantee ONE instance each — the Angular `.factory` adapters in `SessionService.js` and the
 * React `sessionAccessor.ts` both import these same consts, so there is exactly one `sessionManager`
 * keepalive state machine (a second instance would split session-expiry tracking and break logout/renew).
 *
 * The keepalive logic itself is the pure, unit-tested `makeSessionManager` in `./sessionManagerCore.ts`;
 * this module only injects the pure `sessionApi` / `idleModal` deps the Angular factory used to wire.
 */
export { sessionApi, idleModal };

export const sessionManager = makeSessionManager({
  sessionService: sessionApi,
  idleModal: idleModal,
});

export default sessionManager;
