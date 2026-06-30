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

import { HtmlWithMathJax } from '../../components/HtmlWithMathjax';
import React from 'react';

/**
 * React port of the `questionLevelRemediation` component (B2-quiz). Renders the
 * question's correct / incorrect feedback. DOM preserved from the template:
 * `.alert.alert-success.question-level-remediation[data-id=correct-feedback]` and
 * `.alert.alert-danger…[data-id=incorrect-feedback]`, each wrapping the author
 * feedback HTML via `<HtmlWithMathJax>` (the Selenide feedback selectors).
 */

interface Remediation {
  correct?: string;
  incorrect?: string;
}

export const QuestionLevelRemediation: React.FC<{ remediation?: Remediation }> = ({ remediation }) => (
  <>
    {!!remediation?.correct?.length && (
      <div
        className="alert alert-success question-level-remediation"
        data-id="correct-feedback"
      >
        <div>
          <HtmlWithMathJax html={remediation.correct} />
        </div>
      </div>
    )}
    {!!remediation?.incorrect?.length && (
      <div
        className="alert alert-danger question-level-remediation"
        data-id="incorrect-feedback"
      >
        <div>
          <HtmlWithMathJax html={remediation.incorrect} />
        </div>
      </div>
    )}
  </>
);
