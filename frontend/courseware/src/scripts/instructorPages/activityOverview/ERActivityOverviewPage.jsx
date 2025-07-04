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

import ERContentContainer from '../../landmarks/ERContentContainer';
import { omit } from 'lodash';
import { withTranslation } from '../../i18n/translationContext';
import BasicList from '../../listComponents/BasicList';
import { createLoaderComponent } from '../../utilities/withLoader';
import * as React from 'react';
import { connect } from 'react-redux';

import OverviewPage from './components/OverviewPage';
import { searchByProps, sortByProps } from './config';
import {
  paginateWithWorkActionCreator,
  paginateWithoutWorkActionCreator,
  searchWithWorkActionCreator,
  searchWithoutWorkActionCreator,
  sortWithWorkActionCreator,
  sortWithoutWorkActionCreator,
} from './services/listActions';
import { loadActivityOverviewActionCreator } from './services/loadActions';
import {
  selectActivityOverviewLoaderComponent,
  selectActivityOverviewPageComponent,
} from './services/selectors';
import ERNonContentTitle from '../../commonPages/contentPlayer/ERNonContentTitle';

const AssessmentOverviewLoader = createLoaderComponent(
  selectActivityOverviewLoaderComponent,
  ({ contentId }) => loadActivityOverviewActionCreator(contentId),
  'AssessmentOverviewLoader'
);

const OverviewListHeader = withTranslation(({ translate, title, icon }) => (
  <div className="card-header">
    <div className="flex-row-content">
      <span className="circle-badge badge-primary">
        <span className={icon}></span>
      </span>
      <h3 className="h5 flex-col-fluid">{translate(title)}</h3>
    </div>
  </div>
));

const WithWorkList = ({
  children,
  contentId,
  withWorkState,
  observation,
  sortWithWork,
  searchWithWork,
  paginateWithWork,
}) => (
  <BasicList
    title={
      !observation
        ? 'OVERVIEW_ASSESSMENT_STUDENT_WITH_WORK'
        : 'OVERVIEW_AUTHENTIC_ASSESSMENT_STUDENT_WITH_WORK'
    }
    Header={OverviewListHeader}
    icon="icon-users"
    emptyMessage="OVERVIEW_NO_STUDENTS_STARTED"
    filteredMessage="OVERVIEW_ALL_STUDENTS_FILTERED"
    searchByProps={searchByProps}
    sortByProps={sortByProps}
    listState={withWorkState}
    sortAction={c => sortWithWork(contentId, c)}
    searchAction={(s, c) => searchWithWork(contentId, s, c)}
    paginateAction={p => paginateWithWork(contentId, p)}
  >
    {children}
  </BasicList>
);

const WithoutWorkList = ({
  children,
  contentId,
  withoutWorkState,
  observation,
  sortWithoutWork,
  searchWithoutWork,
  paginateWithoutWork,
}) => (
  <BasicList
    title={
      !observation
        ? 'OVERVIEW_ASSESSMENT_STUDENT_WITHOUT_WORK'
        : 'OVERVIEW_AUTHENTIC_ASSESSMENT_STUDENT_WITHOUT_WORK'
    }
    Header={OverviewListHeader}
    icon="icon-user-cancel"
    emptyMessage="OVERVIEW_NO_STUDENTS_UNSTARTED"
    filteredMessage="OVERVIEW_ALL_STUDENTS_FILTERED"
    searchByProps={searchByProps}
    sortByProps={omit(sortByProps, 'SORT_SUBMISSION_DATE')}
    listState={withoutWorkState}
    sortAction={c => sortWithoutWork(contentId, c)}
    searchAction={(s, c) => searchWithoutWork(contentId, s, c)}
    paginateAction={p => paginateWithoutWork(contentId, p)}
  >
    {children}
  </BasicList>
);

const ERActivityOverviewPage = ({ translate, content, ...props }) => (
  <ERContentContainer title={translate('INSTRUCTOR_ASSIGNMENTS')}>
    <div className="container p-0">
      <div className="card er-content-wrapper mb-2 m-md-3 m-lg-4">
        <div className="card-body">
          <ERNonContentTitle label={translate('INSTRUCTOR_ASSIGNMENTS')} />

          <AssessmentOverviewLoader contentId={props.contentId}>
            <OverviewPage
              content={content}
              listProps={props}
              WithWorkList={WithWorkList}
              WithoutWorkList={WithoutWorkList}
            />
          </AssessmentOverviewLoader>
        </div>
      </div>
    </div>
  </ERContentContainer>
);

export default connect(selectActivityOverviewPageComponent, {
  sortWithWork: sortWithWorkActionCreator,
  sortWithoutWork: sortWithoutWorkActionCreator,
  searchWithWork: searchWithWorkActionCreator,
  searchWithoutWork: searchWithoutWorkActionCreator,
  paginateWithWork: paginateWithWorkActionCreator,
  paginateWithoutWork: paginateWithoutWorkActionCreator,
})(withTranslation(ERActivityOverviewPage));
