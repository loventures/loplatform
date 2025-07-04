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

import {
  GRADEBOOK_TABLE_TOGGLE_EXTERNAL_IDS,
  GRADEBOOK_TABLE_TOGGLE_FOR_CREDIT_ONLY,
  GRADEBOOK_TABLE_SET_GRADE_DISPLAY_METHOD,
  GRADEBOOK_TABLE_COLLAPSE_TABLE,
  GRADEBOOK_TABLE_EXPAND_GROUP,
} from '../actionTypes';

export const toggleExternalIdsAC = () => ({
  type: GRADEBOOK_TABLE_TOGGLE_EXTERNAL_IDS,
});
export const toggleForCreditOnlyAC = () => ({
  type: GRADEBOOK_TABLE_TOGGLE_FOR_CREDIT_ONLY,
});
export const setGradeDisplayMethodAC = data => ({
  type: GRADEBOOK_TABLE_SET_GRADE_DISPLAY_METHOD,
  data,
});
export const collapseTableAC = data => ({
  type: GRADEBOOK_TABLE_COLLAPSE_TABLE,
  data,
});
export const expandTablesAC = (start, end) => ({
  type: GRADEBOOK_TABLE_EXPAND_GROUP,
  data: { start, end },
});
