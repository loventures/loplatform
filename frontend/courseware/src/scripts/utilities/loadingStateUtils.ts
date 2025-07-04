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

import { flatten, isFunction, map } from 'lodash';
import { AnyAction, Dispatch, Reducer } from 'redux';
import { batchActions } from 'redux-batched-actions';

export const LOADING_RESET_ACTION = 'LOADING_RESET_ACTION';
export const LOADING_START_ACTION = 'LOADING_START_ACTION';
export const LOADING_SUCCESS_ACTION = 'LOADING_SUCCESS_ACTION';
export const LOADING_ERROR_ACTION = 'LOADING_ERROR_ACTION';

export const loadingResetActionCreatorMaker = (config: Record<string, any>) => {
  return (moreConfigs = {}) => ({
    type: LOADING_RESET_ACTION,
    ...config,
    ...moreConfigs,
  });
};

export const loadingStartActionCreatorMaker = (config: Record<string, any>) => {
  return (moreConfigs = {}) => ({
    type: LOADING_START_ACTION,
    ...config,
    ...moreConfigs,
  });
};

export const loadingSuccessActionCreatorMaker = (config: Record<string, any>) => {
  return (moreConfigs = {}) => ({
    type: LOADING_SUCCESS_ACTION,
    ...config,
    ...moreConfigs,
  });
};

export const loadingErrorActionCreatorMaker = (config: Record<string, any>) => {
  return (moreConfigs = {}, error: any) => ({
    type: LOADING_ERROR_ACTION,
    ...config,
    ...moreConfigs,
    data: { error: error || 'GENERIC_LOADING_ERROR' },
  });
};

/**
 loadingActionCreatorMaker reduces the need to create boilerplate actions around
 keeping track of the ui state as an async API is fired.

 @param configForACM {object}
 Configuration you want to use with the generic loading start,success,error actionCreatorMakers
 @param loader {function}
 Async API call that you wish to keep track the loading state of
 @param additionalSuccessACs {array} optional
 A list of action creators that you wish to dispatch upon success of the loader function.
 These action creators will be passed the result of the loader function before being dispatched.
 @param getConfigForAC {function} optional
 You can further customize the generic loading start,success,error actions by passing
 in a function that will generate config based on the inputs to the loadingActionCreator.
 @param additionalFailureACS {array} optional
 A list of additional failure action creators.

 @return {function}
 Returns a loadingActionCreator.  Whatever argurments passed to this creator will be
 used in the loader function and as well as the getConfigForAC if present

 Example:
 const activityLoader = someId => $q.when({someResult});
 const updateActivityAC = someResult => {sliceName: 'activity', activity:someResult };
 const loadingActivityAC = loadingActionCreatorMaker(
 { sliceName: 'activityLoadingState' },
 activityLoader,
 [ updateActivityAC ],
 (activity) => ({ id: activity.id })
 );
 dispatch(loadingActivityAC(paramsForLoader));

 //Reducers watching 'activityLoadingState' will get updates on loading state for the api call
 //Reducers watching 'activity' will get the result of the api call
 //See loadingStateReducer below for an example of how loading actions are handled
 */
type SuccessACs = Record<string, any> | ((r: any) => Record<string, any>);

export const loadingActionCreatorMaker = (
  configForACM: Record<string, any>,
  loader: (...args: any[]) => Promise<any>,
  additionalSuccessACs: SuccessACs[] = [],
  getConfigForAC?: (...args: any[]) => Record<string, any>,
  additionalFailureACS = []
) => {
  const startAC = loadingStartActionCreatorMaker(configForACM);
  const successAC = loadingSuccessActionCreatorMaker(configForACM);
  const errorAC = loadingErrorActionCreatorMaker(configForACM);

  return (...args: any[]) => {
    return (dispatch: Dispatch) => {
      const configForAC = isFunction(getConfigForAC) ? getConfigForAC(...args) : {};
      dispatch(startAC(configForAC));
      loader(...args).then(
        result => {
          const actions = flatten(
            map(additionalSuccessACs, actionOrCreator => {
              if (isFunction(actionOrCreator)) {
                return actionOrCreator(result);
              } else {
                return actionOrCreator;
              }
            })
          );
          dispatch(batchActions([...actions, successAC(configForAC)]));
        },
        error =>
          dispatch(
            additionalFailureACS && additionalFailureACS.length
              ? batchActions([...additionalFailureACS, errorAC(configForAC, error)])
              : errorAC(configForAC, error)
          )
      );
    };
  };
};

export type LoadingState = {
  loading?: boolean;
  loaded?: boolean;
  error?: any;
};

type LoadingAction = AnyAction & { data?: any };

export const loadingStateReducer: Reducer<LoadingState, LoadingAction> = function (
  state = {
    loading: false,
    loaded: false,
    error: null,
  },
  { type, data }
) {
  switch (type) {
    case LOADING_RESET_ACTION:
      return {
        loading: false,
        loaded: false,
        error: null,
      };
    case LOADING_START_ACTION:
      return {
        ...state,
        loading: true,
        loaded: false,
        error: null,
      };
    case LOADING_SUCCESS_ACTION:
      return {
        ...state,
        loading: false,
        loaded: true,
        error: null,
      };
    case LOADING_ERROR_ACTION:
      return {
        ...state,
        loading: false,
        loaded: false,
        error: data.error,
      };
    default:
      return state;
  }
};
