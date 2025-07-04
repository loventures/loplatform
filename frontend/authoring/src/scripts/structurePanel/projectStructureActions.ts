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

export const FETCHING_STRUCTURE_START = 'FETCHING_STRUCTURE_START';

export function startFetchingStructure() {
  return {
    type: FETCHING_STRUCTURE_START,
  };
}

export const FETCHING_STRUCTURE_END = 'FETCHING_STRUCTURE_END';

export function endFetchingStructure(commit, success) {
  return {
    type: FETCHING_STRUCTURE_END,
    commit,
    success,
  };
}

export const SET_STRUCTURE_HIDDEN = 'SET_STRUCTURE_HIDDEN';

export const hideStructurePanel = (hidden: boolean) => ({
  type: SET_STRUCTURE_HIDDEN,
  hidden,
});
