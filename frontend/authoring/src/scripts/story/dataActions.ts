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

import gretchen from '../grfetchen/';
import { useDispatch } from 'react-redux';

import { useDcmSelector } from '../hooks';
import SurveyApiService, { SurveyResponseStats } from '../services/SurveyApiService';
import { Thunk } from '../types/dcmState';
import { LtiToolApiResponse } from '../types/lti';
import { ProjectResponse, ProjectsResponse } from './NarrativeMultiverse';

export const SET_LTI_TOOLS = 'SET_LTI_TOOLS';

const NoTools = [];

const fetchLtiTools = (): Promise<LtiToolApiResponse[]> =>
  gretchen
    .get('/api/v2/ltiTools')
    .exec()
    .then(res => res.objects);

const setLtiTools = (ltiTools: LtiToolApiResponse[]) => ({
  type: SET_LTI_TOOLS,
  ltiTools,
});

// This should be useResource but we're not that advanced in DCM
export const useLtiTools = (): LtiToolApiResponse[] => {
  const dispatch = useDispatch();
  const ltiTools = useDcmSelector(state => state.data.ltiTools);
  if (ltiTools != null) return ltiTools;
  dispatch(setLtiTools(NoTools));
  fetchLtiTools().then(res => {
    dispatch(setLtiTools(res.sort((a, b) => a.name.localeCompare(b.name))));
  });
  return NoTools;
};

export const SET_PROJECTS = 'SET_PROJECTS';

export const NoProjects = [];

export const fetchProjects = (archived = false): Promise<ProjectsResponse> =>
  gretchen.get(`/api/v2/authoring/projects${!archived ? '?archived=false' : ''}`).exec();

export const setProjects = (projects: ProjectResponse[]) => ({
  type: SET_PROJECTS,
  projects,
});

export const reloadProjects = (): Thunk => dispatch => {
  fetchProjects().then(({ projects, users }) => {
    for (const project of projects) {
      project.headCreatedByUser = users[project.headCreatedBy];
    }
    dispatch(setProjects(projects.sort((a, b) => a.branchName.localeCompare(b.branchName))));
  });
};

// The back-end filters projects by your rights.
export const useProjects = (): ProjectResponse[] => {
  const dispatch = useDispatch();
  const projects = useDcmSelector(state => state.data.projects);
  if (projects != null) return projects;
  dispatch(setProjects(NoProjects));
  dispatch(reloadProjects());
  return NoProjects;
};

export const SET_SURVEY_STATS = 'SET_SURVEY_STATS';

const NoSurveyStats: SurveyResponseStats = { sectionIds: [], responseStats: {} };

const setSurveyStats = (surveyStats: SurveyResponseStats) => ({
  type: SET_SURVEY_STATS,
  surveyStats,
});

// This should be useResource but we're not that advanced in DCM
export const useSurveyStats = (): SurveyResponseStats => {
  const dispatch = useDispatch();
  const surveyStats = useDcmSelector(state => state.data.surveyStats);
  if (surveyStats != null) return surveyStats;
  dispatch(setSurveyStats(NoSurveyStats));
  SurveyApiService.getResponseStats().then(surveyStats => dispatch(setSurveyStats(surveyStats)));
  return NoSurveyStats;
};
