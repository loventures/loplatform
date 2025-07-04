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

import { concat, findIndex } from 'lodash';

import { createNamedReducer } from '../../utilities/reduxify.js';

import { toastSlice } from './sliceNames.js';

import {
  ADD_TOAST_MESSAGE,
  REMOVE_TOAST_MESSAGE,
  SHOW_TOAST_MESSAGE,
  HIDE_TOAST_MESSAGE,
} from './actionTypes.js';

const reducer = (toasts = { listState: [] }, action) => {
  switch (action.type) {
    case ADD_TOAST_MESSAGE: {
      const existingToasts = toasts.listState || [];
      return {
        listState: concat(existingToasts, {
          ...action.data,
        }),
      };
    }
    case REMOVE_TOAST_MESSAGE: {
      const index = findIndex(toasts.listState, {
        toastId: action.data.toastId,
      });
      if (index === -1) {
        return toasts;
      }
      return {
        listState: [...toasts.listState.slice(0, index), ...toasts.listState.slice(index + 1)],
      };
    }
    case SHOW_TOAST_MESSAGE: {
      const index = findIndex(toasts.listState, {
        toastId: action.data.toastId,
      });
      if (index === -1) {
        return toasts;
      }
      const toast = toasts.listState[index];
      const newToast = {
        ...toast,
        cls: {
          ...toast.cls,
          show: true,
        },
      };
      return {
        listState: [
          ...toasts.listState.slice(0, index),
          newToast,
          ...toasts.listState.slice(index + 1),
        ],
      };
    }

    case HIDE_TOAST_MESSAGE: {
      const index = findIndex(toasts.listState, {
        toastId: action.data.toastId,
      });
      if (index === -1) {
        return toasts;
      }
      const toast = toasts.listState[index];
      const newToast = {
        ...toast,
        cls: {
          ...toast.cls,
          show: false,
        },
      };
      return {
        listState: [
          ...toasts.listState.slice(0, index),
          newToast,
          ...toasts.listState.slice(index + 1),
        ],
      };
    }

    default:
      return toasts;
  }
};

export { toastSlice };
export default createNamedReducer(toastSlice, reducer);
