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

import { Thunk } from '../types/dcmState';

interface ToastTypes {
  SUCCESS: 'success';
  DANGER: 'danger';
}

export const TOAST_TYPES: ToastTypes = {
  SUCCESS: 'success',
  DANGER: 'danger',
} as const;

export type ToastType = 'success' | 'danger';

export const OPEN_TOAST = 'OPEN_TOAST';
export function openToast(message: string, toastType: ToastType): Thunk {
  return (dispatch, getState) => {
    const { toast } = getState();

    type TimeoutDuration = 2000 | 5000;
    const timeoutDuration: TimeoutDuration = toastType === TOAST_TYPES.SUCCESS ? 2000 : 5000;

    const popToast = () => {
      const timeout = setTimeout(() => dispatch(closeToast()), timeoutDuration);

      dispatch({
        type: OPEN_TOAST,
        message,
        toastType,
        timeout,
      });
    };

    if (toast.open) {
      dispatch(closeToast());
      setTimeout(popToast, 150);
    } else {
      popToast();
    }
  };
}

export const CLOSE_TOAST = 'CLOSE_TOAST';
export function closeToast(): Thunk {
  return (dispatch, getState) => {
    const { toast } = getState();
    const { timeout } = toast;

    if (typeof timeout === 'number') {
      clearTimeout(timeout);
    }

    dispatch({
      type: CLOSE_TOAST,
    });
  };
}
