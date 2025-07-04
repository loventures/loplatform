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

import NotificationService from '../services/NotificationService';
import NotificationsResolver from '../services/NotificationsResolver';

export default angular
  .module('lof.bootstrap.notifications', [NotificationService.name, NotificationsResolver.name])
  .run([
    'NotificationService',
    'NotificationsResolver',
    function (NotificationService, NotificationsResolver) {
      NotificationService.addAction(
        'gradeNotification2',
        NotificationsResolver.instructorNotificationAction
      );
      NotificationService.addAction(
        'instructorNotification',
        NotificationsResolver.instructorNotificationAction
      );
      NotificationService.addAction(
        'attemptInvalidatedNotification',
        NotificationsResolver.instructorNotificationAction
      );
      NotificationService.addAction(
        'autoSubmitAttemptNotification',
        NotificationsResolver.instructorNotificationAction
      );
      NotificationService.addAction(
        'gateDateNotification',
        NotificationsResolver.gateDateNotificationAction
      );
      NotificationService.addAction(
        'contentGateNotification',
        NotificationsResolver.gateDateNotificationAction
      );
      NotificationService.addAction('competencyMasteryNotification', () => {});
      NotificationService.addAction('updateNotification', () => {});
      NotificationService.addAction(
        'postNotification',
        NotificationsResolver.postNotificationAction
      );
      NotificationService.addAction(
        'inappropriatePostNotification',
        NotificationsResolver.inappropriatePostNotificationAction
      );
    },
  ]);
