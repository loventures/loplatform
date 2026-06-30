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

import { createDataListUpdateMergeAction } from '../../utilities/apiDataActions.js';

import { loadingActionCreatorMaker } from '../../utilities/loadingStateUtils.js';

import { reportProgressActionCreator } from './activityActions.js';

import { identiferToId } from '../../utilities/contentIdentifier.js';
import { get } from 'lodash';
import { discussionBoardAPI } from '../../services/discussionBoardAPI.ts';
import * as DiscussionBoardActions from '../../discussion/actions/DiscussionBoardActions.ts';

export { reportProgressActionCreator };

const discussionActivityLoader = (content: any) =>
  Promise.all([
    new Promise((resolve, reject) =>
      discussionBoardAPI.loadDiscussion(content.id, true, true).then(resolve, reject)
    ),
  ]);

const loadDiscussionActivitySuccessACs = ([discussion]: [any]) => {
  return [
    createDataListUpdateMergeAction('discussions', {
      [identiferToId(discussion.id)]: discussion,
    }),
    DiscussionBoardActions.updateLastVisitedActionCreator(
      identiferToId(discussion.id),
      get(discussion, 'summary.lastVisited')
    ),
  ];
};

export const loadDiscussionActivityActionCreator = loadingActionCreatorMaker(
  { sliceName: 'contentActivityLoadingState' },
  discussionActivityLoader,
  [loadDiscussionActivitySuccessACs],
  content => ({ id: content.id })
);
