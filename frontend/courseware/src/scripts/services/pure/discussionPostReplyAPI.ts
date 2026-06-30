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

import type { CourseState } from '../../loRedux';
import { map, without } from 'lodash';
import Course from '../../bootstrap/course.ts';
import { loConfig } from '../../bootstrap/loConfig.ts';
import { selectCurrentUserGatingInformation } from '../../selectors/gatingInformationSelector.js';
import { toContentIdentifierForContext } from '../../utilities/contentIdentifier.js';
import UrlBuilder from '../../utilities/UrlBuilder.js';

/** The Request object this service needs (only promiseRequest). */
export interface RequestLike {
  promiseRequest(url: any, method?: string, ...rest: any[]): PromiseLike<any>;
}

/** Just the post/thread shaping helpers this service borrows from DiscussionPostAPI. */
export interface PostShaper {
  toPost(post: any, contentId: any): any;
  toThread(rootPost: any, contentId?: any): any;
}

/**
 * Resolve a plain value into a promise. The Angular adapter passes `$q.when`
 * (so the resolution lands inside a digest — discussion writing actions read the
 * redux store off it); native callers pass `Promise.resolve`.
 */
export type WhenFn = <T>(value: T) => PromiseLike<T>;

/**
 * Discussion reply/thread-writing API, migrated verbatim from the AngularJS
 * `DiscussionPostReplyAPI` service to plain TS taking the injected `Request`, the
 * `DiscussionPostAPI` shaping helpers, a redux `getState`, and a `when` runtime.
 */
export const makeDiscussionPostReplyAPI = (
  Request: RequestLike,
  DiscussionPostAPI: PostShaper,
  getState: () => CourseState,
  when: WhenFn
) => {
  const toContentIdentifier = toContentIdentifierForContext(Course.id);

  const service: any = {};

  service.newReply = function (contentId: any, data: any) {
    const url = new (UrlBuilder as any)(loConfig.discussionPost.list, {}, { discussion: toContentIdentifier(contentId) });
    return Request.promiseRequest(url, 'post', data).then((post: any) => DiscussionPostAPI.toPost(post, contentId));
  };

  service.newThread = function (contentId: any, data: any) {
    return service.newReply(contentId, data).then((thread: any) => DiscussionPostAPI.toThread(thread, contentId));
  };

  service.updateReply = function (contentId: any, postId: any, { title, content, uploads, removals, attachments }: any) {
    const url = new (UrlBuilder as any)(
      loConfig.discussionPost.onePost,
      { postId },
      { discussion: toContentIdentifier(contentId) }
    );

    const attachmentsToKeep = without(
      map(attachments, (a: any) => a.id),
      removals
    );

    const request = {
      title,
      content,
      uploads,
      attachments: attachmentsToKeep,
    };

    return Request.promiseRequest(url, 'put', request).then((reply: any) => DiscussionPostAPI.toPost(reply, contentId));
  };

  service.getErrorDetails = (discussionId: any, error: any) => {
    const errorDetails: any = {
      ...error,
      title: 'DISCUSSION_GENERIC_ERROR_TITLE',
      description: 'DISCUSSION_GENERIC_ERROR_DESCRIPTION',
    };
    //@TODO
    //Change this once we have more granular errors for
    //unauthorized access (i.e. why was it unauthorized?)
    //For now get more info and guess based on gating policies?
    if (error.type === 'UNAUTHORIZED_ERROR') {
      const availability: any = selectCurrentUserGatingInformation(getState())[discussionId];
      if (availability.isClosed) {
        return when({
          ...errorDetails,
          title: 'DISCUSSION_CLOSED_MESSAGE',
          description: 'DISCUSSION_CLOSED_EXPLAINATION',
        });
      } else if (availability.isOpen) {
        //probably because of grading?
        return when({
          ...errorDetails,
          title: 'DISCUSSION_POST_EDIT_FORBIDDEN',
          description: 'DISCUSSION_POST_EDIT_FORBIDDEN_EXPLAINATION',
        });
      }
    } else if (error.type === 'CLIENT_ERROR' && error.messages.fileNames) {
      return when({
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

    return when(errorDetails);
  };

  return service;
};

export type DiscussionPostReplyAPI = ReturnType<typeof makeDiscussionPostReplyAPI>;
