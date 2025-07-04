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
import DiscussionBoardAPI from '../../services/DiscussionBoardAPI.js';
import enrolledUserService from '../../services/enrolledUserService.js';
import { createDataListUpdateMergeAction } from '../../utilities/apiDataActions.js';

import {
  DISCUSSION_UPDATE_LAST_VISITED,
  DISCUSSION_UPDATE_LAST_VISITED_ERROR,
  DISCUSSION_UPDATE_SETTINGS,
  DISCUSSION_VISITED_NOTICE,
} from '../actionTypes.js';

export default angular
  .module('lo.discussionBoard.DiscussionBoardActions', [
    DiscussionBoardAPI.name,
    enrolledUserService.name,
  ])
  .service('DiscussionBoardActions', [
    'DiscussionBoardAPI',
    function DiscussionBoardActions(DiscussionBoardAPI) {
      const service = {};

      const validateTime = maybeTime => {
        const m = dayjs(maybeTime);
        if (m.isValid()) {
          return m.toISOString();
        } else {
          return dayjs(0).toISOString();
        }
      };

      service.updateLastVisitedActionCreator = (discussionId, lastVisitedTime) => ({
        type: DISCUSSION_UPDATE_LAST_VISITED,
        discussionId,
        data: {
          lastVisitedTime: validateTime(lastVisitedTime),
        },
      });

      service.notifyDiscussionVisitedActionCreator = (discussionId, lastVisitedTime) => ({
        type: DISCUSSION_VISITED_NOTICE,
        discussionId,
        data: {
          lastVisitedTime: validateTime(lastVisitedTime),
        },
      });

      service.makeVisitBoardActionCreator = discussionId => () => dispatch => {
        DiscussionBoardAPI.explicitlyVisitDiscussion(discussionId).then(
          response => {
            dispatch(service.notifyDiscussionVisitedActionCreator(discussionId, response));
          },

          error =>
            dispatch({
              type: DISCUSSION_UPDATE_LAST_VISITED_ERROR,
              discussionId,
              data: { error },
            })
        );
      };

      service.makeUpdateSettingsActionCreator = discussionId => settings => ({
        type: DISCUSSION_UPDATE_SETTINGS,
        discussionId,
        data: settings,
      });

      service.makeCloseDiscussionActionCreator =
        (contentItemId, discussionId, existingPolicies) => settings => dispatch => {
          DiscussionBoardAPI.setClosePolicy(
            contentItemId,
            settings.closeDiscussion,
            existingPolicies
          ).then(() => {
            const updateSettingsActionCreator =
              service.makeUpdateSettingsActionCreator(discussionId);
            dispatch(updateSettingsActionCreator(settings));
            dispatch(
              createDataListUpdateMergeAction('discussions', {
                [contentItemId]: { closed: !!settings.closeDiscussion },
              })
            );
          });
        };

      return service;
    },
  ]);
