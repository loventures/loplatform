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

import * as React from 'react';
import { get } from 'lodash';
import { WithTranslateProps } from '../../i18n/translationContext.tsx';

const scoringMessages: Record<string, string> = {
  allOrNothing: 'NO_PARTIAL_CREDIT_GRADING_STRATEGY',
  allowPartialCredit: 'PARTIAL_CREDIT_GRADING_STRATEGY',
  fullCreditForAnyCorrectChoice: 'FULL_CREDIT_FOR_ANY_CORRECT_CHOICE_GRADING_STRATEGY',
};

const matchingMessages: Record<string, string> = {
  allOrNothing: 'NO_PARTIAL_CREDIT_GRADING_STRATEGY_MATCHING',
  allowPartialCredit: 'PARTIAL_CREDIT_GRADING_STRATEGY_MATCHING',
};

const scoringClassName: Record<string, string> = {
  allOrNothing: 'deny-partial-credit',
  allowPartialCredit: 'allow-partial-credit',
  fullCreditForAnyCorrectChoice: 'full-credit-any-correct',
};

export type QuestionGradingStrategyProps = WithTranslateProps & {
  question?: { scoringOption?: string; _type?: string };
};

/**
 * Shows the partial-credit grading strategy of a question as an info line,
 * migrated verbatim from the AngularJS `questionGradingStrategy` component to
 * React (bridged via react2angular). DOM preserved: the
 * `<p class="question-grading-strategy">` with an info icon and the
 * (translated) strategy message under its scoring class.
 */
export const QuestionGradingStrategy = ({ question, translate }: QuestionGradingStrategyProps) => {
  const scoringOption = get(question, 'scoringOption', 'allOrNothing');
  const msg = question?._type === 'matching' ? matchingMessages[scoringOption] : scoringMessages[scoringOption];
  const className = scoringClassName[scoringOption];

  return (
    <p className="question-grading-strategy">
      <span className="icon icon-info" aria-hidden="true" />
      <span className={className}>{translate(msg)}</span>
    </p>
  );
};
