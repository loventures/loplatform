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

import { toContentIdentifierForContext } from '../utilities/contentIdentifier.js';
import { selectCurrentUserGatingInformation } from '../selectors/gatingInformationSelector.js';

import DiscussionPostAPI from './DiscussionPostAPI.js';

import { without, map } from 'lodash';
import { loConfig } from '../bootstrap/loConfig.js';
import UrlBuilder from '../utilities/UrlBuilder.js';
import Course from '../bootstrap/course.js';

export default angular
  .module('lo.service.DiscussionPostReplyAPI', [DiscussionPostAPI.name])
  .service('DiscussionPostReplyAPI', [
    'DiscussionPostAPI',
    '$ngRedux',
    'Request',
    '$q',
    function (DiscussionPostAPI, $ngRedux, Request, $q) {
      const service = {};

      const toContentIdentifier = toContentIdentifierForContext(Course.id);

      service.newReply = function (contentId, data) {
        var url = new UrlBuilder(
          loConfig.discussionPost.list,
          {},
          { discussion: toContentIdentifier(contentId) }
        );
        return Request.promiseRequest(url, 'post', data).then(post =>
          DiscussionPostAPI.toPost(post, contentId)
        );
      };

      service.newThread = function (contentId, data) {
        return service
          .newReply(contentId, data)
          .then(thread => DiscussionPostAPI.toThread(thread, contentId));
      };

      service.updateReply = function (
        contentId,
        postId,
        { title, content, uploads, removals, attachments }
      ) {
        var url = new UrlBuilder(
          loConfig.discussionPost.onePost,
          { postId },
          { discussion: toContentIdentifier(contentId) }
        );

        const attachmentsToKeep = without(
          map(attachments, a => a.id),
          removals
        );

        const request = {
          title,
          content,
          uploads,
          attachments: attachmentsToKeep,
        };

        return Request.promiseRequest(url, 'put', request).then(reply =>
          DiscussionPostAPI.toPost(reply, contentId)
        );
      };

      service.getErrorDetails = (discussionId, error) => {
        let errorDetails = {
          ...error,
          title: 'DISCUSSION_GENERIC_ERROR_TITLE',
          description: 'DISCUSSION_GENERIC_ERROR_DESCRIPTION',
        };
        //@TODO
        //Change this once we have more granular errors for
        //unauthorized access (i.e. why was it unauthorized?)
        //For now get more info and guess based on gating policies?
        if (error.type === 'UNAUTHORIZED_ERROR') {
          const availability = selectCurrentUserGatingInformation($ngRedux.getState())[
            discussionId
          ];
          if (availability.isClosed) {
            return $q.when({
              ...errorDetails,
              title: 'DISCUSSION_CLOSED_MESSAGE',
              description: 'DISCUSSION_CLOSED_EXPLAINATION',
            });
          } else if (availability.isOpen) {
            //probably because of grading?
            return $q.when({
              ...errorDetails,
              title: 'DISCUSSION_POST_EDIT_FORBIDDEN',
              description: 'DISCUSSION_POST_EDIT_FORBIDDEN_EXPLAINATION',
            });
          }
        } else if (error.type === 'CLIENT_ERROR' && error.messages.fileNames) {
          return $q.when({
            ...errorDetails,
            messages: [
              {
                i18nableMessage: 'DISCUSSION_POST_INVALID_ATTACHMENTS',
                data: {
                  fileNames: error.messages.fileNames.join(', '),
                },
              },
            ],
          });
        }

        return $q.when(errorDetails);
      };

      return service;
    },
  ]);
