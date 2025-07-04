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

import { SurveyResponseStats } from '../services/SurveyApiService';
import { LtiToolApiResponse } from '../types/lti';
import { SET_LTI_TOOLS, SET_PROJECTS, SET_SURVEY_STATS } from './dataActions';
import { ProjectResponse } from './NarrativeMultiverse';

export interface DataState {
  ltiTools?: LtiToolApiResponse[];
  projects?: ProjectResponse[];
  surveyStats?: SurveyResponseStats;
}

const initialState: DataState = {};

export default function dataReducer(state: DataState = initialState, action): DataState {
  switch (action.type) {
    case SET_LTI_TOOLS: {
      const { ltiTools } = action;
      return {
        ...state,
        ltiTools,
      };
    }

    case SET_PROJECTS: {
      const { projects } = action;
      return {
        ...state,
        projects,
      };
    }

    case SET_SURVEY_STATS: {
      const { surveyStats } = action;
      return {
        ...state,
        surveyStats,
      };
    }

    default:
      return state;
  }
}
