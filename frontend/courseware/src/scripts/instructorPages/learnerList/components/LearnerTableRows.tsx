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

import { LearnerListComponent } from '../../../instructorPages/learnerList/learnerListActions';
import { LearnerTableRecord } from '../../../instructorPages/learnerList/learnerListStore';
import { map } from 'lodash';
import React from 'react';

import LearnerTableRow from './LearnerTableRow';

type LearnerTableRowsStateProps = LearnerListComponent;

const LearnerTableRows: React.FC<LearnerTableRowsStateProps> = ({ state, _dispatch }) => {
  const rows: LearnerTableRecord[] =
    state.students?.map(learner => ({
      learner,
      grade: state.grades?.[learner.id],
      progress: state.progress?.[learner.id],
    })) ?? [];
  return (
    <tbody>
      {map(rows, row => (
        <LearnerTableRow
          key={row.learner.id}
          {...row}
          state={state}
          _dispatch={_dispatch}
        />
      ))}
    </tbody>
  );
};

export default LearnerTableRows;
