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

import { keyBy, map, mapValues } from 'lodash';

import { createDataListUpdateMergeAction } from '../../../utilities/apiDataActions';

import { loadingActionCreatorMaker } from '../../../utilities/loadingStateUtils';

import { configWithUserId } from '../config';

import { selectLearnerAssignmentsRowsComponent } from '../selectors/listSelectors';
import { getAttemptOverviews } from '../../../api/attemptOverviewApi';

const assignmentDetailsLoader = (viewingAs, rows) => {
  return getAttemptOverviews(map(rows, 'content.id'), viewingAs.id).then(overviews => {
    return {
      overviews,
      viewingAs,
    };
  });
};

const assignmentDetailsSuccessAC = ({ viewingAs, overviews }) => {
  const activityByContent = mapValues(keyBy(overviews, 'edgePath'), d => ({
    attemptOverview: d,
  }));
  return createDataListUpdateMergeAction('activityByContentByUser', {
    [viewingAs.id]: activityByContent,
  });
};

export const loadAssignmentDetailsActionCreator = getState => {
  const rowState = selectLearnerAssignmentsRowsComponent(getState());
  const userId = rowState.viewingAs.id;
  const loader = () => assignmentDetailsLoader(rowState.viewingAs, rowState.rows);

  return loadingActionCreatorMaker(configWithUserId(userId), loader, [
    assignmentDetailsSuccessAC,
  ])();
};
