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

import axios from 'axios';
import Course from '../bootstrap/course';
import { loConfig } from '../bootstrap/loConfig';
import { queryClient } from './queryClient';
import { Resource, useSuspenseQuery } from './Resource';
import dayjs from 'dayjs';
import { UserWithRoleInfo } from '../utilities/rootSelectors';
import UrlBuilder from '../utilities/UrlBuilder';
import { useMemo } from 'react';
import { QueryFunction } from 'react-query';

import { SrsList } from '../api/commonTypes';
import User from '../bootstrap/user';
import { UrlAndParamsKey } from './ContentsResource';

export type InstructorNotification = {
  id: string;
  message: string;
  context: number;
  type?: string;
  _type?: string;
  name?: string;
  title?: string;
  topic?: string;
  iconClass?: string;
  time?: Date | dayjs.Dayjs;
  urgency?: string;
};

/**
 * React-Query Resource for Instructor Notifications
 * */
class InstructorNotificationsResource<
  TData extends SrsList<InstructorNotification>,
> extends Resource<TData, [string, string, boolean], UrlAndParamsKey> {
  urlTemplate = loConfig.notifications.base;

  getKey(contentId: string, notificationType: string, excludeSentByMe: boolean): UrlAndParamsKey {
    return [
      {
        contentId,
        notificationType,
        excludeSentByMe,
      },
      this.urlTemplate,
    ];
  }

  /**
   * Reimplementing fetcher because the filter Query params require a UrlBuilder
   * */
  fetcher =
    (_: Record<string, any>): QueryFunction<TData, UrlAndParamsKey> =>
    context => {
      const [params, template, ..._] = context.queryKey;
      const { contentId, notificationType, excludeSentByMe } = params;
      const url = new UrlBuilder(
        template,
        {},
        {
          offset: 0,
          limit: 5,
        }
      );
      if (contentId) {
        url.query.setFilter('topic', 'eq', contentId);
      }

      if (notificationType) {
        url.query.setFilter('_type', 'eq', notificationType);
      }

      if (excludeSentByMe) {
        url.query.setFilter('sender', 'ne', User.id);
      }

      return axios.get(url.toString()).then(({ data }) => data as TData);
    };

  fetch(key: UrlAndParamsKey, config: Record<string, any> = {}) {
    return queryClient.fetchQuery(key, this.fetcher(config));
  }

  read(
    contentId: string,
    notificationType = 'instructorNotification',
    excludeSentByMe = true,
    config?: Record<string, any>
  ) {
    const key = this.getKey(contentId, notificationType, excludeSentByMe);
    const promise = this.fetch(key, config);
    const data = queryClient.getQueryData<TData>(key);
    const fetching = queryClient.isFetching(key);

    return { promise, fetching, data, key };
  }
}

const instructorNotificationsResource = new InstructorNotificationsResource();

export default instructorNotificationsResource;

export const useInstructorNotificationsResource = (
  actualUser: UserWithRoleInfo,
  contentId: string,
  notificationType = 'instructorNotification',
  excludeSentByMe = true,
  sizeOfList = 1
) => {
  const key = instructorNotificationsResource.getKey(contentId, notificationType, excludeSentByMe);
  const fetcher = instructorNotificationsResource.fetcher({ redux: true });
  const selector = useMemo(
    () =>
      ({ objects: notifications }: SrsList<InstructorNotification>) => {
        /**
         * Munge, filter, and sort the notifications before taking how many we want.
         * */
        return notifications
          .map(n => deserializeNotification(n))
          .filter(n => n.context === Course.id)
          .sort((a, b) => dayjs(b.time).diff(a.time))
          .slice(0, sizeOfList);
      },
    [sizeOfList]
  );
  return useSuspenseQuery(key, fetcher, {
    select: selector,
    enabled: actualUser.isStudent,
  });
};

/****** Utils ******/

export const NotificationIconClass = {
  badgeNotification: 'medal2',
  discussionPostNotification: 'bubbles',
  gradeNotification: 'check-squared',
  attemptDeletedNotification: 'trash',
  attemptRestoredNotification: 'toggle',
  contentAvailabilityNotification: 'lock-open',
  gateDateNotification: 'lock-open',
  contentGateNotification: 'lock-open',
  learnerTransferNotification: 'user-check',
  competencyMasteryNotification: 'stars',
  updateNotification: 'reload',
} as const;

/**
 * @description Helper method for assigning iconClasses and types etc
 * @returns {Notification} the notification munged into a slightly more consistent type
 *
 * Note: it is unclear whether this is necessary
 */
export const deserializeNotification = (
  notification: InstructorNotification
): InstructorNotification => {
  if (notification) {
    notification.type = notification._type;
    notification.name = notification.name || notification.title || notification.topic;
    // @ts-ignore
    notification.iconClass = NotificationIconClass[notification.type];
    notification.time = dayjs(notification.time);
    if (typeof notification.urgency === 'string') {
      notification.urgency = notification.urgency.toLowerCase();
    }
  }
  return notification;
};
