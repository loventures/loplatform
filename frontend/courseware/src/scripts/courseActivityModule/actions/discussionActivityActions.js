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

import { createDataListUpdateMergeAction } from '../../utilities/apiDataActions.js';

import { loadingActionCreatorMaker } from '../../utilities/loadingStateUtils.js';

import { reportProgressActionCreator } from './activityActions.js';

import { identiferToId } from '../../utilities/contentIdentifier.js';
import { get } from 'lodash';
import { lojector } from '../../loject.js';

export { reportProgressActionCreator };

const discussionActivityLoader = content =>
  Promise.all([
    new Promise((resolve, reject) =>
      lojector
        .get('DiscussionBoardAPI')
        .loadDiscussion(content.id, true, true)
        .then(resolve, reject)
    ),
  ]);

const loadDiscussionActivitySuccessACs = ([discussion]) => {
  return [
    createDataListUpdateMergeAction('discussions', {
      [identiferToId(discussion.id)]: discussion,
    }),
    lojector
      .get('DiscussionBoardActions')
      .updateLastVisitedActionCreator(
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
