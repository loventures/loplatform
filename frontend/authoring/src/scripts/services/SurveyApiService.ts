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

import { QuestionStat } from '../assetSidePanel/SurveyFeedbackQuestions';
import { NodeName } from '../types/asset';
import { getBranchId } from '../router/ReactRouterService';

export interface SurveyResponseStats {
  sectionIds: number[];
  responseStats: Record<NodeName, number>;
}

export interface SurveyQuestionStats {
  sectionIds: number[];
  edgePaths: string[];
  questionStats: QuestionStat[];
}

interface SurveyRequestParams {
  /** questionId is a stringified object:
   *   {
   *     questionName: string;
   *     sectionIds: number[];
   *     edgePaths: string[];
   *   }
   **/
  questionId: string;
  offset: number;
  limit: number;
}

class SurveyApiService {
  private previousRequestAbort: AbortController;

  public getResponseStats(): Promise<SurveyResponseStats> {
    return gretchen
      .get('/api/v2/authoring/:branchId/survey/stats')
      .params({
        branchId: getBranchId(),
      })
      .makeCancellable(this.getAbortController())
      .exec();
  }

  public getSurveyStats(nodeName: string): Promise<SurveyQuestionStats> {
    return gretchen
      .get('/api/v2/authoring/:branchId/nodes/:nodeName/survey/stats')
      .params({
        branchId: getBranchId(),
        nodeName,
      })
      .makeCancellable(this.getAbortController())
      .exec();
  }

  public getAdditionalEssayRepsonses(
    questionName: string,
    sectionIds: number[],
    edgePaths: string[],
    offset: number
  ): Promise<string[]> {
    const params: SurveyRequestParams = {
      questionId: JSON.stringify({
        questionName,
        sectionIds,
        edgePaths,
      }),
      offset,
      limit: 10,
    };
    return gretchen.get('/api/v2/authoring/surveyEssayResponses').params(params).exec();
  }

  ////

  private getAbortController() {
    this.previousRequestAbort && this.previousRequestAbort.abort();
    this.previousRequestAbort = new AbortController();
    return this.previousRequestAbort;
  }
}

export default new SurveyApiService();
