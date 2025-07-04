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

import axios from 'axios';
import Course from '../../bootstrap/course';
import { createLoadableReducer } from '../../loRedux/createLoadableReducer';

export type ExemptLearner = {
  id: number;
  givenName: string;
  familyName: string;
};

const fetchDueDateExemptions = () => {
  // loConfig.instructorCustomization.dueDateAccommodation
  const url = `/api/v2/contentConfig/dueDateAccommodation;context=${Course.id}`;
  return axios.get(url).then(({ data }) => data.exemptLearners as ExemptLearner[]);
};

export const { fetchAction: fetchDueDateExemptionsAction, reducer: dueDateExemptionsReducer } =
  createLoadableReducer<typeof fetchDueDateExemptions, ExemptLearner[]>(
    'DueDateExemptions',
    fetchDueDateExemptions
  );
