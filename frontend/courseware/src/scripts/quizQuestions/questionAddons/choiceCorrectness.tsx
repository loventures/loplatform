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

export type ChoiceCorrectnessProps = {
  isCorrect?: boolean;
};

/**
 * A correct (check) or incorrect (cross) icon for a quiz choice. Migrated from
 * the AngularJS `choiceCorrectness` component to React (bridged back via
 * react2angular). The DOM is preserved verbatim — a single
 * `.choice-tooltip.correct` or `.choice-tooltip.incorrect` div wrapping the same
 * SVG path — so existing CSS and any selectors keep working.
 */
const ChoiceCorrectness = ({ isCorrect }: ChoiceCorrectnessProps) =>
  isCorrect ? (
    <div className="choice-tooltip correct">
      <svg aria-hidden="true" stroke="currentColor" viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg">
        <path
          fill="none"
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth="32"
          d="M416 128L192 384l-96-96"
        />
      </svg>
    </div>
  ) : (
    <div className="choice-tooltip incorrect">
      <svg aria-hidden="true" stroke="currentColor" viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg">
        <path
          fill="none"
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth="32"
          d="M368 368L144 144m224 0L144 368"
        />
      </svg>
    </div>
  );

export { ChoiceCorrectness };
