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

import { UserInfo } from '../../loPlatform';
import { TutorialInfo } from '../api/tutorialApi';
import { selectActualUser } from '../utilities/rootSelectors';
import { AnyAction, Reducer } from 'redux';
import { createSelector } from 'reselect';

export const setTutorials = (infos: Record<string, TutorialInfo>): AnyAction => {
  return {
    type: 'actualUser/setTutorials',
    payload: infos,
  };
};

export const actualUserReducer: Reducer<UserInfo> = (
  state = window.lo_platform.user,
  action
) => {
  switch (action.type) {
    case 'actualUser/setTutorials':
      return {
        ...state,
        tutorials: action.payload,
      };
    default:
      return state;
  }
};

export const selectTutorials = createSelector(selectActualUser, user => user.tutorials);
