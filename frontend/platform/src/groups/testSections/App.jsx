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

import CrumbRoute from '../../components/crumbRoute';
import * as MainActions from '../../redux/actions/MainActions';
import Configurations from '../Configurations';
import Enrollments from '../Enrollments';
import TestSections from './TestSections';
import { PiHardHat } from 'react-icons/pi';

const Identifier = 'testSections';

class App extends React.Component {
  render() {
    const {
      match: { path },
      translations: T,
      readOnly,
    } = this.props;
    return (
      <Switch>
        <Route
          path={path}
          exact
          render={props => {
            const parsed = queryString.parse(props.location.search);
            const adding = parsed && parsed.project;
            const initModal = adding && {
              projectId: parseInt(parsed.project, 10),
            };
            return (
              <TestSections
                initModal={initModal}
                history={this.props.history}
                readOnly={readOnly}
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
            />
          )}
        />
        <CrumbRoute
          path={`${path}/:courseId/Enrollments`}
          render={props => (
            <Enrollments
              {...props}
              T={T}
              sudoUrl={course => course.url}
              controllerValue={Identifier}
              includeRights={true}
              readOnly={readOnly}
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
  readOnly: PropTypes.bool.isRequired,
};

function mapStateToProps(state) {
  const rights = state.main.lo_platform.user.rights;
  const readOnly =
    !rights.includes('loi.cp.admin.right.CourseAdminRight') &&
    !rights.includes('loi.cp.course.right.ManageCoursesAdminRight');
  return {
    translations: state.main.translations,
    readOnly,
  };
}

function mapDispatchToProps(dispatch) {
  return bindActionCreators(MainActions, dispatch);
}

const TestSectionsMain = connect(mapStateToProps, mapDispatchToProps)(App);

TestSectionsMain.pageInfo = {
  identifier: Identifier,
  icon: PiHardHat,
  link: '/TestSections',
  group: 'courses',
  right: 'loi.cp.course.right.ManageCoursesReadRight',
  entity: 'testSections',
};

export default TestSectionsMain;
