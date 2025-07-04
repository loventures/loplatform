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

import axios from 'axios';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Redirect, Route, Switch } from 'react-router-dom';
import { bindActionCreators } from 'redux';

import Announcements from '../../announcements';
import CrumbRoute from '../../components/crumbRoute';
import * as MainActions from '../../redux/actions/MainActions';
import Configurations from '../Configurations';
import Enrollments from '../Enrollments';
import CourseSections from './CourseSections';
import { IoSchoolOutline } from 'react-icons/io5';

const Identifier = 'courseSections';

class App extends React.Component {
  componentDidMount() {
    const { customFilters, setLastCrumb, translations: T } = this.props;
    const userFilter = customFilters && customFilters.find(filter => filter.property === 'user_id');
    const userId = userFilter && userFilter.value;
    if (userId) {
      axios.get(`/api/v2/users/${userId}`).then(res => {
        const params = { fullName: res.data.fullName };
        setLastCrumb(T.t('adminPage.courseSections.name.withUser', params));
      });
    }
  }
  render() {
    const {
      match: { url },
      translations: T,
      readOnly,
      customFilters,
      user,
    } = this.props;
    return (
      <Switch>
        <Route
          path={url}
          exact
          render={props => (
            <CourseSections
              history={this.props.history}
              {...props}
              customFilters={customFilters}
              user={user}
              readOnly={readOnly}
            />
          )}
        />
        <CrumbRoute
          path={`${url}/:courseId/Configurations`}
          render={props => (
            <Configurations
              {...props}
              T={T}
              controllerValue="courseSections"
            />
          )}
        />
        <CrumbRoute
          path={`${url}/:courseId/Enrollments`}
          render={props => (
            <Enrollments
              {...props}
              T={T}
              sudoUrl={course => course.url}
              controllerValue="courseSections"
              includeRights={true}
              readOnly={readOnly}
            />
          )}
        />
        <CrumbRoute
          path={`${url}/:courseId/Announcements`}
          render={props => (
            <Announcements
              {...props}
              T={T}
              controllerValue="courseSections"
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
  customFilters: PropTypes.array,
  user: PropTypes.number,
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

const CourseSectionsMain = connect(mapStateToProps, mapDispatchToProps)(App);

CourseSectionsMain.pageInfo = {
  identifier: Identifier,
  icon: IoSchoolOutline,
  link: '/CourseSections',
  group: 'courses',
  right: 'loi.cp.course.right.ManageCoursesReadRight',
  entity: 'courseSections',
};

export default CourseSectionsMain;
