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

import contentsResource from '../resources/ContentsResource';
import PresenceService from '../presence/PresenceService';
import { createDataListUpdateMergeAction } from '../utilities/apiDataActions';

// sync with ContentGateNotification.scala
const ContentGateNotificationSchema = 'contentGateNotification';

export default angular.module('course.bootstrap.gatingListener', [PresenceService.name]).run([
  'PresenceService',
  '$ngRedux',
  function gatingListener(PresenceService, $ngRedux) {
    PresenceService.on('Notification', ({ _type, contentId, student }) => {
      if (_type === ContentGateNotificationSchema) {
        $ngRedux.dispatch(
          createDataListUpdateMergeAction('gatingInformationByContentByUser', {
            [student]: { [contentId]: { gateStatus: 'OPEN' } },
          })
        );
        contentsResource.invalidate().then();
      }
    });
  },
]);
