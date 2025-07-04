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
import {
  GRADEBOOK_TABLE_TOGGLE_EXTERNAL_IDS,
  GRADEBOOK_TABLE_TOGGLE_FOR_CREDIT_ONLY,
  GRADEBOOK_TABLE_SET_GRADE_DISPLAY_METHOD,
  GRADEBOOK_TABLE_COLLAPSE_TABLE,
  GRADEBOOK_TABLE_EXPAND_GROUP,
} from '../actionTypes';

import { setReduxState } from '../../../utilities/reduxify';

const gradebookTableOptionsReducer = (
  state = {
    showForCreditOnly: true,
    showExternalIds: false,
    gradeDisplayMethod: 'pointsOutOf',
    collapsedTables: [],
  },
  action
) => {
  switch (action.type) {
    case GRADEBOOK_TABLE_COLLAPSE_TABLE: {
      const collapsedTables = state.collapsedTables.slice();
      collapsedTables[action.data] = true;
      return {
        ...state,
        collapsedTables,
      };
    }
    case GRADEBOOK_TABLE_EXPAND_GROUP: {
      const { start, end } = action.data;
      return {
        ...state,
        collapsedTables: map(state.collapsedTables, (status, index) => {
          if (index >= start && index < end) {
            return false;
          } else {
            return status;
          }
        }),
      };
    }
    case GRADEBOOK_TABLE_TOGGLE_FOR_CREDIT_ONLY:
      return setReduxState(state, 'showForCreditOnly', !state.showForCreditOnly);
    case GRADEBOOK_TABLE_TOGGLE_EXTERNAL_IDS:
      return setReduxState(state, 'showExternalIds', !state.showExternalIds);
    case GRADEBOOK_TABLE_SET_GRADE_DISPLAY_METHOD:
      return setReduxState(state, 'gradeDisplayMethod', action.data);
    default:
      return state;
  }
};

export default gradebookTableOptionsReducer;
