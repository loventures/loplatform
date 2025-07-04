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
import { bindActionCreators } from 'redux';

import * as MainActions from '../redux/actions/MainActions';
import ScrollTopAlert from './ScrollTopAlert';

class AlertBar extends React.Component {
  hideAlert = () => {
    const { setPortalAlertStatus } = this.props;
    setPortalAlertStatus(false, false, '');
  };

  render() {
    const { adminPageError, adminPageSuccess, adminPageMessage } = this.props;
    return (
      (adminPageError || adminPageSuccess) && (
        <div className="container-fluid">
          <ScrollTopAlert
            id="admin-page-alert"
            color={adminPageError ? 'warning' : 'success'}
            toggle={this.hideAlert}
          >
            {adminPageMessage}
          </ScrollTopAlert>
        </div>
      )
    );
  }
}

AlertBar.propTypes = {
  adminPageError: PropTypes.bool,
  adminPageMessage: PropTypes.string,
  adminPageSuccess: PropTypes.bool,
  setPortalAlertStatus: PropTypes.func.isRequired,
};

function mapStateToProps(state) {
  return {
    adminPageError: state.main.adminPageError,
    adminPageMessage: state.main.adminPageMessage,
    adminPageSuccess: state.main.adminPageSuccess,
  };
}

function mapDispatchToProps(dispatch) {
  return bindActionCreators({ ...MainActions }, dispatch);
}

export default connect(mapStateToProps, mapDispatchToProps)(AlertBar);
