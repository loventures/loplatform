/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

import { isString } from 'lodash';

import { ADD_TOAST_MESSAGE, HIDE_TOAST_MESSAGE, REMOVE_TOAST_MESSAGE, SHOW_TOAST_MESSAGE } from './actionTypes.js';
import { toastSlice } from './sliceNames.js';

/**
 * The toast Redux action creators, as a pure TS module (the source of truth). Was the Angular
 * `.service('ToastActions')`; its only Angular coupling was `$timeout` for the 250ms show/remove
 * transition delays — replaced here with `setTimeout`, which is safe because the toast stack is rendered
 * only by the React `ToastContainer` (`useSelector`), so no Angular digest needs to be nudged. The thin
 * Angular adapter in `ToastActions.js` re-exposes this for the remaining Angular injector
 * (`DiscussionPostActions`); React code imports these functions directly.
 *
 * Behaviour is preserved verbatim from the original service, including `displayToastThunkActionCreator`
 * passing the built config object straight to `addToastMessageActionCreator`.
 */

type Dispatch = (action: any) => any;

let toastId = 0;

export const buildToastConfig = (msg: any, duration?: any, type?: any) => {
  const colorCls = isString(type) ? 'toast-' + type : 'toast-default';
  const config = {
    toastId: toastId++,
    cls: {
      show: false,
      [colorCls]: true,
    },
    msg: msg,
    dismissOnTimeout: duration,
  };

  return config;
};

export const addToastMessageActionCreator = (msg: any, duration?: any, type?: any) => ({
  type: ADD_TOAST_MESSAGE,
  sliceName: toastSlice,
  data: {
    ...buildToastConfig(msg, duration, type),
  },
});

export const removeToastMessageActionCreator = (toastId: any) => ({
  type: REMOVE_TOAST_MESSAGE,
  sliceName: toastSlice,
  data: {
    toastId,
  },
});

export const showToastMessageActionCreator = (toastId: any) => ({
  type: SHOW_TOAST_MESSAGE,
  sliceName: toastSlice,
  data: {
    toastId,
  },
});

export const hideToastMessageActionCreator = (toastId: any) => ({
  type: HIDE_TOAST_MESSAGE,
  sliceName: toastSlice,
  data: {
    toastId,
  },
});

export const displayToastThunkActionCreator = (msg: any, duration?: any, type?: any) => (dispatch: Dispatch) => {
  const toastConfig = buildToastConfig(msg, duration, type);
  const addToastMessageAction = addToastMessageActionCreator(toastConfig);
  const showToastMessageAction = showToastMessageActionCreator(toastConfig.toastId);
  dispatch(addToastMessageAction);
  // slight delay to allow for prettier ui transition
  setTimeout(() => dispatch(showToastMessageAction), 250);
};

export const dismissToastThunkActionCreator = (toastId: any) => (dispatch: Dispatch) => {
  const hideToastMessageAction = hideToastMessageActionCreator(toastId);
  const removeToastMessageAction = removeToastMessageActionCreator(toastId);
  dispatch(hideToastMessageAction);
  // slight delay to allow for prettier ui transition
  setTimeout(() => dispatch(removeToastMessageAction), 250);
};

/** The same surface as the old `ToastActions` service, for the Angular adapter / lojector consumers. */
export const toastActions = {
  buildToastConfig,
  addToastMessageActionCreator,
  removeToastMessageActionCreator,
  showToastMessageActionCreator,
  hideToastMessageActionCreator,
  displayToastThunkActionCreator,
  dismissToastThunkActionCreator,
};

export default toastActions;
