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

import { INITIALIZE_DCM } from '../dcmStoreConstants';
import {
  FETCHING_STRUCTURE_END,
  FETCHING_STRUCTURE_START,
  SET_STRUCTURE_HIDDEN,
} from './projectStructureActions';
import storedStructureState from './storedStructureState';

export type ProjectStructure = {
  hidden: boolean;
  isFetching: boolean;
  success: null | 'Delta' | 'Full' | 'Error'; // this is set for a period after a fetch, used to debounce fetches
  fetchedCommit: number | null; // this is set when an explicit prior commit was loaded
  revertCount: number;
};

const initialState = {
  hidden: false,
  isFetching: false,
  success: null,
  fetchedCommit: null,
  revertCount: 0,
};

export default function projectStructure(state = initialState, action) {
  switch (action.type) {
    case INITIALIZE_DCM: {
      return {
        ...state,
        hidden: !!storedStructureState.getHidden(),
      };
    }
    case FETCHING_STRUCTURE_START: {
      return {
        ...state,
        isFetching: true,
      };
    }
    case FETCHING_STRUCTURE_END: {
      return {
        ...state,
        isFetching: false,
        fetchedCommit: action.commit === undefined ? state.fetchedCommit : action.commit,
        success: action.success,
      };
    }
    case SET_STRUCTURE_HIDDEN: {
      storedStructureState.setHidden(action.hidden);
      return {
        ...state,
        hidden: action.hidden,
      };
    }
    default: {
      return { ...state };
    }
  }
}
