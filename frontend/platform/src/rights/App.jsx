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
import { bindActionCreators } from 'redux';

import * as MainActions from '../redux/actions/MainActions';
import RightsTree from './RightsTree';
import { PiGavel, PiGavelLight } from 'react-icons/pi';

class App extends React.Component {
  render() {
    return (
      <RightsTree
        {...this.props}
        rolesUrl="/api/v2/roles"
        rightTreeUrl="/api/v2/rights"
        rightsUrl="/api/v2/rights/all"
        postUrl="/api/v2/rights"
      />
    );
  }
}

function mapStateToProps(state) {
  return {
    translations: state.main.translations,
  };
}

function mapDispatchToProps(dispatch) {
  return bindActionCreators(MainActions, dispatch);
}

const Rights = connect(mapStateToProps, mapDispatchToProps)(App);

Rights.pageInfo = {
  identifier: 'rights',
  icon: PiGavelLight,
  link: '/Rights',
  group: 'users',
  right: 'loi.cp.admin.right.AdminRight',
};

export default Rights;
