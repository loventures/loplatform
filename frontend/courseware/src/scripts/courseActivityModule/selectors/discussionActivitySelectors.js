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

import { createSelector } from 'reselect';

import { createInstanceSelector } from '../../utilities/reduxify.js';

import { selectCurrentUserRubricScores } from '../../selectors/rubricScoreSelectors.js';

import { selectContentActivityLoaderComponent } from './activitySelectors.js';

export { selectContentActivityLoaderComponent };

import { selectPageContentId } from '../../courseContentModule/selectors/contentEntrySelectors.js';

import { discussionsSelector } from '../../selectors/discussionSelectors.js';

export const pageDiscussionSelector = createInstanceSelector(
  discussionsSelector,
  selectPageContentId
);

export const contentDiscussionActivitySelector = createSelector(
  [pageDiscussionSelector, selectCurrentUserRubricScores],
  (discussion, rubricScores = {}) => {
    return {
      discussion,
      rubricScore: discussion && rubricScores[discussion.id],
    };
  }
);
