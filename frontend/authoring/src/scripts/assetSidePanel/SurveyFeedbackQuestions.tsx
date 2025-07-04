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

import { keyBy, mapValues, omit, range } from 'lodash';
import React, { ReactNode, useState } from 'react';
import CSSTransition from 'react-transition-group/CSSTransition';
import TransitionGroup from 'react-transition-group/TransitionGroup';
import { Card } from 'reactstrap';

import { usePolyglot } from '../hooks';
import type { Asset, AssetNode, HtmlPart } from '../types/asset';
import * as T from '../types/typeIds';

type QuestionStat = ChoiceQuestionStat | EssayQuestionStat;

interface ChoiceQuestionStat {
  questionName: string;
  choiceCounts: ChoiceCounts[];
}

interface ChoiceCounts {
  choice: string;
  count: number;
}

export interface EssayQuestionStat {
  questionName: string;
  responseCount: number;
  responses: string[];
}

function isChoice(stat: QuestionStat): stat is ChoiceQuestionStat {
  return 'choiceCounts' in stat;
}

function isEssay(stat: QuestionStat): stat is EssayQuestionStat {
  return 'responseCount' in stat;
}

function isLikert(question: AssetNode): question is Asset<T.LikertQuestion> {
  return question.typeId === T.LikertQuestion;
}

function isRating(question: AssetNode): question is Asset<T.RatingQuestion> {
  return question.typeId === T.RatingQuestion;
}

const percentDisplay = (numerator: number, denomintor: number): string => {
  if (denomintor > 0) {
    return `${Math.round((numerator / denomintor) * 100)}%`;
  } else {
    return '__%';
  }
};

const RatingQuestionChoicesDisplay: React.FC<{
  stats: ChoiceQuestionStat;
  question: Asset<'ratingScaleQuestion.1'>;
}> = ({ stats, question }) => {
  const allPossibleChoices = range(0, question.data.max).map(a => a.toString());
  const mappedChoices = stats.choiceCounts.reduce<{ [choice: string]: number }>((acc, c) => {
    acc[c.choice] = c.count;
    return acc;
  }, {});
  const choices = allPossibleChoices.reduce<{ [choice: string]: number }>((acc, rating) => {
    acc[rating] = mappedChoices[rating] || 0;
    return acc;
  }, {});
  return (
    <SurveyStatisticsTable
      choices={choices}
      ratingLabelKey="Rating_Scale_Rating_Label"
      responseLableKey="Rating_Scale_Responses_Label"
    >
      {(choice, index) => (
        <>
          {choice}
          {index === 0 && (
            <span className="text-muted">&nbsp;&mdash;&nbsp;{question.data.lowRatingText}</span>
          )}
          {index + 1 === allPossibleChoices.length && (
            <span className="text-muted">&nbsp;&mdash;&nbsp;{question.data.highRatingText}</span>
          )}
        </>
      )}
    </SurveyStatisticsTable>
  );
};

const LikertQuestionChoices: React.FC<{ stats: ChoiceQuestionStat }> = ({ stats }) => {
  const polyglot = usePolyglot();
  const mappedChoices = keyBy(stats.choiceCounts, s => s.choice);
  const choices = {
    LIKERT_STRONGLY_AGREE: mappedChoices['0']?.count ?? 0,
    LIKERT_AGREE: mappedChoices['1']?.count ?? 0,
    LIKERT_NEITHER: mappedChoices['2']?.count ?? 0,
    LIKERT_DISAGREE: mappedChoices['3']?.count ?? 0,
    LIKERT_STRONGLY_DISAGREE: mappedChoices['4']?.count ?? 0,
  };
  const extraChoices = omit(mappedChoices, ['0', '1', '2', '3', '4']);
  Object.values(extraChoices).forEach(s => {
    choices[`Unknown - ${s.choice}`] = s.count;
  });
  return (
    <SurveyStatisticsTable
      choices={choices}
      ratingLabelKey="Likert_Scale_Rating_Label"
      responseLableKey="Likert_Scale_Responses_Label"
    >
      {choice => polyglot.t(choice)}
    </SurveyStatisticsTable>
  );
};

