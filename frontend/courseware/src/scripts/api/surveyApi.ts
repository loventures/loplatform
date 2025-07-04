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

import axios, { AxiosError } from 'axios';
import { Survey } from '../components/survey/contentSurveyReducer';

export function fetchSurvey<C extends { id: string }>(
  sectionId: number,
  content: C
): Promise<Survey> {
  // loConfig.contentItem.survey
  return axios
    .get(`/api/v2/lwc/${sectionId}/contents/${content.id}/survey`)
    .then(({ data: survey }) => survey)
    .catch((error: AxiosError) => {
      if (error && error.response && error.response.status !== 404) {
        throw error;
      }
    });
}

export interface SurveyQuestionResponse {
  questionAssetId: string;
  response: string;
}

export interface SurveyResponse {
  responses: SurveyQuestionResponse[];
}

export function postSurveyResponse(
  sectionId: number,
  contentId: string,
  responses: SurveyResponse
): Promise<void> {
  // loConfig.contentItem.survey
  return axios
    .post(`/api/v2/lwc/${sectionId}/contents/${contentId}/survey`, responses)
    .then(_resp => {});
}
