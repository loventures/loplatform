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
import LoPropTypes from '../react/loPropTypes';
import * as MainActions from '../redux/actions/MainActions';
import AccessCodeBatches from './AccessCodeBatches';
import AccessCodes from './AccessCodes';
import { IoKeyOutline } from 'react-icons/io5';

class App extends React.Component {
  constructor(props) {
    super(props);
  }

  render() {
    const {
      match: { url },
      translations: T,
    } = this.props;
    return (
      <Switch>
        <Route
          path={url}
          exact
          render={props => (
            <AccessCodeBatches
              history={this.props.history}
              {...props}
            />
          )}
        />
        <CrumbRoute
          path={`${url}/:batchId`}
          render={props => (
            <AccessCodes
              {...props}
              T={T}
            />
          )}
        />
        <Route render={() => <Redirect to="/" />} />
      </Switch>
    );
  }
}

App.propTypes = {
  translations: LoPropTypes.translations,
  setPortalAlertStatus: PropTypes.func.isRequired,
};

function mapStateToProps(state) {
  return {
    translations: state.main.translations,
  };
}

function mapDispatchToProps(dispatch) {
  return bindActionCreators(MainActions, dispatch);
}

const AccessCodesMain = connect(mapStateToProps, mapDispatchToProps)(App);

AccessCodesMain.pageInfo = {
  identifier: 'accessCodes',
  icon: IoKeyOutline,
  link: '/AccessCodes',
  group: 'users',
  right: 'loi.cp.admin.right.AdminRight',
};

export default AccessCodesMain;
