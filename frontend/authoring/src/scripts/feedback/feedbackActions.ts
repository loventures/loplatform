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

import { NodeName } from '../types/asset';
import { Thunk } from '../types/dcmState';
import { FeedbackDto, FeedbackProfileDto } from './FeedbackApi';
import { AssetCounts, FeedbackFilters } from './feedbackReducer';

export const SET_FEEDBACK_FILTERS = 'SET_FEEDBACK_FILTERS';

export const setFeedbackFilters = (filters: Partial<FeedbackFilters>) => ({
  type: SET_FEEDBACK_FILTERS,
  filters,
});

export const SET_FEEDBACK_OFFSET = 'SET_FEEDBACK_OFFSET';

export const setFeedbackOffset = (offset: number) => ({
  type: SET_FEEDBACK_OFFSET,
  offset,
});

export const TOGGLE_FEEDBACK_OPEN = 'TOGGLE_FEEDBACK_OPEN';

export const toggleFeedbackOpen = (open?: boolean, mode?: 'survey') => ({
  type: TOGGLE_FEEDBACK_OPEN,
  open,
  mode,
});

export const TOGGLE_FEEDBACK_ON = 'TOGGLE_FEEDBACK_ON';

export const toggleFeedbackOn = (on?: boolean) => ({
  type: TOGGLE_FEEDBACK_ON,
  on,
});

export const SET_FEEDBACK_ADD = 'SET_FEEDBACK_ADD';

export const setAddFeedbackForAsset = (path?: NodeName[], quote?: string, id?: string) => ({
  type: SET_FEEDBACK_ADD,
  path,
  quote,
  id,
});

export const REFRESH_FEEDBACK = 'REFRESH_FEEDBACK';

export const refreshFeedback = () => ({
  type: REFRESH_FEEDBACK,
});

export const FEEDBACK_LOADED = 'FEEDBACK_LOADED';

export const feedbackLoaded = (feedbacks: FeedbackDto[]) => ({
  type: FEEDBACK_LOADED,
  feedbacks,
});

export const FEEDBACK_DELETED = 'FEEDBACK_DELETED';

export const feedbackDeleted = (feedback: number) => ({
  type: FEEDBACK_DELETED,
  feedback,
});

export const FEEDBACK_ADDED = 'FEEDBACK_ADDED';

export const feedbackAdded = (feedback: number | undefined) => ({
  type: FEEDBACK_ADDED,
  feedback,
});

export const SET_HTML_FEEDBACK = 'SET_HTML_FEEDBACK';

export const setHtmlFeedback = (
  quote: string,
  x: number,
  y: number,
  path: string[],
  id?: string
) => ({
  type: SET_HTML_FEEDBACK,
  htmlFeedback: {
    quote,
    x,
    y,
    path,
    id,
  },
});

export const resetHtmlFeedback = (): Thunk => (dispatch, getState) => {
  if (getState().feedback.htmlFeedback)
    dispatch({
      type: SET_HTML_FEEDBACK,
    });
};

export const SET_FEEDBACK_ASSIGNEES = 'SET_FEEDBACK_ASSIGNEES';

export const setFeedbackAssignees = (assignees: FeedbackProfileDto[]) => ({
  type: SET_FEEDBACK_ASSIGNEES,
  assignees,
});

export const SET_FEEDBACK_COUNTS = 'SET_FEEDBACK_COUNTS';

// If the specified filters are now stale then don't update the counts.
export const setFeedbackCounts =
  (counts: AssetCounts, filters: FeedbackFilters): Thunk =>
  (dispatch, getState) => {
    if (filters === getState().feedback.filters)
      dispatch({
        type: SET_FEEDBACK_COUNTS,
        counts,
      });
  };

export const SET_FEEDBACK_TOTALS = 'SET_FEEDBACK_TOTALS';

export const setFeedbackTotals = (totals: AssetCounts) => ({
  type: SET_FEEDBACK_TOTALS,
  totals,
});

export const SET_FEEDBACK_SUCCESS = 'SET_FEEDBACK_SUCCESS';

export const setFeedbackSuccess = (success?: boolean) => ({
  type: SET_FEEDBACK_SUCCESS,
  success,
});

export const SET_FEEDBACK_STALE = 'SET_FEEDBACK_STALE';

export const setFeedbackStale = (stale: boolean) => ({
  type: SET_FEEDBACK_STALE,
  stale,
});

type UpdateAction = 'create' | 'update' | 'delete' | 'activity' | 'archive';

export const remoteFeedbackUpdate =
  (_action: UpdateAction, updater: number /*, _id: number | null*/): Thunk =>
  (dispatch, getState) => {
    const { user, feedback } = getState();
    if (updater === user.profile?.id) return; // ignore my own updates. TODO should be tab session id.
    // It is a lot of effort to do anything other than just a full
    // refresh, even for a state change, because filters may cause
    // things to disappear etc. If analysis shows one type of change
    // is most common, could plausibly optimize for that.
    if (!feedback.stale) dispatch(setFeedbackStale(true));
  };
