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
import { makeListLoadActionCreator } from '../../../list/makeListActionCreators';

import srs from '../../../utilities/srs';

import { moduleConfig } from '../config';

import { selectProgressReportListState } from '../selectors';
import { lojector } from '../../../loject';

const progressReportLoader = listState => {
  const query = srs.fromListOptions(listState.options);
  return lojector
    .get('enrolledUserService')
    .getStudents(query)
    .then(learners => {
      const userIds = map(learners, 'id');
      return new Promise((resolve, reject) => {
        lojector
          .get('ProgressService')
          .getProgressReportForLearners(userIds)
          .then(progressReport => {
            resolve({
              progressReport,
              learners,
              count: learners.length,
              filterCount: learners.filterCount,
              totalCount: learners.totalCount,
            });
          }, reject);
      });
    });
};

const progressReportSuccessAC = ({ learners, progressReport }) => {
  return [
    createDataListUpdateMergeAction('users', keyBy(learners, 'id')),
    createDataListUpdateMergeAction(
      'progressByContentByUser',
      mapValues(progressReport, r => r.progress)
    ),
    createDataListUpdateMergeAction(
      'progressLastActivityTimeByUser',
      mapValues(progressReport, r => r.lastModified)
    ),
  ];
};

export const loadProgressReportActionCreator = makeListLoadActionCreator(
  moduleConfig,
  getState => progressReportLoader(selectProgressReportListState(getState())),
  false,
  [progressReportSuccessAC],
  data => data.learners.slice()
);
