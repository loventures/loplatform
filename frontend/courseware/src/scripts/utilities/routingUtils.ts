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

import navBlockerService from '../services/navBlockerService';
import { history } from './history';
import { focusMainContent } from './focusUtils';

// After a route change, return focus to the main content (which should have changed as a result of
// the transition) — except on the app's first navigation. This formerly went through a ui-router
// `$stateChangeSuccess` broadcast that only `tabOrderWithStateChange` listened to; that loop is now
// inlined here (the `$stateChangeStart`/`$stateChangeCancel` branch was dead — nothing listened to it).
//
// Navigation now goes straight through the history singleton (react-router v6 drove the removal of
// connected-react-router's dispatchable `push`). The action-creator shape is kept for callers that
// still dispatch it as a thunk.
let first = true;
export const gotoLinkActionCreator = (link: any) => (): void => {
  // history v5 takes navigation state as a separate argument (our link builders bundle it in `link`).
  const proceed = () => {
    history.push(link, link?.state);
    if (first) {
      first = false;
    } else {
      focusMainContent();
    }
  };
  // In-app unsaved-work guard (see LoLink / navBlockerService): if a page has registered a hot
  // nav-blocker, confirm before this programmatic navigation and only proceed on confirm.
  if (navBlockerService.getActiveBlockerMessages().length) {
    navBlockerService.confirmNavByModal().then(proceed, () => {});
  } else {
    proceed();
  }
};

export const gotoLink = (link: any): void => {
  gotoLinkActionCreator(link)();
};
