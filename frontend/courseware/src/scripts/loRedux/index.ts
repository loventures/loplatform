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

import { merge } from 'lodash';
import { subcribeDeanToRedux } from '../analytics/dean';
import { preferencesToSettings } from '../utilities/preferencesToSettings';
import { useSelector } from 'react-redux';
import { AnyAction, applyMiddleware, compose, createStore } from 'redux';
import { enableBatching } from 'redux-batched-actions';

import middlewares from './middlewares';
import reducer from './reducer';
import enhancers from './reduxEnhancers';
import { createSubscription, getSavedState } from './reduxLocalStorage';

export type CourseState = ReturnType<typeof reducer>;

const getInitialState = () => ({
  ...getSavedState(),
  actualUser: window.lo_platform.user,
  course: window.lo_platform.course,
  actualUserRights: window.lo_platform.course_roles,
  preferences: window.lo_platform.preferences,
  settings: preferencesToSettings(window.lo_platform.preferences),
});

const finalReducer = (state: CourseState | undefined, action: AnyAction) => {
  if (action.type === 'TEST_ACTION_SET_STATE') {
    return reducer(merge({}, getInitialState(), action.newState), {
      type: '@@redux/INIT',
    });
  } else {
    return reducer(state, action);
  }
};

export const courseReduxStore = createStore(
  enableBatching(finalReducer),
  getInitialState(),
  compose(applyMiddleware(...middlewares), ...enhancers)
);

createSubscription(courseReduxStore);

subcribeDeanToRedux(courseReduxStore);

export const useCourseSelector = <A>(f: (s: CourseState, p?: any) => A): A =>
  useSelector<CourseState, A>(f);

export const useUiState = () => useCourseSelector(s => s.ui);
