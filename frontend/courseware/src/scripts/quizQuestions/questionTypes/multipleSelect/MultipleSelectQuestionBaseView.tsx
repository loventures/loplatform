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
import { SELECTION_TYPE_MULTIPLE_SELECT } from '../../../utilities/questionTypes.js';
import { BasicQuestionTemplate } from '../../questionTemplates/BasicQuestionTemplate.tsx';
import { QuestionDistractorRow } from '../../questionTemplates/QuestionDistractorRow.tsx';

interface Choice {
  choiceText?: string;
  correct?: boolean;
  rationales?: any[];
}
interface Response {
  selection?: { selectedIndexes?: (string | number)[]; responseType?: string } | null;
}
interface Question {
  choices?: Choice[];
  displayDetail?: { correctAnswer?: boolean };
}
type Selection = Record<string, boolean>;

export interface MultipleSelectQuestionBaseViewProps {
  index: number;
  focusOnRender?: boolean;
  assessment?: { settings?: { isCheckpoint?: boolean } };
  questionCount?: number;
  question: Question;
  response?: Response;
  changeAnswer: (index: number, response: any) => void;
  canEditAnswer?: boolean;
}

const responseToSelection = (response?: Response): Selection => {
  const selectedIndexes = response?.selection?.selectedIndexes ?? [];
  const sel: Selection = {};
  selectedIndexes.forEach(a => {
    sel[a as any] = true;
  });
  return sel;
};

/**
 * React port of the learner `multipleSelectQuestionBaseView` (B2-quiz). Like the
 * multipleChoice view but with checkboxes and a `selection` map (choice index →
 * boolean), reusing the shared React `QuestionDistractorRow` (with `isMulti`).
 * DOM preserved verbatim from multipleSelectQuestionBaseView.html — the
 * `.question-distractor-list`, `label.question-choice-content`
 * (disabled/selected/correct/incorrect), the `input[type=checkbox].form-check-input`
 * (`name="choice-group-{choiceIndex}"`), ordinals and `.choice-text`. The print
 * view stays Angular; the wrapper picks base-vs-print.
 */
export const MultipleSelectQuestionBaseView: React.FC<MultipleSelectQuestionBaseViewProps> = ({
  index,
  assessment,
  questionCount,
  question,
  response,
  changeAnswer,
  canEditAnswer,
}) => {
  const translate = useTranslation();

  const [selection, setSelection] = useState<Selection>(() => responseToSelection(response));
  useEffect(() => {
    if (response) setSelection(responseToSelection(response));
  }, [response]);

  const isCheckpoint = assessment?.settings?.isCheckpoint;

  const selectionToResponse = (sel: Selection) => {
    const resp: any = response || {};
    let selection: any = (response && response.selection) || { responseType: SELECTION_TYPE_MULTIPLE_SELECT };
    const hasSelection = Object.values(sel).some(v => v);
    if (hasSelection) {
      selection = { ...selection, selectedIndexes: Object.keys(sel).filter(k => sel[k]) };
    } else {
      selection = null;
    }
    return { ...resp, selection };
  };

  const toggleChoiceIndex = (choiceIndex: number, checked: boolean) => {
    const next = { ...selection, [choiceIndex]: checked };
    setSelection(next);
    changeAnswer(index, selectionToResponse(next));
  };

  const hasCorrectness = (choice: Choice) =>
    isCheckpoint ? false : response ? !!question.displayDetail?.correctAnswer : !!choice.correct;

  const isCorrect = (i: number, choice: Choice) =>
    response
      ? (!!choice.correct && !!selection[i]) || (!choice.correct && !selection[i])
      : !!choice.correct;

  const choiceOrdinalAriaLabel = (i: number, choice: Choice) => {
    let key: string;
    if (hasCorrectness(choice)) {
      key = choice.correct
        ? isCorrect(i, choice)
          ? 'QUIZ_PLAYER_CHOICE_ORDINAL_LABEL_CORRECT'
          : 'QUIZ_PLAYER_CHOICE_ORDINAL_LABEL_CORRECT_NOT_SELECTED'
        : isCorrect(i, choice)
          ? 'QUIZ_PLAYER_CHOICE_ORDINAL_LABEL_INCORRECT_NOT_SELECTED'
          : 'QUIZ_PLAYER_CHOICE_ORDINAL_LABEL_INCORRECT_SELECTED';
    } else {
      key = 'QUIZ_PLAYER_CHOICE_ORDINAL_LABEL';
    }
    return translate(key, { ordinal: choiceOrdinal(i) });
  };

  return (
    <BasicQuestionTemplate
      className="question choice-question multiple-select-question"
      index={index}
      assessment={assessment}
      questionCount={questionCount}
      question={question as any}
      response={response as any}
    >
      <ol className="question-distractor-list">
        {(question.choices ?? []).map((choice, $index) => {
          const checked = !!selection[$index];
          const labelClasses = [
            'question-choice-content',
            !canEditAnswer ? 'disabled' : '',
            checked || choice.correct ? 'selected' : '',
            checked && isCorrect($index, choice) ? 'correct' : '',
            checked && !isCorrect($index, choice) ? 'incorrect' : '',
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
                hasCorrectness={hasCorrectness(choice)}
                isCorrect={isCorrect($index, choice)}
                isSelected={response ? checked : false}
                isInstructor={!response}
                isMulti={true}
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
                      name={`choice-group-${$index}`}
                      type="checkbox"
                      checked={checked}
                      disabled={!canEditAnswer}
                      onChange={e => toggleChoiceIndex($index, e.target.checked)}
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
