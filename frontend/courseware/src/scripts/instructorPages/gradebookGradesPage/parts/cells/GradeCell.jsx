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

import classNames from 'classnames';
import LoLink from '../../../../components/links/LoLink';
import { InstructorGradebookSyncColumnLearnerPageLink } from '../../../../utils/pageLinks';
import dayjs from 'dayjs';
import { get } from 'lodash';
import GradeBadge from '../../../../directives/GradeBadge';
import { withTranslation } from '../../../../i18n/translationContext';
import { isGradableAssignment } from '../../../../utilities/contentTypes';
import {
  ItemSyncStatus,
  hasLtiHistoryRight,
  latestGradeSyncStatus,
} from '../../../../utilities/gradeSync';
import { connect } from 'react-redux';
import { compose, withState } from 'recompose';

import GradeCellTooltip from '../controls/GradeCellTooltip';
import GradeCellEditing from './GradeCellEditing';

const GradeCell = ({
  translate,
  grade,
  percentFormat,
  gradeDisplayMethod,
  isGradable,
  canEdit,
  isEditing,
  setIsEditing,
  isLate,
  isPending,
  syncStatus = latestGradeSyncStatus(grade),
  id = 'gc-' + grade.user_id + grade.column_id,
}) => (
  <div
    className={classNames('grade-body-cell', {
      'late-submission': isLate,
    })}
    id={id}
    title={
      isLate
        ? translate('GRADEBOOK_GRADE_LATE', {
            time: grade.info.submissionDate,
          })
        : isPending
          ? translate('GRADEBOOK_GRADE_PENDING', {
              time: grade.info.submissionDate,
            })
          : ''
    }
  >
    {isEditing && (
      <GradeCellEditing
        id={id}
        grade={grade}
        gradeDisplayMethod={gradeDisplayMethod}
        endEditing={() => setIsEditing(false)}
      />
    )}

    {canEdit && !isEditing && (
      <button
        className="grade-cell-edit-button"
        onClick={() => setIsEditing(true)}
      >
        <GradeBadge
          grade={grade}
          outline={true}
          display={gradeDisplayMethod}
          isPending={isPending}
          className={isPending ? 'text-warning' : ''}
          showEmptyPostfix={true}
        />
      </button>
    )}

    {!canEdit && (
      <div className="grade-cell-no-edit">
        <GradeBadge
          grade={grade}
          outline={true}
          display={percentFormat ? 'percentSign' : gradeDisplayMethod}
          isPending={isPending}
          className={isPending ? 'text-warning' : ''}
        />
      </div>
    )}

    {hasLtiHistoryRight && (
      <LoLink
        className={classNames('lti-sync-status-link', {
          'text-danger': syncStatus === ItemSyncStatus.Failed,
          'text-success': syncStatus === ItemSyncStatus.Synced,
          'text-warning': syncStatus === ItemSyncStatus.Attempted,
        })}
        to={InstructorGradebookSyncColumnLearnerPageLink.toLink({
          columnId: grade.column_id,
          learnerId: grade.user_id,
        })}
      >
        <span className="material-icons">history</span>
      </LoLink>
    )}

    {isGradable && (
      <GradeCellTooltip
        grade={grade}
        target={id}
        isOpen={isEditing}
      />
    )}
  </div>
);

export default compose(
  connect((state, { learnerId, contentId, percentFormat }) => {
    const content = state.api.contentItems[contentId] || {};
    const grade = get(state.api.gradeByContentByUser, [learnerId, contentId], {
      user_id: learnerId,
      column_id: contentId,
      info: {},
    });
    return {
      learnerId,
      grade,
      percentFormat,
      gradeDisplayMethod: state.ui.gradebookTableOptions.gradeDisplayMethod,
      isGradable: isGradableAssignment(content),
      isLate: content.dueDate && dayjs(grade.info.submissionDate).isAfter(content.dueDate),
      isPending: grade.info.status === 'Pending',
    };
  }),
  withState('isEditing', 'setIsEditing', false),
  withTranslation
)(GradeCell);
