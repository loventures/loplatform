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

import { Reducer } from 'redux';

import { CLOSE_MODAL, OPEN_MODAL, SET_MODAL_DATA } from './modalActions';
import { ModalIds } from './modalIds';

export interface ModalState {
  openModalId: ModalIds;
  data: any;
}

const initialState: ModalState = {
  openModalId: null,
  data: {},
};

type ModalAction =
  | {
      type: typeof OPEN_MODAL;
      modalId: ModalIds;
      data?: any;
    }
  | {
      type: typeof SET_MODAL_DATA;
      data: any;
    }
  | {
      type: typeof CLOSE_MODAL;
      modalId: ModalIds;
    };

const modalReducer: Reducer<ModalState, ModalAction> = (state = initialState, action) => {
  switch (action.type) {
    case OPEN_MODAL:
      return {
        ...state,
        openModalId: action.modalId,
        data: {
          ...state.data,
          ...(action.data ? action.data : {}),
        },
      };
    case SET_MODAL_DATA:
      return {
        ...state,
        data: action.data,
      };
    case CLOSE_MODAL:
      return !action.modalId || action.modalId === state.openModalId ? initialState : state;
    default:
      return state;
  }
};

export default modalReducer;
