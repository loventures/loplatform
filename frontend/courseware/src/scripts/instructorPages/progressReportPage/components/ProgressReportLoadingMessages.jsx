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

import { connect } from 'react-redux';
import { withTranslation } from '../../../i18n/translationContext';

import { selectProgressReportListStateComponent } from '../selectors';

const ProgressReportLoadingMessages = ({ translate, listState, className }) => (
  <div>
    {listState.data.totalCount > 0 && listState.data.filterCount === 0 && (
      <div className={className}>
        <div className="alert alert-warning m-0">{translate('SRS_STORE_FILTERED')}</div>
      </div>
    )}

    {listState.data.totalCount === 0 && (
      <div className={className}>
        <div className="alert alert-warning m-0">{translate('SRS_STORE_EMPTY')}</div>
      </div>
    )}
  </div>
);

export default connect(selectProgressReportListStateComponent)(
  withTranslation(ProgressReportLoadingMessages)
);
