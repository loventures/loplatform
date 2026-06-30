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

import React from 'react';

import { HtmlWithMathJax } from '../../../components/HtmlWithMathjax';
import { choiceOrdinal } from '../../../filters/choiceOrdinalFilter.js';
import { PrintQuestionDistractorRow } from '../../questionTemplates/PrintQuestionDistractorRow.tsx';
import { PrintQuestionTemplate } from '../../questionTemplates/PrintQuestionTemplate.tsx';

interface Choice {
  choiceText?: string;
  correct?: boolean;
}
interface Response {
  selection?: { selectedIndexes?: number[] };
}

export interface MultipleChoiceQuestionPrintViewProps {
  index: number;
  assessment?: { settings?: { isCheckpoint?: boolean } };
  questionCount?: number;
  question: { choices?: Choice[]; displayDetail?: { correctAnswer?: boolean } };
  response?: Response;
}

/**
 * React port of the learner `multipleChoiceQuestionPrintView` (B2-quiz print) — also serves true/false
 * (same registry entry). Renders the React PrintQuestionTemplate hub with the choice list as content,
 * read-only: the selected choice gets `.border-primary`, and (once correctness is shown) each
 * selected/correct choice gets a checkmark/cross via PrintQuestionDistractorRow. DOM preserved verbatim
 * from multipleChoiceQuestionPrintView.html so PrintModuleTest holds (`.border-primary`,
 * `.icon-checkmark`, `.choice-text`, the `dl > dd` list).
 */
export const MultipleChoiceQuestionPrintView: React.FC<MultipleChoiceQuestionPrintViewProps> = ({
  index,
  assessment,
  questionCount,
  question,
  response,
}) => {
  const selectedIndex =
    response && response.selection ? (response.selection.selectedIndexes?.[0] ?? null) : null;
  const isCheckpoint = assessment?.settings?.isCheckpoint;

  const hasCorrectness = ($index: number, choice: Choice) =>
    !!question.displayDetail?.correctAnswer && ($index === selectedIndex || !!choice.correct);

  return (
    <PrintQuestionTemplate
      className="question choice-question multiple-choice-question"
      index={index}
      assessment={assessment}
      questionCount={questionCount}
      question={question as any}
      response={response as any}
    >
      <dl>
        {(question.choices ?? []).map((choice, $index) => (
          <dd key={$index}>
            <PrintQuestionDistractorRow
              choice={choice}
              hasCorrectness={hasCorrectness($index, choice)}
              isCorrect={choice.correct}
            >
              <div className={$index === selectedIndex ? 'p-2 border border-primary' : 'p-2 border'}>
                {!isCheckpoint && (
                  <div className="float-left">
                    <span>{choiceOrdinal($index)}.</span>
                  </div>
                )}

                <div className="ms-4 choice-text">
                  <HtmlWithMathJax html={choice.choiceText} />
                </div>
              </div>
            </PrintQuestionDistractorRow>
          </dd>
        ))}
      </dl>
    </PrintQuestionTemplate>
  );
};
