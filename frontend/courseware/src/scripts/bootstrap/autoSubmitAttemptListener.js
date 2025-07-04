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

import { loadQuizActivityActionCreator } from '../courseActivityModule/actions/quizActivityActions';
import PresenceService from '../presence/PresenceService';
import { selectContentItems } from '../selectors/contentItemSelectors';
import { selectCurrentUser } from '../utilities/rootSelectors';

// sync with AutoSubmitAttemptNotification.scala
const AutoSubmitAttemptNotificationSchema = 'autoSubmitAttemptNotification';
const AttemptInvalidatedNotificationSchema = 'attemptInvalidatedNotification';

export default angular
  .module('course.bootstrap.autoSubmitAttemptListener', [PresenceService.name])
  .run([
    'PresenceService',
    '$ngRedux',
    function gatingListener(PresenceService, $ngRedux) {
      PresenceService.on('Notification', ({ _type, topic }) => {
        if (
          _type === AutoSubmitAttemptNotificationSchema ||
          _type === AttemptInvalidatedNotificationSchema
        ) {
          $ngRedux.dispatch((dispatch, getState) => {
            const state = getState();
            const viewingAs = selectCurrentUser(state);
            const content = selectContentItems(state)[topic];
            dispatch(loadQuizActivityActionCreator(content, viewingAs, viewingAs.id));
          });
        }
      });
    },
  ]);
