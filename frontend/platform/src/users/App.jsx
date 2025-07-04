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
import React from 'react';
import { connect } from 'react-redux';
import { Redirect, Route, Switch } from 'react-router-dom';
import { bindActionCreators } from 'redux';

import CrumbRoute from '../components/crumbRoute';
import CourseSections from '../groups/courseSections/App';
import * as MainActions from '../redux/actions/MainActions';
import Users from './Users';
import { IoPersonOutline } from 'react-icons/io5';

class UsersPage extends React.Component {
  render() {
    const {
      match: { url },
      translations: T,
      lo_platform,
      setPortalAlertStatus,
      history,
    } = this.props;
    return (
      <Switch>
        <Route
          path={url}
          exact
          render={props => (
            <Users
              {...props}
              history={history}
              translations={T}
              lo_platform={lo_platform}
              setPortalAlertStatus={setPortalAlertStatus}
            />
          )}
        />
        <CrumbRoute
          path={`${url}/:userId/CourseSections`}
          render={props => (
            <CourseSections
              {...props}
              history={history}
              customFilters={[
                {
                  property: 'user_id',
                  operator: 'eq',
                  value: props.match.params.userId,
                  prefilter: true,
                },
              ]}
              user={props.match.params.userId}
            />
          )}
        />
        <Route render={() => <Redirect to="/" />} />
      </Switch>
    );
  }
}

UsersPage.propTypes = {
  match: PropTypes.object.isRequired,
  translations: PropTypes.object.isRequired,
};

function mapStateToProps(state) {
  return {
    translations: state.main.translations,
    lo_platform: state.main.lo_platform,
  };
}

function mapDispatchToProps(dispatch) {
  return bindActionCreators(MainActions, dispatch);
}

const UsersMain = connect(mapStateToProps, mapDispatchToProps)(UsersPage);

UsersMain.pageInfo = {
  identifier: 'users',
  icon: IoPersonOutline,
  link: '/Users',
  group: 'users',
  right: 'loi.cp.admin.right.UserAdminRight',
  entity: 'users',
};

export default UsersMain;
