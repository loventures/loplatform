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

import { mapValues, keyBy, omit } from 'lodash';

import { toContentIdentifierForContext, identiferToId } from '../utilities/contentIdentifier.js';
import { loConfig } from '../bootstrap/loConfig.js';
import UrlBuilder from '../utilities/UrlBuilder.js';
import Course from '../bootstrap/course.js';

export default angular
  .module('lo.service.DiscussionBoardAPILight', [])
  .service('DiscussionBoardAPILight', [
    'Request',
    'Settings',
    function (Request, Settings) {
      const service = {};

      const toContentIdentifier = toContentIdentifierForContext(Course.id);

      service.loadDiscussionListRaw = function (viewingAsId, context = Course.id) {
        const summarize = Settings.isFeatureEnabled('ShowDiscussionBoardSummaries');
        const url = new UrlBuilder(
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

      service.loadDiscussionList = function (viewingAsId, contextId) {
        return service.loadDiscussionListRaw(viewingAsId, contextId).then(discussions => {
          const byId = keyBy(discussions, d => identiferToId(d.id));
          return {
            discussions: mapValues(byId, d => omit(d, 'summary')),
            summaryByContentByUser: {
              [viewingAsId]: mapValues(byId, d => d.summary || emptySummary),
            },
          };
        });
      };

      service.loadDiscussion = function (contentId, summarize, details, context = Course.id) {
        const url = new UrlBuilder(
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

      service.explicitlyVisitDiscussion = function (contentId) {
        const url = new UrlBuilder(loConfig.discussionBoard.visit, {
          discussion: toContentIdentifier(contentId),
        });

        return Request.promiseRequest(url, 'post');
      };

      service.setClosePolicy = (contentId, isClosed) => {
        return service.batchClosePolicy({ [contentId]: isClosed });
      };

      service.batchClosePolicy = contentToAction => {
        const url = new UrlBuilder(loConfig.discussionBoard.close, {}, { context: Course.id });

        return Request.promiseRequest(url, 'post', {
          discussions: contentToAction,
        });
      };

      return service;
    },
  ]);
