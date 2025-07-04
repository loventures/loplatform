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

import { CONTENT_TYPE_LTI, CONTENT_TYPE_SCORM } from '../../utilities/contentTypes.ts';
import { selectCurrentUser } from '../../utilities/rootSelectors.ts';
import { createSelector } from 'reselect';

import { selectContent } from './contentEntrySelectors.ts';
import { selectQuizLikeActivityInfo, showHeaderGrade } from './contentLandmarkSelectors.ts';

const selectShowContentHeaderGrade = createSelector(
  [selectContent, selectCurrentUser, selectQuizLikeActivityInfo],
  (content, viewingAs, { activityState, hasSubmittedAttempts }) => {
    const showAnyGrade = viewingAs.isStudent && showHeaderGrade(content, activityState);
    const hasGrade = !!content.grade;
    const hasPendingGrade =
      content.hasGradebookEntry &&
      content.progress.isFullyCompleted &&
      !content.progress.progressTypes.includes('SKIPPED') &&
      content.typeId !== CONTENT_TYPE_LTI &&
      content.typeId !== CONTENT_TYPE_SCORM;
    return showAnyGrade && (hasGrade || hasPendingGrade || hasSubmittedAttempts);
  }
);

export default selectShowContentHeaderGrade;
