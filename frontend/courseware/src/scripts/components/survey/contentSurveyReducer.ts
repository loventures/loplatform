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

import { fetchSurvey } from '../../api/surveyApi';
import { Asset } from '../../authoring';
import SurveyQuestion from '../../components/survey/SurveyQuestion';
import { createLoadableKeyedReducer } from '../../loRedux/createLoadableKeyedReducer';

export interface Survey {
  title: string;
  questions: Asset<SurveyQuestion>[];
  submitted?: boolean;
  disabled: boolean;
  inline: boolean;
  programmatic: boolean;
}

export const {
  fetchKeyedAction: fetchKeyedSurveyAction,
  modifyAction: modifySurveyAction,
  reducer: surveyReducer,
} = createLoadableKeyedReducer<typeof fetchSurvey, Survey, 'submitted'>(
  'ContentSurvey',
  fetchSurvey
);
