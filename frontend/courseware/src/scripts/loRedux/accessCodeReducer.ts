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

import { AnyAction, Reducer } from 'redux';
import { createAction, isActionOf } from 'typesafe-actions';

type Status = 'Loading' | 'Loaded' | 'Empty' | 'Error';

export type AccessCodeState = {
  status: Status | undefined;
  accessCode: string | undefined;
};

const initialState: AccessCodeState = {
  status: undefined,
  accessCode: undefined,
};

export const updateAccessCodeState = createAction('UPDATE_ACCESS_CODE_STATE')<
  Partial<AccessCodeState>
>();

const accessCodeReducer: Reducer<AccessCodeState, AnyAction> = (state = initialState, action) => {
  if (isActionOf(updateAccessCodeState, action)) {
    return {
      ...state,
      ...action.payload,
    };
  } else {
    return state;
  }
};

export default accessCodeReducer;
