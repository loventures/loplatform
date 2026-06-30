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

import { createDataListUpdateMergeAction } from '../../utilities/apiDataActions.js';
import { discussionBoardAPI } from '../../services/discussionBoardAPI.ts';

import {
  DISCUSSION_UPDATE_LAST_VISITED,
  DISCUSSION_UPDATE_LAST_VISITED_ERROR,
  DISCUSSION_UPDATE_SETTINGS,
  DISCUSSION_VISITED_NOTICE,
} from '../actionTypes.js';

const validateTime = (maybeTime: any) => {
  const m = dayjs(maybeTime);
  if (m.isValid()) {
    return m.toISOString();
  } else {
    return dayjs(0).toISOString();
  }
};

export const updateLastVisitedActionCreator = (discussionId: any, lastVisitedTime: any) => ({
  type: DISCUSSION_UPDATE_LAST_VISITED,
  discussionId,
  data: {
    lastVisitedTime: validateTime(lastVisitedTime),
  },
});

export const notifyDiscussionVisitedActionCreator = (discussionId: any, lastVisitedTime: any) => ({
  type: DISCUSSION_VISITED_NOTICE,
  discussionId,
  data: {
    lastVisitedTime: validateTime(lastVisitedTime),
  },
});

export const makeVisitBoardActionCreator = (discussionId: any) => (_unused?: any) => (dispatch: any) => {
  discussionBoardAPI
    .explicitlyVisitDiscussion(discussionId)
    .then(
      (response: any) => {
        dispatch(notifyDiscussionVisitedActionCreator(discussionId, response));
      },

      (error: any) =>
        dispatch({
          type: DISCUSSION_UPDATE_LAST_VISITED_ERROR,
          discussionId,
          data: { error },
        })
    );
};

export const makeUpdateSettingsActionCreator = (discussionId: any) => (settings: any) => ({
  type: DISCUSSION_UPDATE_SETTINGS,
  discussionId,
  data: settings,
});

export const makeCloseDiscussionActionCreator =
  (contentItemId: any, discussionId: any, existingPolicies: any) =>
  (settings: any) =>
  (dispatch: any) => {
    discussionBoardAPI
      .setClosePolicy(contentItemId, settings.closeDiscussion, existingPolicies)
      .then(() => {
        const updateSettingsActionCreator = makeUpdateSettingsActionCreator(discussionId);
        dispatch(updateSettingsActionCreator(settings));
        dispatch(
          createDataListUpdateMergeAction('discussions', {
            [contentItemId]: { closed: !!settings.closeDiscussion },
          })
        );
      });
  };
