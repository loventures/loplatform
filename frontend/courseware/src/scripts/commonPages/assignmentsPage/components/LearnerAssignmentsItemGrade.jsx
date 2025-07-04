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

import { withTranslation } from '../../../i18n/translationContext';
import GradeBadge from '../../../directives/GradeBadge';

import { Credit, NoCredit, ExtraCredit } from '../../../utilities/creditTypes';

const creditTypeKeys = {
  [Credit]: 'ASSIGNMENT_FOR_CREDIT',
  [NoCredit]: 'ASSIGNMENT_NO_CREDIT',
  [ExtraCredit]: 'ASSIGNMENT_EXTRA_CREDIT',
};

const LearnerAssignmentsItemGrade = ({
  translate,
  content,
  gradebookColumn,
  latestSubmissionTime = content.activity.attemptOverview &&
    content.activity.attemptOverview.latestSubmissionTime,
}) => (
  <div className="assignment-summary flex-row-content flex-column">
    {!content.grade && (
      <div className="grade-status nowrap text-color">
        {latestSubmissionTime ? (
          <span>{translate('NOT_GRADED')}</span>
        ) : (
          <span>{translate('NOT_STARTED')}</span>
        )}
      </div>
    )}

    {content.grade && (
      <div className="assignment-grade">
        <GradeBadge
          grade={content.grade}
          outline={true}
          percent="full"
        />
      </div>
    )}

    <small className="credit-type text-color">
      {translate(creditTypeKeys[gradebookColumn.credit])}
    </small>

    <small className="assignment-grade-points">
      <GradeBadge
        grade={content.grade || { maximumPoints: gradebookColumn.maximumPoints }}
        outline={true}
        display="pointsOutOf"
        showEmptyPostfix={true}
      />
    </small>
  </div>
);

export default withTranslation(LearnerAssignmentsItemGrade);
