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
import {
  hasLtiHistoryRight,
  ItemSyncStatus,
  latestColumnSyncStatus,
} from '../../../../utilities/gradeSync';
import { InstructorGradebookSyncColumnPageLink } from '../../../../utils/pageLinks';
import LoLink from '../../../../components/links/LoLink';

const OverallColumnHeaderCell = ({ translate, categoryId, syncStatus }) => (
  <div className="column-header-cell">
    <div className="header-text">{translate('OVERALL_GRADE_HEADER')}</div>

    {categoryId !== '_root_' && hasLtiHistoryRight && (
      <LoLink
        className={classNames('lti-sync-status-link', {
          'text-danger': syncStatus === ItemSyncStatus.Failed,
          'text-success': syncStatus === ItemSyncStatus.Synced,
          'text-warning': syncStatus === ItemSyncStatus.Attempted,
        })}
        to={InstructorGradebookSyncColumnPageLink.toLink({
          columnId: categoryId,
        })}
      >
        <span className="material-icons">history</span>
        <span className="sr-only">{translate('GRADEBOOK_SYNC_STATUS')}</span>
      </LoLink>
    )}
  </div>
);

export default compose(
  connect((state, { categoryId }) => {
    return {
      categoryId,
      syncStatus:
        categoryId !== '_root_' && latestColumnSyncStatus(state.api.gradebookColumns[categoryId]),
    };
  }),
  withTranslation
)(OverallColumnHeaderCell);
