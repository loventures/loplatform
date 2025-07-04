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
import { createUrl, loConfig } from '../bootstrap/loConfig';
import { UrlAndParamsKey } from './ContentsResource';
import { queryClient } from './queryClient';
import { Resource, useSuspenseQuery } from './Resource';
import UrlBuilder from '../utilities/UrlBuilder';
import { QueryFunction, useMutation } from 'react-query';

import { SrsList } from '../api/commonTypes';
import { QnaAttachment } from '../qna/qnaApi';

const VALID_NUDGE_TYPES = [
  'postNotification',
  'instructorNotification',
  'qnaNotification',
  'instructorMessageSentNotification',
];

type BaseNotification = {
  context: number;
  id: number;
  urgency: 'Safe';
  topic: string; // null or edgepath?
  time: string;
  sender: number;
};

export const isPostNotification = (
  alert: AlertNotification
): alert is AlertNotification<PostNotification> => {
  return alert.notification._type === 'postNotification';
};

export type PostNotification = BaseNotification & {
  _type: 'postNotification';
  threadId: number;
  authorId: number;
  postId: number;
  edgePath: string;
  title: string;
};

export const isFeedbackNotification = (
  alert: AlertNotification
): alert is AlertNotification<FeedbackNotification> => {
  return alert.notification._type === 'instructorNotification';
};

export type FeedbackNotification = BaseNotification & {
  _type: 'instructorNotification';
  message: string;
};

export const isQnaNotification = (
  alert: AlertNotification
): alert is AlertNotification<QnaNotification> => {
  return alert.notification._type === 'qnaNotification';
};

export type QnaNotification = BaseNotification & {
  _type: 'qnaNotification';
  edgePath: string;
  questionId: number;
};

export const isInstructorMessageSentNotification = (
  alert: AlertNotification
): alert is AlertNotification<InstructorMessageSentNotification> => {
  return alert.notification._type === 'instructorMessageSentNotification';
};

export type InstructorMessageSentNotification = BaseNotification & {
  _type: 'instructorMessageSentNotification';
  questionId: number;
  messageId: number;
  subject: string;
  body: string;
  attachments: QnaAttachment[];
};

type NotificationType =
  | PostNotification
  | FeedbackNotification
  | QnaNotification
  | InstructorMessageSentNotification;

export type AlertNotification<NType extends NotificationType = NotificationType> = {
  _type: 'alert';
  id: number;
  notification: NType;
  time: string;
  count: number;
  aggregationKey: string;
  context_id: number;
  viewed: boolean;
};

/**
 * Resource for Alerts. In our usage, these will tend to be from the same
 * course where we're asking. No cross-course nudges.
 *
 * TODO: notes
 * Notes: we we need to figure out if Redux is the source of truth we need or if
 * we can abandon it and/or plug in the SSE events.
 *
 * */
class AlertsResource<TData extends SrsList<AlertNotification>> extends Resource<
  TData,
  [number, number, number],
  UrlAndParamsKey
> {
  urlTemplate = loConfig.alerts.base;

  getKey(limit: number, offset: number, contextId: number): UrlAndParamsKey {
    return [{ limit, offset, contextId }, this.urlTemplate];
  }

  /**
   * Reimplementing fetcher because the filter Query params require a UrlBuilder
   * */
  fetcher =
    (_: Record<string, any>): QueryFunction<TData, UrlAndParamsKey> =>
    context => {
      const [params, template] = context.queryKey;
      const { limit, offset, contextId } = params;
      const url = new UrlBuilder(
        template,
        {},
        {
          offset: offset,
          limit: limit,
        }
      );

      url.query.setOrder('time', 'desc');

      if (contextId) {
        url.query.setFilter('context_id', 'eq', contextId);
      }

      // Unused matrix filters.
      // if (aggregationKey) {
      //   url.query.setFilter('aggregationKey', 'eq', aggregationKey);
      // }
      // if (lastValidTime) {
      //   url.query.setPrefilter('time', 'gt', lastValidTime.toISOString());
      // }

      return axios.get(url.toString()).then(({ data }) => data as TData);
    };

  fetch(key: UrlAndParamsKey, config: Record<string, any> = {}) {
    return queryClient.fetchQuery(key, this.fetcher(config));
  }

  read(limit: number, offset: number, contextId: number, config?: Record<string, any>) {
    const key = this.getKey(limit, offset, contextId);
    const promise = this.fetch(key, config);
    const data = queryClient.getQueryData<TData>(key);
    const fetching = queryClient.isFetching(key);

    return { promise, fetching, data, key };
  }

  markAsViewed(alertId: number) {
    return axios.post(createUrl(loConfig.alerts.markAsViewed, { alertId }));
  }

  getData(key: UrlAndParamsKey) {
    return queryClient.getQueryData<TData>(key);
  }

  setData(key: UrlAndParamsKey, newData: TData) {
    return queryClient.setQueryData<TData>(key, newData);
  }
}

const alertsResource = new AlertsResource();

export default alertsResource;

export const useAlertsResource = (limit = 5, offset = 0, contextId: number = Course.id) => {
  const key = alertsResource.getKey(limit, offset, contextId);
  const fetcher = alertsResource.fetcher({ redux: true });
  return useSuspenseQuery(key, fetcher, {
    select: data => data.objects.filter(a => VALID_NUDGE_TYPES.includes(a.notification._type)),
  });
};

export const useUnviewedAlerts = (limit = 5, offset = 0, contextId: number = Course.id) => {
  const key = alertsResource.getKey(limit, offset, contextId);
  const fetcher = alertsResource.fetcher({ redux: true });
  return useSuspenseQuery(key, fetcher, {
    select: data =>
      data.objects
        .filter(a => !a.viewed)
        .filter(a => VALID_NUDGE_TYPES.includes(a.notification._type)),
  });
};

export const useAlertViewedMutation = (alertId: number) =>
  useMutation(() => alertsResource.markAsViewed(alertId), {
    onSuccess: async () => {
      // update the cache since our endpoint returns a 204.
      const key = alertsResource.getKey(5, 0, Course.id);
      const oldData = alertsResource.getData(key);

      if (oldData) {
        const newObjects = oldData.objects.map(alert => {
          if (alert.id === alertId) {
            alert.viewed = true;
            return alert;
          } else {
            return alert;
          }
        });
        alertsResource.setData(key, {
          ...oldData,
          objects: newObjects ?? [],
        });
      } else {
        alertsResource.invalidateAll();
      }
    },
  });
