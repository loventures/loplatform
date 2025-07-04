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

import { groupBy, keyBy, map, mapValues } from 'lodash';
import { makeListLoadActionCreator } from '../../../list/makeListActionCreators';
import { createDataListUpdateMergeAction } from '../../../utilities/apiDataActions';
import srs from '../../../utilities/srs';

import { moduleConfig } from '../config';
import { lojector } from '../../../loject';

const gradebookTableLoader = listOptions => {
  if (!listOptions.pageSize) {
    listOptions.pageSize = 25;
  }
  const query = srs.fromListOptions(listOptions);
  if (listOptions.search?.options?.inactive) {
    query.prefilters.push(['', 'includeInactive', '']);
    query.embed = 'roles';
  }
  return lojector
    .get('enrolledUserService')
    .getStudents(query)
    .then(learners => {
      return lojector
        .get('GradebookAPI')
        .getGradesForUsers(map(learners, 'id'), undefined, true)
        .then(grades => {
          return {
            learners,
            grades,
            count: learners.length,
            filterCount: learners.filterCount,
            totalCount: learners.totalCount,
          };
        });
    });
};

const gradebookTableSuccessAC = ({ learners, grades }) => {
  const gradeByContentByUser = mapValues(groupBy(grades, 'user_id'), grades =>
    keyBy(grades, 'column_id')
  );
  return [
    createDataListUpdateMergeAction('users', keyBy(learners, 'id')),
    createDataListUpdateMergeAction('gradeByContentByUser', gradeByContentByUser),
  ];
};

export const loadGradebookTableActionCreator = makeListLoadActionCreator(
  moduleConfig,
  getState => gradebookTableLoader(getState().ui[moduleConfig.sliceName].options),
  false,
  [gradebookTableSuccessAC],
  data => data.learners.slice()
);
