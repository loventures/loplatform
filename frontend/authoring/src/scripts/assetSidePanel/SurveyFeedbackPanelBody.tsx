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

import classNames from 'classnames';
import * as React from 'react';
import { useEffect, useState } from 'react';
import { Alert, CardBody } from 'reactstrap';

import { Loadable } from '../authoringUi/index';
import { extractTitle } from '../editor/editorUtilities';
import { useIsAdded, useIsEdited } from '../graphEdit';
import { usePolyglot } from '../hooks';
import AuthoringApiService from '../services/AuthoringApiService';
import SurveyApiService from '../services/SurveyApiService';
import { AssetNode } from '../types/asset';
import { isEssay, QuestionStat, SurveyQuestionFeedback } from './SurveyFeedbackQuestions';

interface ErrorResponse {
  status: number;
  message: {
    type: string;
    messages: {
      message: string;
      trace: string[];
    };
  };
}

function isError(err: ErrorResponse | null): err is ErrorResponse {
  return err !== null;
}

function isEmpty(arr: any[]): arr is [] {
  return arr.length === 0;
}

export const SurveyFeedbackComponent: React.FC<{
  node: string;
  surveyAssetName?: string;
  narrative?: boolean;
}> = ({ node, surveyAssetName, narrative }) => {
  const polyglot = usePolyglot();
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<null | ErrorResponse>(null);
  const [questionStats, setQuestionStats] = useState<QuestionStat[]>([]);
  const [extraResponseStats, setExtraResponseStats] = useState<QuestionStat[]>([]);
  const [sectionIds, setSectionIds] = useState<number[]>([]);
  const [edgePaths, setEdgePaths] = useState<string[]>([]);
  const [authoredQuestions, setAuthoredQuestions] = useState<
    {
      questionName: string;
      position: number;
      question: AssetNode;
    }[]
  >([]);
  const isNewAsset = useIsAdded(node);
  const isNewSurvey = useIsAdded(surveyAssetName);
  const isDirtySurvey = useIsEdited(surveyAssetName);

  const fetchAdditionalEssayResponses = (questionName: string, offset: number) => {
    return SurveyApiService.getAdditionalEssayRepsonses(
      questionName,
      sectionIds,
      edgePaths,
      offset
    ).then(res => {
      const newStats = questionStats.map(s => {
        if (s.questionName === questionName && isEssay(s)) {
          s.responses.push(...res);
        }
        return s;
      });
      setQuestionStats(newStats);
      return res;
    });
  };

  useEffect(() => {
    if (surveyAssetName && !isNewAsset && !isNewSurvey && !isDirtySurvey) {
      setLoading(true);
      SurveyApiService.getSurveyStats(node)
        .then(res => {
          setQuestionStats(res.questionStats);
          setSectionIds(res.sectionIds);
          setEdgePaths(res.edgePaths);
          return AuthoringApiService.fetchAssetAndIncludes(surveyAssetName, 'survey.1').then(
            ({ includes }) => {
              const questionEdges = includes.questions.sort((a, b) => (a.position = b.position));
              const orderedQuestions = questionEdges.map(e => {
                return {
                  questionName: e.target.name,
                  position: e.position,
                  question: e.target,
                };
              });
              const authoredQuestionNames = orderedQuestions.map(q => q.questionName);
              const extraResponses = res.questionStats.filter(
                stat => !authoredQuestionNames.includes(stat.questionName)
              );
              setAuthoredQuestions(orderedQuestions);
              setExtraResponseStats(extraResponses);
            }
          );
        })
        .catch(err => {
          setError(err);
        })
        .finally(() => setLoading(false));
    }
  }, [node, surveyAssetName, isNewAsset, isNewSurvey, isDirtySurvey]);

  return (
    <Loadable loading={loading}>
      {() => (
        <>
          {isError(error) ? (
            <Alert color="danger">
              <h5>{polyglot.t('ERROR_PAGE_TITLE')}</h5>
              <pre style={{ overflow: 'scroll' }}>{JSON.stringify(error?.message?.messages)}</pre>
              <pre>
                {JSON.stringify(
                  error?.message?.messages?.trace?.filter(s => s.startsWith('Caused')),
                  undefined,
                  2
                )}
              </pre>
            </Alert>
          ) : !surveyAssetName ? (
            <div className="d-flex align-items-center justify-content-center h-100">
              <div className="jumbotron">
                <CardBody className="text-center">
                  <span style={{ color: '#343a40' }}>No Survey Configured</span>
                </CardBody>
              </div>
            </div>
          ) : isNewAsset || isNewSurvey || isDirtySurvey ? (
            <div className="d-flex align-items-center justify-content-center h-100">
              <div className="jumbotron">
                <CardBody className="text-center">
                  <span style={{ color: '#343a40' }}>Save this survey to see the responses.</span>
                </CardBody>
              </div>
            </div>
          ) : !narrative && isEmpty(questionStats) ? (
            <div className="d-flex align-items-center justify-content-center h-100">
              <div className="jumbotron">
                <CardBody className="text-center">
                  <span style={{ color: '#343a40' }}>{polyglot.t('SURVEY_NO_RESPONSES')}</span>
                </CardBody>
              </div>
            </div>
          ) : (
            <>
              {authoredQuestions.map(({ questionName, question }, idx) => {
                const stats = questionStats.find(s => s.questionName === questionName);
                return (
                  <div
                    className={classNames(narrative ? 'survey-response' : 'mt-3', !idx && 'first')}
                    key={idx}
                  >
                    <div
                      className="mb-2"
                      style={{
                        display: 'grid',
                        gridTemplateColumns: 'auto 1fr auto',
                      }}
                    >
                      <span className="pe-2">{idx + 1}.</span>
                      <span style={{ justifySelf: 'start' }}>{extractTitle(question)}</span>
                      <span
                        className="text-muted"
                        style={{ justifySelf: 'end' }}
                      >
                        ({polyglot.t(question.typeId)})
                      </span>
                    </div>
                    <div className={narrative ? 'ms-3' : 'ms-3 mb-5'}>
                      {stats ? (
                        <SurveyQuestionFeedback
                          stats={stats}
                          question={question}
                          fetchEssays={fetchAdditionalEssayResponses}
                        />
                      ) : (
                        <table className="table table-sm mb-0">
                          <thead>
                            <tr>
                              <th>{polyglot.t('Rating_Scale_Rating_Label')}</th>
                              <th>{polyglot.t('Rating_Scale_Responses_Label')}</th>
                            </tr>
                          </thead>
                          <tbody>
                            <tr>
                              <td colSpan={2}>
                                <span className="text-muted ms-2">No Responses</span>
                              </td>
                            </tr>
                          </tbody>
                        </table>
                      )}
                    </div>
                  </div>
                );
              })}
              {!isEmpty(extraResponseStats) && (
                <div className="mb-5">
                  {polyglot.t('SURVEY_EXTRA_RESPONSES', {
                    smart_count: extraResponseStats.length,
                  })}
                </div>
              )}
            </>
          )}
        </>
      )}
    </Loadable>
  );
};
