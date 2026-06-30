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

import { keyBy, mapValues, omit } from 'lodash';
import Course from '../../bootstrap/course.ts';
import { loConfig } from '../../bootstrap/loConfig.ts';
import { identiferToId, toContentIdentifierForContext } from '../../utilities/contentIdentifier.js';
import UrlBuilder from '../../utilities/UrlBuilder.js';

/** The Request object this service needs (only promiseRequest). */
export interface RequestLike {
  promiseRequest(url: any, method?: string, ...rest: any[]): PromiseLike<any>;
}

/** Minimal shape of the injected Settings service used here. */
export interface SettingsLike {
  isFeatureEnabled(feature: string): boolean;
}

/**
 * "Light" discussion board API (board lists + a board + visit/close policy),
 * migrated verbatim from the AngularJS `DiscussionBoardAPILight` service to plain
 * TS taking the injected `Request` and `Settings`.
 */
export const makeDiscussionBoardAPILight = (Request: RequestLike, Settings: SettingsLike) => {
  const toContentIdentifier = toContentIdentifierForContext(Course.id);

  const service: any = {};

  service.loadDiscussionListRaw = function (viewingAsId: any, context: any = Course.id) {
    const summarize = Settings.isFeatureEnabled('ShowDiscussionBoardSummaries');
    const url = new (UrlBuilder as any)(
      loConfig.discussionBoard.list,
      {
        userId: viewingAsId,
      },
      {
        summarize,
        context,
      }
    );

    return Request.promiseRequest(url);
  };

  const emptySummary = {
    participantCount: 0,
    postCount: 0,
  };

  service.loadDiscussionList = function (viewingAsId: any, contextId: any) {
    return service.loadDiscussionListRaw(viewingAsId, contextId).then((discussions: any) => {
      const byId = keyBy(discussions, (d: any) => identiferToId(d.id));
      return {
        discussions: mapValues(byId, (d: any) => omit(d, 'summary')),
        summaryByContentByUser: {
          [viewingAsId]: mapValues(byId, (d: any) => d.summary || emptySummary),
        },
      };
    });
  };

  service.loadDiscussion = function (contentId: any, summarize: any, details: any, context: any = Course.id) {
    const url = new (UrlBuilder as any)(
      loConfig.discussionBoard.oneBoard,
      {
        discussion: toContentIdentifier(contentId),
      },
      {
        context,
        summarize,
        details,
      }
    );

    return Request.promiseRequest(url);
  };

  service.explicitlyVisitDiscussion = function (contentId: any) {
    const url = new (UrlBuilder as any)(loConfig.discussionBoard.visit, {
      discussion: toContentIdentifier(contentId),
    });

    return Request.promiseRequest(url, 'post');
  };

  service.setClosePolicy = (contentId: any, isClosed: any) => {
    return service.batchClosePolicy({ [contentId]: isClosed });
  };

  service.batchClosePolicy = (contentToAction: any) => {
    const url = new (UrlBuilder as any)(loConfig.discussionBoard.close, {}, { context: Course.id });

    return Request.promiseRequest(url, 'post', {
      discussions: contentToAction,
    });
  };

  return service;
};

export type DiscussionBoardAPILight = ReturnType<typeof makeDiscussionBoardAPILight>;
