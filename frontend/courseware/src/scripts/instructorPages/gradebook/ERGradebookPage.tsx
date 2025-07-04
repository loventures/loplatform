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

import LoNavLink from '../../components/links/LoNavLink';
import GatingEditor from '../../instructorPages/gatingEditorPage/parts/GatingEditor';
import { AccomodationsPage } from '../../instructorPages/gradebook/parts/AccomodationsPage';
import GradebookAssignments from '../../instructorPages/gradebook/parts/GradebookAssignments';
import GradebookDownloadButton from '../../instructorPages/gradebook/parts/GradebookDownloadButton';
import GradebookSyncButton from '../../instructorPages/gradebook/parts/GradebookSyncButton';
import LtiColumnSyncHistory from '../../instructorPages/gradebook/parts/LtiColumnSyncHistory';
import LtiGradeSyncHistory from '../../instructorPages/gradebook/parts/LtiGradeSyncHistory';
import GradebookGradesPage from '../../instructorPages/gradebookGradesPage/GradebookGradesPage';
import ERContentContainer from '../../landmarks/ERContentContainer';
import {
  InstructorGradebookAccommodationsPageLink,
  InstructorGradebookAssignmentsPageLink,
  InstructorGradebookGatingPageLink,
  InstructorGradebookGradesPageLink,
} from '../../utils/pageLinks';
import { useTranslation } from '../../i18n/translationContext';
import GradebookColumnsLoader from '../../loaders/GradebookColumnsLoader';
import { editGatingPolicies, gradebookSync } from '../../utilities/preferences';
import React from 'react';
import { Route, Switch, useParams } from 'react-router';
import ERNonContentTitle from '../../commonPages/contentPlayer/ERNonContentTitle';

const GradebookNav: React.FC = () => {
  const translate = useTranslation();
  return (
    <ul className="nav nav-tabs d-flex">
      <li className="nav-item">
        <LoNavLink
          className="nav-link"
          activeClassName="active"
          to={InstructorGradebookGradesPageLink.toLink()}
          target={undefined}
          disabled={undefined}
        >
          {translate('GRADEBOOK_TAB_GRADES')}
        </LoNavLink>
      </li>
      <li className="nav-item">
        <LoNavLink
          className="nav-link"
          activeClassName="active"
          to={InstructorGradebookAssignmentsPageLink.toLink()}
          target={undefined}
          disabled={undefined}
        >
          {translate('GRADEBOOK_TAB_CATEGORIES')}
        </LoNavLink>
      </li>
      <li className="nav-item">
        <LoNavLink
          className="nav-link"
          activeClassName="active"
          to={InstructorGradebookGatingPageLink.toLink()}
          target={undefined}
          disabled={undefined}
        >
          {translate('GRADEBOOK_TAB_GATING')}
        </LoNavLink>
      </li>
      <li className="nav-item">
        <LoNavLink
          className="nav-link"
          activeClassName="active"
          to={InstructorGradebookAccommodationsPageLink.toLink()}
          target={undefined}
          disabled={undefined}
        >
          {translate('GRADEBOOK_TAB_ACCOMMODATIONS')}
        </LoNavLink>
      </li>
    </ul>
  );
};

type ColumnIdUserId = {
  columnId: string;
  userId: string;
};

type ColumnId = {
  columnId: string;
};

const LtiGradeSyncHistoryPage: React.FC = () => {
  const { columnId, userId } = useParams<ColumnIdUserId>();
  return (
    <div className="col px-4 py-3">
      <GradebookNav />
      <LtiGradeSyncHistory
        columnId={columnId}
        userId={userId}
      />
    </div>
  );
};

const LtiColumnSyncHistoryPage: React.FC = () => {
  const { columnId } = useParams<ColumnId>();
  return (
    <div className="col px-4 py-3">
      <GradebookNav />
      <LtiColumnSyncHistory columnId={columnId} />
    </div>
  );
};

const GatingEditorPage: React.FC = () => {
  return (
    <div className="col px-4 py-3">
      <GradebookNav />
      <GatingEditor />
    </div>
  );
};

const AccommodationsEditorPage: React.FC = () => {
  return (
    <div className="col px-4 py-3">
      <GradebookNav />
      <AccomodationsPage />
    </div>
  );
};

const GradebookHeader: React.FC = () => {
  const translate = useTranslation();
  return (
    <Switch>
      <Route
        exact={true}
        path="/instructor/gradebook/grades"
      >
        <ERNonContentTitle label={translate('GRADEBOOK_HEADER')} />
        <div className="d-flex justify-content-center align-items-center gap-3">
          {gradebookSync && <GradebookSyncButton />}
          <GradebookDownloadButton />
        </div>
      </Route>
      <Route>
        <ERNonContentTitle label={translate('GRADEBOOK_HEADER')} />
      </Route>
    </Switch>
  );
};

const ERGradebookPage: React.FC = () => {
  const translate = useTranslation();
  return (
    <ERContentContainer title={translate('GRADEBOOK_HEADER')}>
      <div className="container p-0">
        <div className="card er-content-wrapper mb-2 m-md-3 m-lg-4">
          <div className="card-body">
            <GradebookHeader />
            <Route
              path="/instructor/gradebook/grades"
              render={() => (
                <Switch>
                  <Route path="/instructor/gradebook/grades/syncHistory/:columnId/:userId">
                    <LtiGradeSyncHistoryPage />
                  </Route>
                  <Route path="/instructor/gradebook/grades/syncHistory/:columnId">
                    <LtiColumnSyncHistoryPage />
                  </Route>
                  <Route>
                    <GradebookColumnsLoader>
                      <div className="col px-4 py-3">
                        <GradebookNav />
                        <GradebookGradesPage />
                      </div>
                    </GradebookColumnsLoader>
                  </Route>
                </Switch>
              )}
            />
            <Route
              path="/instructor/gradebook/assignments"
              render={() => (
                <GradebookColumnsLoader>
                  <div className="col px-4 py-3">
                    <GradebookNav />
                    <GradebookAssignments />
                  </div>
                </GradebookColumnsLoader>
              )}
            />
            {editGatingPolicies && (
              <Route path="/instructor/gradebook/gating">
                <GatingEditorPage />
              </Route>
            )}
            <Route path="/instructor/gradebook/accommodations">
              <AccommodationsEditorPage />
            </Route>
          </div>
        </div>
      </div>
    </ERContentContainer>
  );
};

export default ERGradebookPage;
