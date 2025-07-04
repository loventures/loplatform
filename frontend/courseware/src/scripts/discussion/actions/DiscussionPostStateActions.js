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

import dayjs from 'dayjs';
import { map } from 'lodash';
import { selectCurrentUser } from '../../utilities/rootSelectors.js';

import { DISCUSSION_POST_SET_INITIAL_STATE } from '../actionTypes.js';
import { createBoardSelector } from '../selectors.js';

export default angular
  .module('lo.discussionBoard.DiscussionPostStateActions', [])
  .service('DiscussionPostStateActions', function DiscussionPostStateActions() {
    const service = {};

    service.makePostStateUpdateActionCreator = discussionId => {
      const selectBoard = createBoardSelector(discussionId);

      const initState = function (post, board, currentUser) {
        if (board.settings.canMarkUnread) {
          return {
            expandBody: !post.track || !post.track.viewed,
          };
        } else {
          const isCurrentUserPost = post.author.handle === currentUser.handle;
          const timestamp = post.depth === 0 ? post.lastActivityTime : post.lastModified;
          const isUpdated = dayjs(timestamp).isAfter(board.lastVisitedTime);

          const isNew = post.newDescendantCount > 0 || (!isCurrentUserPost && isUpdated);
          return {
            isNew,
            expandBody: isNew,
          };
        }
      };

      return posts => (dispatch, getState) => {
        const board = selectBoard(getState());
        const currentUser = selectCurrentUser(getState());

        dispatch({
          type: DISCUSSION_POST_SET_INITIAL_STATE,
          discussionId,
          data: {
            posts: map(posts, post => ({
              id: post.id,
              ...initState(post, board, currentUser),
            })),
          },
        });
      };
    };

    return service;
  });
