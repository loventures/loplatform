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
import { downloadProgressReportActionCreator } from '../actions/tableActions';

const ProgressReportDownloadButton = ({ translate, download }) => (
  <button
    className="icon-btn icon-btn-primary"
    type="button"
    title={translate('PROGRESS_DOWNLOAD')}
    aria-label={translate('PROGRESS_DOWNLOAD')}
    onClick={download}
  >
    <i
      className="h1 lo-icon icon-download"
      role="presentation"
    ></i>
  </button>
);

export default connect(null, { download: downloadProgressReportActionCreator })(
  withTranslation(ProgressReportDownloadButton)
);
