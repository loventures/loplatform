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

import React from 'react';
import { connect } from 'react-redux';
import { Route, Switch } from 'react-router-dom';
import { bindActionCreators } from 'redux';

import CrumbRoute from '../components/crumbRoute';
import * as MainActions from '../redux/actions/MainActions';
import RevisionTable from './RevisionTable';
import ZipSiteTable from './ZipSiteTable';
import { IoFolderOpenOutline } from 'react-icons/io5';

function mapStateToProps(state) {
  return {
    translations: state.main.translations,
  };
}

function mapDispatchToProps(dispatch) {
  return bindActionCreators(MainActions, dispatch);
}

const ZipSites = connect(
  mapStateToProps,
  mapDispatchToProps
)(({ match: { path }, setPortalAlertStatus, translations: T }) => (
  <Switch>
    <Route
      exact
      path={path}
      render={() => (
        <ZipSiteTable
          setPortalAlertStatus={setPortalAlertStatus}
          translations={T}
        />
      )}
    />

    <CrumbRoute
      exact
      path={path + '/:siteId'}
      render={props => (
        <RevisionTable
          setLastCrumb={props.setLastCrumb}
          siteId={Number.parseInt(props.match.params.siteId, 10)}
          setPortalAlertStatus={setPortalAlertStatus}
          translations={T}
        />
      )}
    />
  </Switch>
));

ZipSites.pageInfo = {
  identifier: 'zipSites',
  link: '/ZipSites',
  icon: IoFolderOpenOutline,
  group: 'media',
  right: 'loi.cp.zip.ZipSiteAdminRight',
  entity: 'zipSites',
};

export default ZipSites;
