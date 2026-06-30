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

interface GradingQuestionTemplateProps {
  index: number;
  question: { questionText?: string; reference?: { nodeName?: string } };
  /** Overrides the default `.question-text` prompt (the Angular `questionTextSlot`). */
  questionTextSlot?: React.ReactNode;
  /** The graded answer UI (the Angular `questionContentSlot`). */
  children?: React.ReactNode;
}

/**
 * React port of the shared `gradingQuestionTemplate` hub (B2-quiz grading): the instructor-grading chrome
 * a question's graded answer renders into — the question number column + the prompt, then the content
 * slot. DOM preserved verbatim from `gradingQuestionTemplate.html` (`.grading-question-template`,
 * `.question-number-column > .question-number`, `.question-text`). A plain React component consumed by
 * React grading views.
 */
export const GradingQuestionTemplate: React.FC<GradingQuestionTemplateProps> = ({
  index,
  question,
  questionTextSlot,
  children,
}) => (
  <div
    className="grading-question-template question-container"
    data-asset-name={question.reference?.nodeName}
  >
    <div className="question-body mb-3">
      <div className="question-number-column">
        <div className="question-number">{index + 1}</div>
      </div>

      <div className="question-content-column">
        {questionTextSlot ?? (
          <div className="question-text">
            <HtmlWithMathJax html={question.questionText} />
          </div>
        )}
        <div>{children}</div>
      </div>
    </div>
  </div>
);

export default GradingQuestionTemplate;
