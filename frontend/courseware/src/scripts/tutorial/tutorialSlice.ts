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

import * as tutorialApi from '../api/tutorialApi';
import { CourseState } from '../loRedux';
import * as actualUserSlice from '../loRedux/actualUserSlice';
import { trackTutorialViewEvent } from '../analytics/dean';
import { AnyAction, Dispatch, Reducer } from 'redux';
import { Selector, createSelector } from 'reselect';

export const play = (): AnyAction => {
  return { type: 'tutorialPlayer/play' };
};

export const manuallyPlay = (): AnyAction => {
  return { type: 'tutorialPlayer/manuallyPlay' };
};

export const stop = (): AnyAction => {
  return { type: 'tutorialPlayer/stop' };
};

export const end =
  (tutName: string, autoplay: boolean, step: number, allStepsViewed: boolean) =>
  async (dispatch: Dispatch, getState: () => CourseState): Promise<void> => {
    const state = getState();
    const tutorialUserInfos = actualUserSlice.selectTutorials(state);
    const complete = tutorialUserInfos[tutName]?.status === 'Complete';

    trackTutorialViewEvent(tutName, autoplay, step, allStepsViewed);

    if (!complete) {
      const nextTutInfos = await tutorialApi.setTutorialStatus(tutName, 'Complete');
      dispatch(actualUserSlice.setTutorials(nextTutInfos));
    }

    dispatch(stop());
  };

export const showManuallyPlayButton = (): AnyAction => {
  return { type: 'tutorialPlayer/showManuallyPlayButton' };
};

export const hideManuallyPlayButton = (): AnyAction => {
  return { type: 'tutorialPlayer/hideManuallyPlayButton' };
};

type PlayerState = {
  playing: boolean;
  manuallyPlaying: boolean;
  showManuallyPlayButton: boolean;
  showManuallyPlayGlow: boolean;
};

const initialState: PlayerState = {
  playing: false,
  manuallyPlaying: false,
  showManuallyPlayButton: false,
  showManuallyPlayGlow: false,
};

export const tutorialPlayerReducer: Reducer<PlayerState> = (state = initialState, action) => {
  switch (action.type) {
    case 'tutorialPlayer/play':
      return {
        ...state,
        playing: true,
      };
    case 'tutorialPlayer/manuallyPlay':
      return {
        ...state,
        playing: true,
        // we maintain manuallyPlaying, or set if we weren't playing already
        manuallyPlaying: state.manuallyPlaying || !state.playing,
      };
    case 'tutorialPlayer/stop':
      return {
        ...state,
        playing: false,
        manuallyPlaying: false,
        showManuallyPlayGlow: true,
      };
    case 'tutorialPlayer/showManuallyPlayButton':
      return {
        ...state,
        showManuallyPlayButton: true,
      };
    case 'tutorialPlayer/hideManuallyPlayButton':
      return {
        ...state,
        showManuallyPlayButton: false,
      };
    default:
      return state;
  }
};

export const selectEnabled: Selector<CourseState, boolean> = (state: CourseState): boolean =>
  (state.preferences as any).enableTutorials;

const selectTutorialPlayer: Selector<CourseState, PlayerState> = state => state.ui.tutorialPlayer;

export const selectShowManuallyPlay = createSelector(
  selectEnabled,
  selectTutorialPlayer,
  (enabled, tutorialPlayer) => enabled && tutorialPlayer.showManuallyPlayButton
);

export const selectShowManuallyPlayGlow = createSelector(
  selectEnabled,
  selectTutorialPlayer,
  (enabled, tutorialPlayer) => enabled && tutorialPlayer.showManuallyPlayGlow
);

export const selectManuallyPlaying = createSelector(
  selectTutorialPlayer,
  tutorialPlayer => tutorialPlayer.manuallyPlaying
);
