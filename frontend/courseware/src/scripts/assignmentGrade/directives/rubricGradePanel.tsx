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

import { GradingRubric } from './rubric/gradingRubric.tsx';
import { GradingRubricViewCards } from './rubric/gradingRubricViewCards.tsx';

interface RubricGradePanelProps {
  /** The grade model (stays Angular); `grade.outgoing.rubric` is the `Rubric`. */
  grade: any;
  /** Read-only (view cards) vs editable; defaults to false. */
  disableEdit?: boolean;
}

/**
 * React port of the `rubricGradePanel` directive (assignmentGrade rubric panels — leaf 3): the thin
 * wrapper that shows the rubric for a grade, editable (`GradingRubric`) or read-only
 * (`GradingRubricViewCards`) per `disableEdit`. Was an Angular directive; now native React rendering the
 * React containers directly, bridged back via react2angular (with the i18n provider) so the still-Angular
 * `gradingPanel` keeps rendering `<rubric-grade-panel>`. The grade `Rubric` model stays Angular. The old
 * controller's `$watch('grade.outgoing.pointsAwarded')` → `$element.find('.direct-grade-input')` is
 * dropped: that input lives in `submissionScore`, not in this element's subtree, so the find never
 * matched (dead code). DOM preserved: `.composite-grade-grading`.
 */
export const RubricGradePanel: React.FC<RubricGradePanelProps> = ({ grade, disableEdit }) => {
  const rubric = grade?.outgoing?.rubric;
  if (!rubric) return null;

  return (
    <div className="composite-grade-grading">
      {disableEdit ? <GradingRubricViewCards rubric={rubric} /> : <GradingRubric rubric={rubric} />}
    </div>
  );
};
