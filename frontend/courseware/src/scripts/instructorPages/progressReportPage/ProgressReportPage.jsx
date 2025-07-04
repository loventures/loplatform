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

import { Helmet } from 'react-helmet';
import { withTranslation } from '../../i18n/translationContext';

import PageContainer from '../../landmarks/PageContainer';
import Breadcrumbs from '../../components/breadcrumbs/Breadcrumbs';
import CrumbSegment from '../../components/breadcrumbs/CrumbSegment';

import ProgressReportDownloadButton from './components/ProgressReportDownloadButton';

import ProgressReportTable from './components/ProgressReportTable';
import ProgressReportBreadcrumbs from './components/ProgressReportBreadcrumbs';
import ProgressReportOptions from './components/ProgressReportOptions';
import ProgressReportPagination from './components/ProgressReportPagination';

import ProgressReportLoader from './components/ProgressReportLoader';
import ProgressReportLoadingMessages from './components/ProgressReportLoadingMessages';

const ProgressReportPage = ({ translate }) => (
  <PageContainer
    wide={true}
    renderBreadcrumbs={() => (
      <Breadcrumbs>
        <CrumbSegment>{translate('STUDENT_PROGRESS_PAGE_HEADER')}</CrumbSegment>
      </Breadcrumbs>
    )}
  >
    <Helmet>
      <title>{translate('STUDENT_PROGRESS_PAGE_HEADER')}</title>
    </Helmet>
    <header className="main-content-header">
      <div className="flex-row-content">
        <h1
          id="maincontent"
          tabIndex="-1"
          className="flex-col-fluid"
        >
          {translate('STUDENT_PROGRESS_PAGE_HEADER')}
        </h1>
        <ProgressReportDownloadButton />
      </div>
    </header>

    <div className="card">
      <div className="card-header student-progress-header">
        <ProgressReportBreadcrumbs />
        <ProgressReportOptions />
      </div>

      <ProgressReportLoader>
        <ProgressReportTable />
      </ProgressReportLoader>

      <ProgressReportLoadingMessages className="card-body" />

      <div className="card-footer">
        <ProgressReportPagination />
      </div>
    </div>
  </PageContainer>
);

export default withTranslation(ProgressReportPage);
