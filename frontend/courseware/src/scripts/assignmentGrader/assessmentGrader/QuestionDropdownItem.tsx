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

import { useTranslation } from '../../i18n/translationContext.tsx';

/**
 * React port of the `questionDropdownItem` directive (the grader's question-selector row): the
 * "Question N" title. DOM preserved from questionDropdownItem.html — note the four status `block-badge`
 * spans there were dead code (the directive's isolate scope had no `attempt`, so every
 * `ng-if="attempt.valid && …"` evaluated falsy), so the rendered row was always title-only; we keep that.
 */
export const QuestionDropdownItem: React.FC<{ question: any }> = ({ question }) => {
  const translate = useTranslation();
  return (
    <div className="flex-row-content">
      <span className="flex-col-fluid text-start text-truncate">
        {translate('GRADER_SELECT_QUESTION_TITLE', { ORDINAL: (question?.index ?? 0) + 1 }, 'messageformat')}
      </span>
    </div>
  );
};

export default QuestionDropdownItem;
