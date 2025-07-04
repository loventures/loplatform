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

import { INITIALIZE_DCM } from '../dcmStoreConstants';
import { NodeName } from '../types/asset';
import * as feedbackActions from './feedbackActions';
import { FeedbackDto, FeedbackProfileDto, FeedbackStatusFilter } from './FeedbackApi';

export type FeedbackFilters = {
  assignee?: number | null; // should be 'Anyone' (currently undefined) | 'No one' (currently null) | number
  branch?: number;
  status: FeedbackStatusFilter;
  unit?: string;
  module?: string;
  refresh: number;
};

export type AssetCounts = Record<NodeName, number>;

// The state of the Html feedback buttonlet
export type HtmlFeedback = {
  quote: string; // the selected text
  x: number; // the selection mouse x
  y: number; // the selection mouse y
  path: NodeName[]; // the node path
  id?: string;
};

// The state of feedback currently being added
export type AddFeedback = {
  path: NodeName[];
  quote?: string;
  id?: string;
};

export interface FeedbackState {
  open: boolean;
  on: boolean;
  mode?: 'survey';
  assignees: FeedbackProfileDto[];
  addFeedback?: AddFeedback;
  htmlFeedback?: HtmlFeedback;
  filters: FeedbackFilters;
  offset: 0;
  feedbacks: Record<number, FeedbackDto | null>;
  counts: AssetCounts;
  totals: AssetCounts;
  justAdded?: number;
  stale: boolean;
  success?: boolean; // briefly true/false after a fetch to debounce refetches
}

const initialState: FeedbackState = {
  open: false,
  on: true,
  assignees: [],
  filters: {
    refresh: 0,
    status: 'Open',
  },
  offset: 0,
  feedbacks: {},
  counts: {},
  totals: {},
  stale: false,
  success: undefined,
};

export default function feedbackReducer(
  state: FeedbackState = initialState,
  action
): FeedbackState {
  switch (action.type) {
    case INITIALIZE_DCM: {
      return initialState;
    }

    case feedbackActions.SET_FEEDBACK_FILTERS: {
      const { filters } = action;
      return {
        ...state,
        filters: { ...state.filters, ...filters },
      };
    }

    case feedbackActions.SET_FEEDBACK_OFFSET: {
      const { offset } = action;
      return {
        ...state,
        offset,
      };
    }

    case feedbackActions.TOGGLE_FEEDBACK_OPEN: {
      const open = action.open ?? !state.open;
      const mode = action.mode;
      const addFeedback = open && !mode ? state.addFeedback : undefined;
      return {
        ...state,
        open,
        mode,
        addFeedback,
      };
    }

    case feedbackActions.TOGGLE_FEEDBACK_ON: {
      const on = action.on ?? !state.on;
      return {
        ...state,
        on,
      };
    }

    case feedbackActions.SET_FEEDBACK_ADD: {
      const { path, quote, id } = action;
      return path
        ? {
            ...state,
            addFeedback: {
              path,
              quote,
              id,
            },
            htmlFeedback: undefined,
          }
        : {
            ...state,
            addFeedback: undefined,
          };
    }

    case feedbackActions.SET_HTML_FEEDBACK: {
      const { htmlFeedback } = action;
      return state.addFeedback
        ? state
        : {
            ...state,
            htmlFeedback,
          };
    }

    case feedbackActions.REFRESH_FEEDBACK: {
      return {
        ...state,
        filters: {
          ...state.filters,
          refresh: 1 + state.filters.refresh,
        },
      };
    }

    case feedbackActions.FEEDBACK_LOADED: {
      const feedbacks = { ...state.feedbacks };
      for (const feedback of action.feedbacks) {
        feedbacks[feedback.id] = feedback;
      }
      return {
        ...state,
        feedbacks,
      };
    }

    case feedbackActions.FEEDBACK_DELETED: {
      const feedbacks = { ...state.feedbacks };
      feedbacks[action.feedback] = null;
      return {
        ...state,
        feedbacks,
      };
    }

    case feedbackActions.FEEDBACK_ADDED: {
      return {
        ...state,
        justAdded: action.feedback,
      };
    }

    case feedbackActions.SET_FEEDBACK_ASSIGNEES: {
      const { assignees } = action;
      return {
        ...state,
        assignees,
      };
    }

    case feedbackActions.SET_FEEDBACK_COUNTS: {
      const { counts } = action;
      return {
        ...state,
        counts,
      };
    }

    case feedbackActions.SET_FEEDBACK_TOTALS: {
      const { totals } = action;
      return {
        ...state,
        totals,
      };
    }

    case feedbackActions.SET_FEEDBACK_SUCCESS: {
      const { success } = action;
      return {
        ...state,
        success,
      };
    }

    case feedbackActions.SET_FEEDBACK_STALE: {
      const { stale } = action;
      return {
        ...state,
        stale,
      };
    }

    default:
      return state;
  }
}
