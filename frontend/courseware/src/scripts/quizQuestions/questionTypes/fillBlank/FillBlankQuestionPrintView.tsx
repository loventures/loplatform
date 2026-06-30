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

import { useTranslation } from '../../../i18n/translationContext.tsx';
import { PrintQuestionTemplate } from '../../questionTemplates/PrintQuestionTemplate.tsx';
import { buildSegments, responseToEntries } from './FillBlankQuestionBaseView.tsx';

export interface FillBlankQuestionPrintViewProps {
  index: number;
  assessment?: { settings?: { isCheckpoint?: boolean } };
  questionCount?: number;
  question: { questionText: string; blanks?: any[]; displayDetail?: { correctAnswer?: boolean } };
  response?: { selection?: { entries?: string[] } | null };
  canEditAnswer?: boolean;
}

/**
 * React port of the learner `fillBlankQuestionPrintView` (B2-quiz print) — retires this type's last
 * `compile` use. The question text interleaves text slices (preserved HTML) with each blank, rendered
 * read-only: the learner's entry as a disabled input, plus the correct-answer candidates (when
 * correctness is shown) or an empty candidate. DOM preserved from fillBlankQuestionPrintView.html +
 * the createBlankHtml template (`.question-text`, `.fill-blank-text`, `input.form-control`,
 * `.fill-blank-answers > .blank-candidate`).
 */
export const FillBlankQuestionPrintView: React.FC<FillBlankQuestionPrintViewProps> = ({
  index,
  assessment,
  questionCount,
  question,
  response,
  canEditAnswer,
}) => {
  const translate = useTranslation();
  const selection = responseToEntries(response);
  const correctAnswer = !!question.displayDetail?.correctAnswer;
  const segments = buildSegments(question);

  const questionText = (
    <div className="question-text">
      {segments.map((seg, i) => {
        if (seg.kind === 'text') {
          return seg.trailing ? (
            <span
              key={i}
              dangerouslySetInnerHTML={{ __html: seg.html }}
            />
          ) : (
            <div
              key={i}
              className="fill-blank-text"
              dangerouslySetInnerHTML={{ __html: seg.html }}
            />
          );
        }
        const { idx, blank } = seg;
        return (
          <div key={i}>
            {response && (
              <input
                className="form-control"
                value={selection[idx] ?? ''}
                disabled={!canEditAnswer}
                readOnly
              />
            )}
            {!canEditAnswer && correctAnswer && (
              <div className="fill-blank-answers">
                <span>{translate('FILL_BLANK_CORRECT_ANSWER_LABEL')}</span>
                {(blank.answers ?? []).map((candidate: string, ci: number) => (
                  <span
                    key={ci}
                    className="blank-candidate"
                  >
                    {candidate}
                  </span>
                ))}
              </div>
            )}
            {!canEditAnswer && !correctAnswer && (
              <div className="fill-blank-answers">
                <span
                  className="blank-candidate"
                  style={{ minWidth: '10rem' }}
                >
                  &nbsp;
                </span>
              </div>
            )}
          </div>
        );
      })}
    </div>
  );

  return (
    <PrintQuestionTemplate
      className="question fill-blank-question"
      index={index}
      assessment={assessment}
      questionCount={questionCount}
      question={question as any}
      response={response as any}
      questionTextSlot={questionText}
    />
  );
};