const MultipleChoiceFeedbackDisplay: React.FC<{
  stats: ChoiceQuestionStat;
  question: Asset<'surveyChoiceQuestion.1'>;
}> = ({ stats, question }) => {
  const choices = question.data.choices.reduce<{
    [value: string]: { label: HtmlPart; count: number };
  }>((acc, choice) => {
    acc[choice.value] = {
      label: choice.label,
      count: 0,
    };
    return acc;
  }, {});
  stats.choiceCounts.forEach(cc => {
    choices[cc.choice].count = cc.count;
  });
  return (
    <SurveyStatisticsTable
      choices={mapValues(choices, c => c.count)}
      ratingLabelKey="Multiple_Choice_Choice_Label"
      responseLableKey="Multiple_Choice_Responses_Label"
    >
      {value => {
        return (
          <span dangerouslySetInnerHTML={{ __html: choices[value].label.renderedHtml }}></span>
        );
      }}
    </SurveyStatisticsTable>
  );
};

interface SurveyStatisticsTableProps {
  choices: Record<string, number>;
  children: (c: string, i: number) => ReactNode;
  ratingLabelKey: string;
  responseLableKey: string;
}

const SurveyStatisticsTable: React.FC<SurveyStatisticsTableProps> = ({
  choices,
  children,
  ratingLabelKey = 'Rating_Scale_Rating_Label',
  responseLableKey = 'Rating_Scale_Responses_Label',
}) => {
  const polyglot = usePolyglot();
  const totalCount = Object.values(choices).reduce((a, b) => a + b, 0);
  return (
    <table className="table table-sm">
      <thead>
        <tr>
          <th>{polyglot.t(ratingLabelKey)}</th>
          <th>{polyglot.t(responseLableKey)}</th>
        </tr>
      </thead>
      <tbody>
        {Object.entries(choices).map(([choice, count], i) => (
          <tr key={i}>
            <td className="col-9">{children(choice, i)}</td>
            <td className="col-3">
              {percentDisplay(count, totalCount)}
              <span className="text-muted ms-2">
                &nbsp;({count}&nbsp;of&nbsp;{totalCount})
              </span>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
};

const EssayQuestionFeedback: React.FC<{
  stats: EssayQuestionStat;
  question: AssetNode;
  fetchEssays: (questionName: string, offset: number) => Promise<string[]>;
}> = ({ stats, question, fetchEssays }) => {
  const polyglot = usePolyglot();
  const [loadingMore, setLoadingMore] = useState<boolean>(false);
  const getMore = () => {
    setLoadingMore(true);
    fetchEssays(question.name, stats.responses.length)
      .then(() => setLoadingMore(false))
      .catch(() => setLoadingMore(false));
  };

  return (
    <div>
      <TransitionGroup>
        {stats.responses.map((response, i) => (
          <CSSTransition
            key={i + '2'}
            timeout={500}
            classNames="item"
          >
            <Card className="my-1">
              <div className="p-2">
                <span>{response}</span>
              </div>
            </Card>
          </CSSTransition>
        ))}
      </TransitionGroup>
      <div className="d-flex justify-content-between align-items-center ms-1 mt-2 ">
        <button
          onClick={getMore}
          disabled={loadingMore || stats.responseCount === stats.responses.length}
          className="btn btn-sm btn-outline-primary"
        >
          {polyglot.t('SURVEY_ESSAY_LOAD_MORE')}
        </button>
        <span className="text-muted">
          {polyglot.t('SURVEY_ESSAY_SHOWING_TOTAL', {
            count: stats.responses.length,
            totalCount: stats.responseCount,
          })}
        </span>
      </div>
    </div>
  );
};

const SurveyQuestionFeedback: React.FC<{
  stats: QuestionStat;
  question: AssetNode;
  fetchEssays: (q: string, o: number) => Promise<string[]>;
}> = ({ stats, question, fetchEssays }) => {
  if (isChoice(stats)) {
    if (isLikert(question)) {
      return <LikertQuestionChoices stats={stats} />;
    } else if (isRating(question)) {
      return (
        <RatingQuestionChoicesDisplay
          stats={stats}
          question={question}
        />
      );
    } else {
      return (
        <MultipleChoiceFeedbackDisplay
          stats={stats}
          question={question as Asset<'surveyChoiceQuestion.1'>}
        />
      );
    }
  } else {
    return (
      <EssayQuestionFeedback
        stats={stats}
        question={question}
        fetchEssays={fetchEssays}
      />
    );
  }
  return null;
};

export type { QuestionStat };
export { SurveyQuestionFeedback, isEssay };
