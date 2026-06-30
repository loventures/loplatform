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

import { reduce, map, pickBy, isArray } from 'lodash';

import { openConfirmModal } from '../directives/modalHost/ConfirmModal.tsx';
import { instant } from '../i18n/pure/i18n.ts';

export interface NavBlockerService {
  blockers: Record<string, () => boolean>;
  ignoreIntercept: boolean;
  getActiveBlockerMessages: (concernedMessageKeys?: string[]) => string[];
  confirmNavByModal: (concernedMessageKeys?: string[]) => Promise<void>;
  blockBeforeUnload: (event: any) => string | undefined;
  blockLogout: () => Promise<void>;
  register: (navBlockConditionFn: () => boolean, i18nMsgKey: string) => () => void;
}

/**
 * Pure-TS port of the AngularJS `NavBlockerService`. Angular deps dropped: `history` is already a pure
 * import (react-router history `block`), `reactModal.confirm` → the pure `openConfirmModal`, `$q` → native
 * `Promise`. `$translate` → the pure `instant` (synchronous) from `i18n/pure/i18n.ts`, on the
 * confirm/before-unload paths.
 *
 * The Angular adapter (`NavBlockerService.js`) returns THIS singleton and keeps the bootstrap `.run`
 * wiring (the `beforeunload` listener + `SessionManager.registerLogoutBlocker`); `BaseGrader` (Angular DI)
 * and `history.js` keep resolving the NavBlocker via lojector (which avoids the NavBlocker↔history import cycle).
 *
 * Methods reference the singleton by name (not `this`): `blockLogout`/`blockBeforeUnload` are handed off
 * as bare function references (to SessionManager / the window listener).
 */
const navBlockerService = {
  blockers: {} as Record<string, () => boolean>,
  ignoreIntercept: false,
} as NavBlockerService;

navBlockerService.getActiveBlockerMessages = concernedMessageKeys => {
  const concerned =
    concernedMessageKeys && isArray(concernedMessageKeys)
      ? pickBy(navBlockerService.blockers, (_fn, key) => concernedMessageKeys.indexOf(key) !== -1)
      : navBlockerService.blockers;

  return reduce(
    concerned,
    (allMsg: string[], conditionFn: () => boolean, i18nMsgKey: string) =>
      conditionFn() ? allMsg.concat(i18nMsgKey) : allMsg,
    [] as string[]
  );
};

navBlockerService.confirmNavByModal = concernedMessageKeys => {
  const msgs = navBlockerService.getActiveBlockerMessages(concernedMessageKeys);
  if (msgs.length === 0) return Promise.resolve();

  const translated = map(msgs, key => instant(key));
  return openConfirmModal({ message: translated.join('; ') } as any);
};

navBlockerService.blockBeforeUnload = event => {
  const msgs = navBlockerService.getActiveBlockerMessages();
  if (msgs.length === 0) return undefined;

  // Note: the default message can't actually be altered anymore by browsers.
  const msg = instant(msgs.join('; '));
  event.returnValue = msg;
  return msg;
};

navBlockerService.blockLogout = () => navBlockerService.confirmNavByModal();

navBlockerService.register = (navBlockConditionFn, i18nMsgKey) => {
  navBlockerService.blockers[i18nMsgKey] = navBlockConditionFn;

  // The unsaved-changes guard just records a condition here; it feeds the `beforeunload` listener,
  // the explicit `confirmNavByModal` callers (quiz players, grader student-switch), and the in-app
  // navigation guard in `LoLink` / `gotoLink`, which prompt before a user-initiated navigation when
  // a blocker is hot. The v4 implementation used history.block, which is fragile under react-router
  // v6's HistoryRouter (it corrupted grader/print/preview POP navigation) — guarding at the point of
  // navigation instead avoids that without a data-router/`useBlocker` migration (see issue #1657).
  return () => {
    delete navBlockerService.blockers[i18nMsgKey];
  };
};

export { navBlockerService };
export default navBlockerService;
