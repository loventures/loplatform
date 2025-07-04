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

import { INITIALIZE_DCM } from '../dcmStoreConstants';
import { User } from '../types/user';
import {
  RECEIVE_AUTHORING_PREFERENCES,
  RECEIVE_SESSION_DURATION,
  RECEIVE_USER_SESSION,
} from './userActions';

export interface UserState {
  duration?: number;
  profile?: User;
  session?: {
    user: null;
    sudoed: boolean;
  };
  roles?: string[];
  rights?: string[];
  preferences: UserPreferences;
}

export interface UserPreferences {
  sendAlertEmails: boolean;
  authoringPreferences: AuthoringPreferences;
}

export interface AuthoringPreferences {
  editModeDefault: boolean; // default to edit mode vs view mode
  autoPreview: boolean; // automatically open html preview
}

export const defaultUserState: UserState = {
  duration: 0,
  preferences: {
    sendAlertEmails: false,
    authoringPreferences: {
      editModeDefault: false,
      autoPreview: false,
    },
  },
};

type UserAction =
  | {
      type: typeof INITIALIZE_DCM;
      user: Partial<UserState>;
    }
  | {
      type: typeof RECEIVE_SESSION_DURATION;
      duration: number;
    }
  | {
      type: typeof RECEIVE_USER_SESSION;
      info: {
        id: number;
        user: User;
        sudoed: boolean;
      };
    }
  | {
      type: typeof RECEIVE_AUTHORING_PREFERENCES;
      authoringPreferences: AuthoringPreferences;
    };

const userReducer: Reducer<UserState, UserAction> = (state = defaultUserState, action) => {
  switch (action.type) {
    case INITIALIZE_DCM: {
      return {
        ...state,
        ...action.user,
      };
    }
    case RECEIVE_SESSION_DURATION:
      return {
        ...state,
        duration: action.duration,
      };
    case RECEIVE_USER_SESSION:
      return {
        ...state,
        profile: {
          ...action.info.user,
        },
        session: {
          ...action.info,
          user: null,
        },
        id: action.info.id,
      };
    case RECEIVE_AUTHORING_PREFERENCES:
      return {
        ...state,
        preferences: {
          ...state.preferences,
          authoringPreferences: action.authoringPreferences,
        },
      };
    default:
      return state;
  }
};

export default userReducer;
