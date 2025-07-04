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

import { keyBy } from 'lodash';

import { createDataListUpdateMergeAction } from '../../../utilities/apiDataActions';

import { loadingActionCreatorMaker } from '../../../utilities/loadingStateUtils';

import { sortWithWorkActionCreator, sortWithoutWorkActionCreator } from './listActions';

import { sortByProps } from '../config';
import { lojector } from '../../../loject';

const activityOverviewSuccessAC = ({ contentId, overview }) => {
  return [
    createDataListUpdateMergeAction('activityOverviewByUserByContent', {
      [contentId]: overview,
    }),
    sortWithWorkActionCreator(contentId, sortByProps.SORT_SUBMISSION_DATE),
    sortWithoutWorkActionCreator(contentId, sortByProps.SORT_GIVEN_NAME_ASC),
  ];
};

export const loadActivityOverviewActionCreator = loadingActionCreatorMaker(
  { sliceName: 'activityOverviewLoadingStateByContent' },
  contentId => {
    return lojector
      .get('QuizOverviewAPI')
      .getStudentSubmissionSummaryByContent(contentId)
      .then(overview => ({
        contentId,
        overview: keyBy(overview, 'learner.id'),
      }));
  },
  [activityOverviewSuccessAC],
  contentId => ({ contentId })
);
