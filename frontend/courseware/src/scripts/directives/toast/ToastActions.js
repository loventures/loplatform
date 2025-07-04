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

import { isString } from 'lodash';

import {
  ADD_TOAST_MESSAGE,
  REMOVE_TOAST_MESSAGE,
  SHOW_TOAST_MESSAGE,
  HIDE_TOAST_MESSAGE,
} from './actionTypes.js';

import { toastSlice } from './sliceNames.js';

export default angular.module('lo.directives.ToastActions', []).service('ToastActions', [
  '$timeout',
  function ToastActions($timeout) {
    const service = {};

    let toastId = 0;
    service.buildToastConfig = (msg, duration, type) => {
      const colorCls = isString(type) ? 'toast-' + type : 'toast-default';
      let config = {
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

    service.addToastMessageActionCreator = (msg, duration, type) => ({
      type: ADD_TOAST_MESSAGE,
      sliceName: toastSlice,
      data: {
        ...service.buildToastConfig(msg, duration, type),
      },
    });

    service.removeToastMessageActionCreator = toastId => ({
      type: REMOVE_TOAST_MESSAGE,
      sliceName: toastSlice,
      data: {
        toastId,
      },
    });

    service.showToastMessageActionCreator = toastId => ({
      type: SHOW_TOAST_MESSAGE,
      sliceName: toastSlice,
      data: {
        toastId,
      },
    });

    service.hideToastMessageActionCreator = toastId => ({
      type: HIDE_TOAST_MESSAGE,
      sliceName: toastSlice,
      data: {
        toastId,
      },
    });

    service.displayToastThunkActionCreator = (msg, duration, type) => dispatch => {
      const toastConfig = service.buildToastConfig(msg, duration, type);
      const addToastMessageAction = service.addToastMessageActionCreator(toastConfig);
      const showToastMessageAction = service.showToastMessageActionCreator(toastConfig.toastId);
      dispatch(addToastMessageAction);
      //slight delay to allow for prettier ui transition
      $timeout(() => dispatch(showToastMessageAction), 250);
    };

    service.dismissToastThunkActionCreator = toastId => dispatch => {
      const hideToastMessageAction = service.hideToastMessageActionCreator(toastId);
      const removeToastMessageAction = service.removeToastMessageActionCreator(toastId);
      dispatch(hideToastMessageAction);
      //slight delay to allow for prettier ui transition
      $timeout(() => dispatch(removeToastMessageAction), 250);
    };

    return service;
  },
]);
