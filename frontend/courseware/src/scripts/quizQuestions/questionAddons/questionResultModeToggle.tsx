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
import { useId, useState } from 'react';
import { WithTranslateProps } from '../../i18n/translationContext.tsx';

export type QuestionResultModeToggleProps = WithTranslateProps & {
  showResponse?: () => void;
  showAnswer?: () => void;
};

/**
 * Radio toggle between "show response" and "show correct answer" on a quiz
 * result, migrated from the AngularJS `questionResultModeToggle` component to
 * React. Rendered directly by the React question result views (FillBlank /
 * Ordering); `showResponse`/`showAnswer` are function props. DOM/classes
 * preserved so existing styling keeps working; the AngularJS `{{$id}}` ids
 * become a stable useId().
 */
export const QuestionResultModeToggle = ({
  showResponse,
  showAnswer,
  translate,
}: QuestionResultModeToggleProps) => {
  const [showingResponse, setShowingResponse] = useState('true');
  const id = useId();

  const toggleMode = (value: string) => {
    setShowingResponse(value);
    if (value === 'true') {
      showResponse?.();
    } else {
      showAnswer?.();
    }
  };

  return (
    <div>
      <label className="custom-control custom-control-inline custom-radio">
        <input
          className="custom-control-input"
          id={`${id}-show-response`}
          name={`results-toggle-${id}`}
          type="radio"
          value="true"
          checked={showingResponse === 'true'}
          onChange={() => toggleMode('true')}
        />
        <span className="custom-control-label">{translate('QUIZ_RESULTS_SHOW_RESPONSE')}</span>
      </label>

      <label className="custom-control custom-control-inline custom-radio">
        <input
          className="custom-control-input"
          id={`${id}-show-answer`}
          name={`results-toggle-${id}`}
          type="radio"
          value="false"
          checked={showingResponse === 'false'}
          onChange={() => toggleMode('false')}
        />
        <span className="custom-control-label">{translate('QUIZ_RESULTS_SHOW_CORRECT')}</span>
      </label>
    </div>
  );
};
