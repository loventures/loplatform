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
import { useTranslation } from '../../i18n/translationContext.tsx';

export interface ChoiceLevelRemediationProps {
  remediation?: string;
  isChoiceCorrect?: boolean;
  isInstructor?: boolean;
  isSelected?: boolean;
  isMulti?: boolean;
  index?: number;
}

/**
 * React port of the inline per-choice remediation (`choiceLevelRemediation`,
 * B2-quiz). Shown under a distractor: the author's choice feedback for the
 * selected/instructor case, or a "this was the correct choice" note otherwise.
 * DOM preserved from choiceLevelRemediation.html — `.alert.mb-0.question-choice-remediation`
 * with the alert-info/-success/-danger modifier and the instructor `data-id`
 * (the Selenide `.alert-*.question-choice-remediation` selectors). The host's
 * `inline-remediation` class (its sole use site) is folded onto the root.
 */
export const ChoiceLevelRemediation: React.FC<ChoiceLevelRemediationProps> = ({
  remediation,
  isChoiceCorrect,
  isInstructor,
  isSelected,
  isMulti,
  index,
}) => {
  const translate = useTranslation();
  const selectedOrInstructor = isSelected || isInstructor;

  const classes = [
    'inline-remediation alert mb-0 question-choice-remediation',
    !isSelected && !isInstructor ? 'alert-info' : '',
    isChoiceCorrect && selectedOrInstructor ? 'alert-success' : '',
    !isChoiceCorrect && selectedOrInstructor ? 'alert-danger' : '',
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <div
      className={classes}
      data-id={isInstructor ? `choice-${index}-feedback` : undefined}
    >
      {selectedOrInstructor ? (
        <div>
          <HtmlWithMathJax html={remediation} />
        </div>
      ) : (
        <div>{translate(isMulti ? 'QUESTION_CHOICE_WAS_CORRECT_MULTI' : 'QUESTION_CHOICE_WAS_CORRECT')}</div>
      )}
    </div>
  );
};
