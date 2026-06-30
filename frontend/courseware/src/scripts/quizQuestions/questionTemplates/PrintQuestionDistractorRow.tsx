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

import { HtmlWithMathJax } from '../../components/HtmlWithMathjax';

export interface PrintQuestionDistractorRowProps {
  choice: { correct?: boolean };
  hasCorrectness?: boolean;
  isCorrect?: boolean;
  /** Inline choice feedback (rationale/remediation) shown when the choice has correctness. */
  choiceFeedback?: string;
  showInlineRemediation?: boolean;
  /** The distractor UI (the Angular `distractorSlot` transclusion). */
  children?: React.ReactNode;
}

/**
 * React port of `printQuestionDistractorRowTemplate` (B2-quiz print): one print choice row — a
 * correctness icon (checkmark/cross) when correctness is shown, the transcluded distractor
 * (`children`), and the optional inline choice feedback. DOM preserved verbatim from the template
 * (`.question-distractor-row-template`, `.icon.icon-checkmark`/`.icon-cross`, `.ms-4.distractor`,
 * the `.arrow-top.border` feedback box with `.border-success`/`.border-danger`).
 */
export const PrintQuestionDistractorRow: React.FC<PrintQuestionDistractorRowProps> = ({
  choice,
  hasCorrectness,
  isCorrect,
  choiceFeedback,
  showInlineRemediation,
  children,
}) => (
  <div className="question-distractor-row-template">
    {hasCorrectness && (
      <div className="mt-2 float-left">
        <div className={`d-inline-block icon ${isCorrect ? 'icon-checkmark' : 'icon-cross'}`} />
      </div>
    )}

    <div className="ms-4 distractor">{children}</div>

    {showInlineRemediation && choiceFeedback && hasCorrectness && (
      <div className={`ms-5 mt-2 p-2 arrow-top border ${choice.correct ? 'border-success' : 'border-danger'}`}>
        <div>
          <HtmlWithMathJax html={choiceFeedback} />
        </div>
      </div>
    )}
  </div>
);

export default PrintQuestionDistractorRow;
