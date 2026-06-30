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

import React, { useEffect, useState } from 'react';

import { HtmlWithMathJax } from '../../../components/HtmlWithMathjax';
import { choiceOrdinal } from '../../../filters/choiceOrdinalFilter.js';
import { useTranslation } from '../../../i18n/translationContext.tsx';
import { SELECTION_TYPE_MULTIPLE_CHOICE } from '../../../utilities/questionTypes.js';
import { BasicQuestionTemplate } from '../../questionTemplates/BasicQuestionTemplate.tsx';
import { QuestionDistractorRow } from '../../questionTemplates/QuestionDistractorRow.tsx';

interface Choice {
  choiceText?: string;
  correct?: boolean;
  rationales?: any[];
}
interface Response {
  selection?: { selectedIndexes?: number[]; responseType?: string };
}
interface Question {
  choices?: Choice[];
  displayDetail?: { correctAnswer?: boolean };
}

export interface MultipleChoiceQuestionBaseViewProps {
  index: number;
  focusOnRender?: boolean;
  assessment?: { settings?: { isCheckpoint?: boolean } };
  questionCount?: number;
  question: Question;
  response?: Response;
  score?: any;
  changeAnswer: (index: number, response: any) => void;
  canEditAnswer?: boolean;
}

/**
 * React port of the learner `multipleChoiceQuestionBaseView` (B2-quiz) — also
 * serves true/false (same registry entry). Renders the React BasicQuestionTemplate
 * hub with the radio distractor list as its content. DOM preserved verbatim from
 * multipleChoiceQuestionBaseView.html so the cours2e MC/MS/TF selectors hold:
 * `.question-distractor-list > li.question-distractor-row`, `label.question-choice-content`
 * (with disabled/selected/correct/incorrect), `.choice-ordinal > .ordinal-number` +
 * the radio `input.form-check-input`, and `.choice-text`.
 *
 * Selection mirrors the Angular `ng-model`: local state seeded from the response
 * for instant feedback, re-synced when the response prop changes, and pushed up
 * through `changeAnswer` (which round-trips a new response via redux). The print
 * view stays Angular; the wrapper picks base-vs-print.
 */
export const MultipleChoiceQuestionBaseView: React.FC<MultipleChoiceQuestionBaseViewProps> = ({
  index,
  assessment,
  questionCount,
  question,
  response,
  changeAnswer,
  canEditAnswer,
}) => {
  const translate = useTranslation();

  const indexFromResponse = () =>
    response && response.selection ? (response.selection.selectedIndexes?.[0] ?? null) : null;
  const [selectedIndex, setSelectedIndex] = useState<number | null>(indexFromResponse());
  useEffect(() => setSelectedIndex(indexFromResponse()), [response]);

  const isCheckpoint = assessment?.settings?.isCheckpoint;

  const selectChoiceIndex = (choiceIndex: number) => {
    setSelectedIndex(choiceIndex);
    const resp: any = response || {};
    const selection = (response && response.selection) || { responseType: SELECTION_TYPE_MULTIPLE_CHOICE };
    changeAnswer(index, { ...resp, selection: { ...selection, selectedIndexes: [choiceIndex] } });
  };

  const hasCorrectness = (i: number, choice: Choice) =>
    !!question.displayDetail?.correctAnswer && (i === selectedIndex || !!choice.correct);

  const choiceOrdinalAriaLabel = (i: number, choice: Choice) => {
    let key: string;
    if (hasCorrectness(i, choice)) {
      key = choice.correct
        ? i === selectedIndex
          ? 'QUIZ_PLAYER_CHOICE_ORDINAL_LABEL_CORRECT'
          : 'QUIZ_PLAYER_CHOICE_ORDINAL_LABEL_CORRECT_NOT_SELECTED'
        : 'QUIZ_PLAYER_CHOICE_ORDINAL_LABEL_INCORRECT_SELECTED';
    } else {
      key = 'QUIZ_PLAYER_CHOICE_ORDINAL_LABEL';
    }
    return translate(key, { ordinal: choiceOrdinal(i) });
  };

  return (
    <BasicQuestionTemplate
      className="question choice-question multiple-choice-question"
      index={index}
      assessment={assessment}
      questionCount={questionCount}
      question={question as any}
      response={response as any}
    >
      <ol className="question-distractor-list">
        {(question.choices ?? []).map((choice, $index) => {
          const selected = response ? $index === selectedIndex : false;
          const correctness = hasCorrectness($index, choice);
          const labelClasses = [
            'question-choice-content',
            !canEditAnswer ? 'disabled' : '',
            $index === selectedIndex || (correctness && choice.correct) ? 'selected' : '',
            correctness && $index === selectedIndex && choice.correct ? 'correct' : '',
            correctness && $index === selectedIndex && !choice.correct ? 'incorrect' : '',
          ]
            .filter(Boolean)
            .join(' ');

          return (
            <li
              className="question-distractor-row"
              key={$index}
            >
              <QuestionDistractorRow
                choice={choice}
                hasCorrectness={correctness}
                isCorrect={choice.correct}
                isSelected={selected}
                isInstructor={!response}
                index={$index}
              >
                <label
                  className={labelClasses}
                  htmlFor={`question-${index}-choice-${$index}`}
                  data-id={!response ? `choice-${$index}` : undefined}
                >
                  <div className="choice-ordinal">
                    {!isCheckpoint && (
                      <div
                        id={`choice-ordinal-${index}-${$index}`}
                        className="ordinal-number"
                        aria-label={choiceOrdinalAriaLabel($index, choice)}
                        aria-hidden="true"
                      >
                        {choiceOrdinal($index)}
                      </div>
                    )}
                    <input
                      className="form-check-input"
                      id={`question-${index}-choice-${$index}`}
                      name={`choice-group-${index}`}
                      type="radio"
                      value={$index}
                      checked={$index === selectedIndex}
                      disabled={!canEditAnswer}
                      onChange={() => selectChoiceIndex($index)}
                      aria-labelledby={`choice-ordinal-${index}-${$index} choice-text-${index}-${$index}`}
                    />
                  </div>
                  <div
                    id={`choice-text-${index}-${$index}`}
                    className="choice-text"
                    aria-hidden="true"
                  >
                    <HtmlWithMathJax html={choice.choiceText} />
                  </div>
                </label>
              </QuestionDistractorRow>
            </li>
          );
        })}
      </ol>
    </BasicQuestionTemplate>
  );
};
