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

import { compose } from 'recompose';
import classNames from 'classnames';
import { connect } from 'react-redux';
import { withTranslation } from '../../../../i18n/translationContext';
import { CONTENT_TYPE_LTI } from '../../../../utilities/contentTypes';
import {
  hasLtiHistoryRight,
  ItemSyncStatus,
  latestColumnSyncStatus,
} from '../../../../utilities/gradeSync';
import {
  InstructorAssignmentOverviewPageLink,
  InstructorGradebookSyncColumnPageLink,
} from '../../../../utils/pageLinks';
import LoLink from '../../../../components/links/LoLink';

const ColumnHeaderCell = ({ translate, content, column, syncStatus, isLti }) => (
  <div className="column-header-cell">
    {isLti ? (
      <div className="header-text">{content.name}</div>
    ) : (
      <LoLink
        className="header-text"
        to={InstructorAssignmentOverviewPageLink.toLink({
          contentId: content.id,
        })}
        title={translate('GRADER_GO_TO')}
      >
        {content.name}
      </LoLink>
    )}

    {hasLtiHistoryRight && (
      <LoLink
        className={classNames('lti-sync-status-link', {
          'text-danger': syncStatus === ItemSyncStatus.Failed,
          'text-success': syncStatus === ItemSyncStatus.Synced,
          'text-warning': syncStatus === ItemSyncStatus.Attempted,
        })}
        to={InstructorGradebookSyncColumnPageLink.toLink({
          columnId: column.id,
        })}
      >
        <span className="material-icons">history</span>
        <span className="sr-only">{translate('GRADEBOOK_SYNC_STATUS')}</span>
      </LoLink>
    )}
  </div>
);

export default compose(
  connect((state, { columnId }) => {
    const content = state.api.contentItems[columnId];
    return {
      content,
      column: state.api.gradebookColumns[columnId],
      syncStatus: latestColumnSyncStatus(state.api.gradebookColumns[columnId]),
      isLti: content.typeId === CONTENT_TYPE_LTI,
    };
  }),
  withTranslation
)(ColumnHeaderCell);
