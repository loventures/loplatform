/*
 * LO Platform copyright (C) 2007–2025 LO Ventures LLC.
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

import DueDateBadge from '../../../contentPlayerComponents/parts/DueDateBadge';
import { percentFilter } from '../../../filters/percent';
import { withTranslation } from '../../../i18n/translationContext';
import { Credit } from '../../../utilities/creditTypes';

const LearnerAssignmentsItemInfo = ({
  translate,
  content,
  gradebookColumn,

  latestSubmissionTime = content.activity.attemptOverview &&
    content.activity.attemptOverview.latestSubmissionTime,
}) => (
  <div className="grade-item-content flex-col-fluid">
    <h2 className="due-date-effects">{content.name}</h2>
    {content.dueDate && (
      <DueDateBadge
        date={content.dueDate}
        completed={!!latestSubmissionTime}
        exempt={content.dueDateExempt}
      />
    )}
    <div className="grade-submissions mt-2">
      {latestSubmissionTime && (
        <span className="submission-date text-color">
          {translate('ASSIGNMENT_LIST_MY_LAST_SUBMISSION', {
            mostRecentSubmissionDateForCurrentUser: latestSubmissionTime,
          })}
        </span>
      )}
      {content.gradebookCategory && (
        <span className="grade-category text-color">
          {translate('ASSIGNMENT_LIST_CATEGORY', {
            category: content.gradebookCategory,
          })}
        </span>
      )}
      {gradebookColumn && gradebookColumn.credit === Credit && (
        <span className="grade-weight text-color">
          {translate('ASSIGNMENT_LIST_PERCENT_WEIGHT', {
            overallWeight: percentFilter(gradebookColumn.weight, 1),
          })}
        </span>
      )}
    </div>
  </div>
);

export default withTranslation(LearnerAssignmentsItemInfo);
