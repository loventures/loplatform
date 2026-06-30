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

import dayjs from 'dayjs';
import duration from 'dayjs/plugin/duration';
import { extend, isFunction, keys, map } from 'lodash';
import { loConfig } from '../../bootstrap/loConfig.ts';
import { deserializeNotification } from '../../resources/InstructorNotificationsResource.js';
import UrlBuilder from '../../utilities/UrlBuilder.js';

dayjs.extend(duration);

/** The Request object this service needs (promiseRequest + NO_SESSION_EXTENSION). */
export interface RequestLike {
  promiseRequest(url: any, method?: string, ...rest: any[]): PromiseLike<any>;
  NO_SESSION_EXTENSION?: any;
}

/**
 * Alerts/notifications API, migrated verbatim from the AngularJS
 * `NotificationService` to plain TS taking the injected `Request`.
 */
export const makeNotificationService = (Request: RequestLike) => {
  const NotificationService: any = {
    summary: {},
  };

  NotificationService.actions = {};

  /** @description How far back in the past should we search for alerts. */
  NotificationService.ALERT_EXPIRATION_TIME = dayjs.duration(1, 'weeks');

  NotificationService.getSummary = function () {
    const url = new (UrlBuilder as any)(loConfig.alerts.summary);

    return Request.promiseRequest(url, 'get', null, null, null, null, null, Request.NO_SESSION_EXTENSION).then(
      function (summary: any) {
        extend(NotificationService.summary, {
          count: summary.count,
          lastUpdated: dayjs(summary.date),
          lastViewed: dayjs(summary.viewDate),
        });
        return NotificationService.summary;
      }
    );
  };

  NotificationService.dismiss = function (alertObj: any) {
    return Request.promiseRequest(loConfig.alerts.base + '/' + alertObj.id, 'delete').then(function (result: any) {
      NotificationService.getSummary();
      return result;
    });
  };

  NotificationService.getAlerts = function (limit: any, context: any, aggregationKey: any, lastValidTime: any) {
    const url = new (UrlBuilder as any)(
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
    return Request.promiseRequest(url, 'get').then(function (items: any) {
      return map(items, NotificationService.deserializeAlert);
    });
  };

  NotificationService.deserializeAlert = function (alertObj: any) {
    if (alertObj) {
      alertObj.notification = deserializeNotification(alertObj.notification);
      alertObj.text = alertObj.notification ? alertObj.notification.text : '';
    }
    return alertObj;
  };

  NotificationService.updateLastViewed = function () {
    return Request.promiseRequest(loConfig.alerts.viewed, 'post', {
      date: dayjs(),
    });
  };

  NotificationService.action = function (notification: any) {
    if (notification) {
      const action = NotificationService.actions[notification._type];
      if (isFunction(action)) {
        action(notification);
      } else if (isFunction(NotificationService.actions['default'])) {
        NotificationService.actions['default'](notification);
      } else {
        console.warn('No action for ', notification._type, 'have', keys(NotificationService.actions));
      }
    }
  };

  NotificationService.addAction = function (notificationType: any, action: any) {
    if (isFunction(action)) {
      NotificationService.actions[notificationType] = action;
    }
  };

  return NotificationService;
};

export type NotificationService = ReturnType<typeof makeNotificationService>;
