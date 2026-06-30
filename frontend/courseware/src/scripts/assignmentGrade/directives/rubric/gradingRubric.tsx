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

import { useTranslation } from '../../../i18n/translationContext';
import { openRubricModal } from './rubricModal.tsx';
import { RubricSectionEditable } from './rubricSectionEditable.tsx';

interface GradingRubricProps {
  /** A `Rubric` model (stays Angular) with `.sections`. */
  rubric: any;
}

/**
 * React port of the `gradingRubric` component (assignmentGrade rubric panels): the editable rubric —
 * one `RubricSectionEditable` per criterion. Was an Angular component (`RubricCtrl`); now native React,
 * rendering the React sections directly and opening the React rubric modal as the title action (the old
 * `RubricCtrl.titleAction`/`$uibModal`). Bridged back via react2angular (with the i18n provider) so the
 * native-React `rubricGradePanel` renders it directly (`{GradingRubric}`). DOM preserved: `.rubric`, the
 * `ul > li` section list.
 */
export const GradingRubric: React.FC<GradingRubricProps> = ({ rubric }) => {
  const translate = useTranslation();
  return (
    <div
      className="rubric"
      role="region"
      aria-label={translate('GRADING_RUBRIC_REGION')}
    >
      <ul className="list-unstyled">
        {rubric.sections.map((section: any) => (
          <li key={section.index}>
            <RubricSectionEditable
              section={section}
              titleIcon="icon-info"
              titleAction={s => openRubricModal(rubric, s.index)}
            />
          </li>
        ))}
      </ul>
    </div>
  );
};
