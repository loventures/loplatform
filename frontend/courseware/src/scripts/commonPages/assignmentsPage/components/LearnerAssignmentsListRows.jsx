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

import { map } from 'lodash';
import { connect } from 'react-redux';

import { selectLearnerAssignmentsRowsComponent } from '../selectors/listSelectors';
import LearnerAssignmentsItem from './LearnerAssignmentsItem';

const LearnerAssignmentsListRows = ({ rows, viewingAs }) => (
  <ul
    className="list-group list-unstyled py-2"
    id="learner-assignments-list-rows"
  >
    {map(rows, item => (
      <li
        className="my-2"
        key={item.content.id}
      >
        <LearnerAssignmentsItem
          viewingAs={viewingAs}
          {...item}
        />
      </li>
    ))}
  </ul>
);

export default connect(selectLearnerAssignmentsRowsComponent)(LearnerAssignmentsListRows);
