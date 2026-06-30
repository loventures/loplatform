/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

import { shuffle } from 'lodash';
import React, { useMemo } from 'react';

import { HtmlWithMathJax } from '../../../components/HtmlWithMathjax';
import { choiceOrdinal } from '../../../filters/choiceOrdinalFilter.js';
import { useTranslation } from '../../../i18n/translationContext.tsx';
import { PrintQuestionDistractorRow } from '../../questionTemplates/PrintQuestionDistractorRow.tsx';
import { PrintQuestionTemplate } from '../../questionTemplates/PrintQuestionTemplate.tsx';
import { getChoiceText, getDisplayAnswer, getDisplayResponse, responseToSelection } from './orderingChoices.ts';

export interface OrderingQuestionPrintViewProps {
  index: number;
  assessment?: { settings?: { isCheckpoint?: boolean } };
  questionCount?: number;
  question: any;
  response?: any;
}

const Row: React.FC<{ choice: any; index: number; correct: boolean; hasCorrectness: boolean }> = ({
  choice,
  index,
  correct,
  hasCorrectness,
}) => (
  <dd>
    <PrintQuestionDistractorRow
      choice={{ correct }}
      hasCorrectness={hasCorrectness}
      isCorrect={correct}
    >
      <div className="p-2 border">
        <div className="float-left">
          <span>{choiceOrdinal(index)}.</span>
        </div>
        <div className="ms-4 choice-text">
          <HtmlWithMathJax html={getChoiceText(choice)} />
        </div>
      </div>
    </PrintQuestionDistractorRow>
  </dd>
);

/**
 * React port of the learner `orderingQuestionPrintView` (B2-quiz print). Read-only: the learner's
 * ordering ("MY ANSWER"), then — when correctness is shown — the correct ordering ("CORRECT ANSWER").
 * Each row's correctness mirrors the Angular template: `question.correctOrder[index] === index`. DOM
 * preserved from orderingQuestionPrintView.html (`.question.choice-question.ordering-question`, the two
 * `dl`s, `.choice-text`). No Selenide print coverage for ordering — verified via vitest + build.
 */
export const OrderingQuestionPrintView: React.FC<OrderingQuestionPrintViewProps> = ({
  index,
  assessment,
  questionCount,
  question,
  response,
}) => {
  const translate = useTranslation();
  const correctAnswer = !!question.displayDetail?.correctAnswer;

  // Mirror the Angular controller's $onInit + $onChanges.
  const displayChoices = useMemo<any[]>(() => {
    let dc: any[] = getDisplayResponse(question, response);
    if (!response && !correctAnswer) dc = shuffle(dc);
    if (response) dc = responseToSelection(question, response, dc as any);
    return dc;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [question, response]);
  const correctChoices = useMemo(() => getDisplayAnswer(question), [question]);

  const isCorrect = ($index: number) => question.correctOrder?.[$index] === $index;

  return (
    <PrintQuestionTemplate
      className="question choice-question ordering-question"
      index={index}
      assessment={assessment}
      questionCount={questionCount}
      question={question}
      response={response}
    >
      <dl>
        <dt>{response && correctAnswer && <span>{translate('MY_ANSWER')}</span>}</dt>
        {displayChoices.map((choice: any, $index: number) => (
          <Row
            key={$index}
            choice={choice}
            index={$index}
            correct={isCorrect($index)}
            hasCorrectness={correctAnswer}
          />
        ))}
      </dl>

      {response && correctAnswer && (
        <dl>
          <dt>{translate('CORRECT_ANSWER')}</dt>
          {correctChoices.map((choice: any, $index: number) => (
            <Row
              key={$index}
              choice={choice}
              index={$index}
              correct={isCorrect($index)}
              hasCorrectness={correctAnswer}
            />
          ))}
        </dl>
      )}
    </PrintQuestionTemplate>
  );
};
