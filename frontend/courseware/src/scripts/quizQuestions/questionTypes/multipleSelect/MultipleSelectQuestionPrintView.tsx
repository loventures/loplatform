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
  selection?: { selectedIndexes?: (string | number)[] };
}

export interface MultipleSelectQuestionPrintViewProps {
  index: number;
  assessment?: { settings?: { isCheckpoint?: boolean } };
  questionCount?: number;
  question: { choices?: Choice[]; displayDetail?: { correctAnswer?: boolean } };
  response?: Response;
}

/**
 * React port of the learner `multipleSelectQuestionPrintView` (B2-quiz print). Read-only multi-select:
 * each selected choice gets `.border-primary`; once correctness is shown, correct choices get a
 * checkmark (mirroring the Angular template's inlined expressions verbatim — note it keys correctness off
 * `choice.correct`, not the selection). DOM preserved from multipleSelectQuestionPrintView.html
 * (`.border-primary`, `.icon-checkmark`, `.choice-text`, the `dl > dd` list).
 */
export const MultipleSelectQuestionPrintView: React.FC<MultipleSelectQuestionPrintViewProps> = ({
  index,
  assessment,
  questionCount,
  question,
  response,
}) => {
  const selection: Record<string, boolean> = {};
  (response?.selection?.selectedIndexes ?? []).forEach(a => {
    selection[a] = true;
  });
  const isCheckpoint = assessment?.settings?.isCheckpoint;
  const correctAnswer = !!question.displayDetail?.correctAnswer;

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
              hasCorrectness={correctAnswer && !!choice.correct}
              isCorrect={choice.correct}
            >
              <div className={selection[$index] ? 'p-2 border border-primary' : 'p-2 border'}>
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
