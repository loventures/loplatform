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
import { Button } from 'reactstrap';

import { clearSavedTableState } from '../components/reactTable/ReactTable';
import LoPropTypes from '../react/loPropTypes';
import { LogoutUrl } from '../services/URLs';

class Logout extends React.Component {
  onLogout = () => {
    clearSavedTableState();
    axios.post(LogoutUrl, {}).then(() => {
      document.body.classList.add('off');
      setTimeout(() => (window.location.href = '/'), 2000);
    });
  };

  render() {
    return (
      <Button
        color="transparent"
        onClick={this.onLogout}
        className="glyphButton"
      >
        <i
          className="material-icons md-16"
          aria-hidden="true"
        >
          power_settings_new
        </i>
      </Button>
    );
  }
}

Logout.propTypes = {
  T: LoPropTypes.translations,
  history: PropTypes.object.isRequired,
};

function mapStateToProps(state) {
  return {
    T: state.main.translations,
  };
}

export default connect(mapStateToProps, null)(Logout);
