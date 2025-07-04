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

import Course from '../bootstrap/course.js';
import { get } from 'lodash';
import { b64Encode } from '../utilities/b64Utils.js';

import CoursesAPI from './CoursesAPI.js';

export default angular
  .module('lo.services.NotificationsResolver', [CoursesAPI.name])
  .service('NotificationsResolver', [
    '$window',
    'CoursesAPI',
    function ($window, CoursesAPI) {
      var S = {};

      S.instructorNotificationAction = function (notification) {
        // this is used for several unrelated notifications
        var contentId = get(notification, 'topic');
        var courseId = get(notification, 'context') || Course.id;
        S.gotoNotification(contentId, courseId);
      };

      S.gradeNotificationAction = function (notification) {
        var contentId = get(notification, 'contentItem');
        var courseId = get(notification, 'context') || Course.id;
        S.gotoNotification(contentId, courseId);
      };

      S.postNotificationAction = function (notification) {
        const edgePath = get(notification, 'edgePath');
        const courseId = get(notification, 'context') || Course.id;
        const data = {
          postId: get(notification, 'postId'),
          threadId: get(notification, 'threadId'),
        };
        S.gotoNotification(edgePath, courseId, data);
      };

      S.inappropriatePostNotificationAction = function (notification) {
        const edgePath = get(notification, 'edgePath');
        const courseId = get(notification, 'context') || Course.id;
        const data = {
          postId: get(notification, 'postId'),
          threadId: get(notification, 'threadId'),
          inappropriate: true,
        };
        S.gotoNotification(edgePath, courseId, data, 'instructor');
      };

      S.gateDateNotificationAction = function (notification) {
        var contentId = get(notification, 'contentId');
        var courseId = get(notification, 'context') || Course.id;
        S.gotoNotification(contentId, courseId);
      };

      S.gotoNotification = function (contentId, courseId, data, role) {
        if (!contentId) {
          return;
        }

        if (courseId === Course.id) {
          S.inCourseAction(contentId, data, role);
        } else {
          S.crossCourseAction(contentId, courseId, data, role);
        }
      };

      S.inCourseAction = function (contentId, data, role = 'student') {
        var href = `${Course.url}/#/${role}/content/${contentId}`;

        if (data) {
          href += '?data=' + b64Encode(data);
        }
        $window.location.href = href;
      };

      S.crossCourseAction = function (contentId, courseId, data, role) {
        //TODO investigate if we can include course specific role in the notification data
        //But for now...guess by notification type?
        role = role || 'student';

        return CoursesAPI.getCourse(courseId).then(
          function (course) {
            var href = `${course.url}/#/${role}/content/${contentId}`;

            if (data) {
              href += '?data=' + b64Encode(data);
            }
            $window.location.href = href;
            //window.location.reload();;
          },
          function (error) {
            console.error(
              `notification for ${contentId} in ${courseId} could not find course. ${error}`
            );
          }
        );
      };
      return S;
    },
  ]);
