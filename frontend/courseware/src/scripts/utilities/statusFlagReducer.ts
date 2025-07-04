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

export const statusFlagToggleActionCreatorMaker =
  (config = {}) =>
  status => ({
    ...config,
    type: 'STATUS_FLAG_TOGGLE',
    data: { status },
  });

const statusFlagReducer = (state: { status: boolean }, action) => {
  switch (action.type) {
    case 'STATUS_FLAG_TOGGLE':
      return {
        ...state,
        status:
          action.data && typeof action.data.status === 'boolean'
            ? action.data.status
            : !state.status,
      };
    default:
      return state;
  }
};

export const initiallyOffStatusFlagReducer = (
  state = {
    status: false,
  },
  action
) => statusFlagReducer(state, action);

export const initiallyOnStatusFlagReducer = (
  state = {
    status: true,
  },
  action
) => statusFlagReducer(state, action);

export const getStatusFlagReducer = (initial?: boolean) =>
  initial ? initiallyOnStatusFlagReducer : initiallyOffStatusFlagReducer;

export const statusValueSetterActionCreatorMaker =
  (config = {}) =>
  value => ({
    ...config,
    type: 'STATUS_VALUE_SET',
    data: { value },
  });

const statusValueReducer = (state, action) => {
  switch (action.type) {
    case 'STATUS_VALUE_SET':
      return {
        ...state,
        value: action.data.value,
      };
    default:
      return state;
  }
};

export const getStatusValueReducer =
  value =>
  (
    state = {
      value,
    },
    action
  ) =>
    statusValueReducer(state, action);
