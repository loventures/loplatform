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
import { Route, Switch } from 'react-router-dom';

import CrumbRoute from '../components/crumbRoute';
import CourseRightsTree from './CourseRightsTree';
import Roster from './Roster';
import UserEnrollments from './UserEnrollments';

class Enrollments extends React.Component {
  componentDidMount() {
    const { match, setLastCrumb, T, controllerValue } = this.props;
    const { courseId } = match.params;
    axios.get(`/api/v2/${controllerValue}/${courseId}`).then(res => {
      setLastCrumb(T.t(`adminPage.${controllerValue}.enrollments.name`, res.data));
    });
  }
  render() {
    const { T, controllerValue, sudoUrl, includeRights, match, readOnly } = this.props;
    return (
      <Switch>
        <Route
          path={match.url}
          exact
          render={props => (
            <Roster
              {...props}
              setTitle={this.setTitle}
              courseId={match.params.courseId}
              controllerValue={controllerValue}
              sudoUrl={sudoUrl}
              includeRights={includeRights}
              readOnly={readOnly}
            />
          )}
        />
        {includeRights && (
          <CrumbRoute
            title={T.t('adminPage.enrollments.rightsTree.name')}
            path={`${match.url}/Rights`}
            exact
            render={props => (
              <CourseRightsTree
                {...props}
                courseId={this.props.match.params.courseId}
              />
            )}
          />
        )}
        <CrumbRoute
          path={`${match.url}/:userId`}
          exact
          render={props => (
            <UserEnrollments
              {...props}
              courseId={this.props.match.params.courseId}
              controllerValue={controllerValue}
            />
          )}
        />
      </Switch>
    );
  }
}

Enrollments.propTypes = {
  T: PropTypes.object.isRequired,
  controllerValue: PropTypes.string.isRequired,
  sudoUrl: PropTypes.func.isRequired,
  includeRights: PropTypes.bool.isRequired,
  match: PropTypes.object.isRequired,
  setLastCrumb: PropTypes.func.isRequired,
  readOnly: PropTypes.bool.isRequired,
};

export default Enrollments;
