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

import { loConfig } from '../bootstrap/loConfig.js';
import { deserializeNotification } from '../resources/InstructorNotificationsResource.js';
import dayjs from 'dayjs';
import duration from 'dayjs/plugin/duration';
import { extend, isFunction, keys, map } from 'lodash';
import UrlBuilder from '../utilities/UrlBuilder.js';

dayjs.extend(duration);

export default angular
  .module('lo.services.NotificationService', [])
  .factory('NotificationService', [
    'Request',
    /**
     * @ngdoc service
     * @alias NotificationService
     * @memberof lo.services
     * @description The notification service provides events that a user should look into. There
     * are two major components of the service that actually fall under alerts vs notfications.
     * An alert is a grouping of notifications associated with an item, and notifications are all
     * the individual events.  For example you would get a single alert about people posting to a
     * discussion board, but there were be many notifications one for each time a person posted.
     *
     * Think the FB event notification system but LO custom code.
     * */
    function NotificationService(Request) {
      /** @alias NotificationService **/
      var NotificationService = {
        summary: {},
      };

      NotificationService.actions = {};

      /**
       *  @description How far back in the past should we search for alerts.
       */
      NotificationService.ALERT_EXPIRATION_TIME = dayjs.duration(1, 'weeks');

      /**
     * @description Used to get a quick summary so you can add a bell icon with a number
     * @returns {Promise} with a result format of {
        count: 24,  the count of notification groups within the last week
        date: "2014-01-24T11:23:45Z",  the date of the most recent notification
        viewed: "2014-02-24T11:23:11Z"  the date you most recently explicitly view notifications
    }
     */
      NotificationService.getSummary = function () {
        var url = new UrlBuilder(loConfig.alerts.summary);

        return Request.promiseRequest(
          url,
          'get',
          null,
          null,
          null,
          null,
          null,
          Request.NO_SESSION_EXTENSION
        ).then(function (summary) {
          extend(NotificationService.summary, {
            count: summary.count,
            lastUpdated: dayjs(summary.date),
            lastViewed: dayjs(summary.viewDate),
          });
          return NotificationService.summary;
        });
      };

      /**
       * @description Dismiss an alert, it will not show up in the user view again
       * @params {Object} alertObj the alert you would like to dismiss
       * @returns {Promise} the newly updated summary is also loaded into the service summary
       */
      NotificationService.dismiss = function (alertObj) {
        return Request.promiseRequest(loConfig.alerts.base + '/' + alertObj.id, 'delete').then(
          function (result) {
            NotificationService.getSummary();
            return result;
          }
        );
      };

      /**
       * @description Get the alerts for this user, remember that alerts can be dismissed but
       * notifications are the event stream that generates them.
       * @params {int} limit SRS limit call
       * @params {string} context if you want to restrict this to a course etc
       * @params {string} aggregationKey restrict to alerts with the given aggregation key (type:topicId)
       * @params {Date} lastValidTime filter out alerts before this time
       * @returns {Promise} the newly updated summary is also loaded into the service summary
       */
      NotificationService.getAlerts = function (limit, context, aggregationKey, lastValidTime) {
        var url = new UrlBuilder(
          loConfig.alerts.base,
          {},
          {
            limit: limit || NotificationService.DEFAULT_ALERT_LIMIT,
          }
        );

        url.query.setOrder('time', 'desc');

        if (context) {
          url.query.setFilter('context_id', 'eq', context);
        }

        if (aggregationKey) {
          url.query.setFilter('aggregationKey', 'eq', aggregationKey);
        }

        if (lastValidTime) {
          url.query.setPrefilter('time', 'gt', lastValidTime.toISOString());
        }
        return Request.promiseRequest(url, 'get').then(function (items) {
          return map(items, NotificationService.deserializeAlert);
        });
      };

      /**
       * @description Helper method for icon assignment and a more common return format (type vs _type)
       * @returns {Promise} the newly updated summary is also loaded into the service summary
       */
      NotificationService.deserializeAlert = function (alertObj) {
        if (alertObj) {
          alertObj.notification = deserializeNotification(alertObj.notification);
          alertObj.text = alertObj.notification ? alertObj.notification.text : '';
        }
        return alertObj;
      };

      /**
       * @description updates when we are considered to have last checked for events.
       * @returns {Promise} if it succeeds it will not bail
       */
      NotificationService.updateLastViewed = function () {
        return Request.promiseRequest(loConfig.alerts.viewed, 'post', {
          date: dayjs(),
        });
      };

      /**
       * @description This will undertake the action on the notification if it is provided.
       */
      NotificationService.action = function (notification) {
        if (notification) {
          var action = NotificationService.actions[notification._type];
          if (isFunction(action)) {
            action(notification);
          } else if (isFunction(NotificationService.actions['default'])) {
            NotificationService.actions['default'](notification);
          } else {
            console.warn(
              'No action for ',
              notification._type,
              'have',
              keys(NotificationService.actions)
            );
          }
        }
      };

      /**
       * @description Adds a new notification type into the service, helps with figuring out what
       * to do with the notification (tbd)
       * @params {String} notificationType the key that represents the evenType, on the object this is ._type
       * @params {function} action the action to take for this type
       */
      NotificationService.addAction = function (notificationType, action) {
        if (isFunction(action)) {
          NotificationService.actions[notificationType] = action;
        }
      };

      return NotificationService;
    },
  ]);
