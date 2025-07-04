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

import PropTypes from 'prop-types';
import queryString from 'query-string';
import React from 'react';
import { connect } from 'react-redux';
import { Redirect, Route, Switch } from 'react-router-dom';
import { bindActionCreators } from 'redux';

import CrumbRoute from '../components/crumbRoute';
import Configurations from '../groups/Configurations';
import * as MainActions from '../redux/actions/MainActions';
import CourseOfferings from './CourseOfferings';
import Announcements from '../announcements';
import { IoLibraryOutline } from 'react-icons/io5';

const Identifier = 'lwc/courseOfferings';

class App extends React.Component {
  render() {
    const {
      match: { path },
      translations: T,
    } = this.props;
    return (
      <Switch>
        <Route
          path={path}
          exact
          render={props => {
            const parsed = queryString.parse(props.location.search);
            const filtered = parsed && parsed.project;
            const filter = filtered && {
              projectId: parseInt(parsed.project, 10),
            };
            return (
              <CourseOfferings
                history={this.props.history}
                initFilter={filter}
              />
            );
          }}
        />
        <CrumbRoute
          path={`${path}/:courseId/Configurations`}
          render={props => (
            <Configurations
              {...props}
              T={T}
              controllerValue={Identifier}
              warning={T.t('adminPage.lwc/courseOfferings.configWarning')}
            />
          )}
        />
        <CrumbRoute
          path={`${path}/:courseId/Announcements`}
          render={props => (
            <Announcements
              {...props}
              T={T}
              controllerValue={Identifier}
            />
          )}
        />
        <Route render={() => <Redirect to="/" />} />
      </Switch>
    );
  }
}

App.propTypes = {
  match: PropTypes.object.isRequired,
  translations: PropTypes.object.isRequired,
};

function mapStateToProps(state) {
  return {
    translations: state.main.translations,
  };
}

function mapDispatchToProps(dispatch) {
  return bindActionCreators(MainActions, dispatch);
}

const CourseOfferingsMain = connect(mapStateToProps, mapDispatchToProps)(App);

CourseOfferingsMain.pageInfo = {
  identifier: 'courseOfferings',
  icon: IoLibraryOutline,
  link: '/CourseOfferings',
  group: 'courses',
  right: 'loi.cp.admin.right.AdminRight',
  entity: 'lwc/courseOfferings',
};

export default CourseOfferingsMain;
