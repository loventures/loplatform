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

import { CLOSE_TOAST, OPEN_TOAST, ToastType } from './actions';

export interface ToastState {
  open: boolean;
  message?: string;
  toastType?: ToastType;
  timeout?: string;
}

const initialState: ToastState = {
  open: false,
};

export default function toastReducer(state: ToastState = initialState, action): ToastState {
  switch (action.type) {
    case OPEN_TOAST: {
      const { message, toastType, timeout } = action;

      return {
        ...state,
        open: true,
        message,
        toastType,
        timeout,
      };
    }
    case CLOSE_TOAST: {
      return {
        ...state,
        open: false,
      };
    }
    default: {
      return state;
    }
  }
}
