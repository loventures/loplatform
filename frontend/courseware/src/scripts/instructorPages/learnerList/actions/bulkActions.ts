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

import { UserInfo } from '../../../../loPlatform';
import { SendMessagePageLink } from '../../../utils/pageLinks';
import { createDataItemInvalidateAction } from '../../../utilities/apiDataActions';
import { b64Encode } from '../../../utilities/b64Utils';
import { gotoLinkActionCreator } from '../../../utilities/routingUtils';
import { Dispatch } from 'redux';
import enrolledUserService from '../../../services/enrolledUserService.ts';

export const learnerTableGotoMessagingActionCreator = (
  entireClass: boolean,
  selectedLearners: UserInfo[]
) =>
  gotoLinkActionCreator(
    SendMessagePageLink.toLink({
      entireClass,
      recipients: entireClass ? void 0 : b64Encode(selectedLearners),
    })
  );

export const learnerTableDropLearnerActionCreator =
  (selectedLearners: UserInfo[], reloadTable: () => void) => (dispatch: Dispatch<any>) => {
    const userIds = selectedLearners.map(u => u.id);
    // The native DELETE (with X-CSRF, added to axiosConfig) commits the drop server-side before resolving, so the
    // `reloadTable()` refetch sees the updated list — the old NB about a digest-reload race was actually the
    // missing-CSRF 403 (the drop never happened, so the reload showed stale rows). The React table reload is a
    // useEffect refetch, not digest-driven, so it works fine off the native promise.
    enrolledUserService.dropUsers(userIds, undefined).then(() => {
      reloadTable();
      userIds.forEach(id => dispatch(createDataItemInvalidateAction('users', id)));
    });
  };
