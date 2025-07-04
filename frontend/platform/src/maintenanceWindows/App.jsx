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

import moment from 'moment-timezone';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

import ReactTable from '../components/reactTable/ReactTable';
import LoPropTypes from '../react/loPropTypes';
import * as MainActions from '../redux/actions/MainActions';
import { inCurrTimeZone } from '../services/moment.js';
import EditAddForm from './EditAddForm';

class App extends React.Component {
  formatTime = t => {
    const T = this.props.translations;
    const dateTimeFormat = T.t('format.dateTime.full');
    return inCurrTimeZone(moment(t)).format(dateTimeFormat);
  };

  formatDuration = d => {
    const T = this.props.translations;
    return T.t('adminPage.maintenanceWindows.duration.minutes', { duration: d });
  };

  formatStatus = (_, row) => {
    const T = this.props.translations;
    return row.disabled
      ? T.t('adminPage.maintenanceWindows.status.suspended')
      : T.t('adminPage.maintenanceWindows.status.active');
  };

  columns = [
    { dataField: 'id', isKey: true },
    {
      dataField: 'startTime',
      sortable: true,
      required: true,
      searchable: false,
      dataFormat: this.formatTime,
    },
    {
      dataField: 'duration',
      sortable: false,
      required: true,
      searchable: false,
      dataFormat: this.formatDuration,
    },
    {
      dataField: 'status',
      sortable: false,
      required: true,
      searchable: false,
      dataFormat: this.formatStatus,
    },
  ];

  renderForm = (row, validationErrors) => {
    const { translations: T } = this.props;
    return (
      <EditAddForm
        row={row}
        validationErrors={validationErrors}
        T={T}
      />
    );
  };

  validateForm = form => {
    const missingF = field => {
      const T = this.props.translations;
      const params = { field: T.t(`adminPage.maintenanceWindows.fieldName.${field}`) };
      return { validationErrors: { [field]: T.t('adminForm.validation.fieldIsRequired', params) } };
    };
    const invalidF = field => {
      const T = this.props.translations;
      const params = { field: T.t(`adminPage.maintenanceWindows.fieldName.${field}`) };
      return {
        validationErrors: { [field]: T.t('adminForm.validation.fieldMustBeValid', params) },
      };
    };
    if (!form.startTime) {
      return missingF('startTime');
    } else if (!moment(form.startTime).isValid()) {
      return invalidF('startTime');
    } else if (!form.duration) {
      return missingF('duration');
    } else if (isNaN(parseInt(form.duration, 10)) || parseInt(form.duration, 10) < 0) {
      return invalidF('duration');
    }
    const data = {
      startTime: moment(form.startTime).toISOString(),
      duration: parseInt(form.duration, 10),
      disabled: form.status !== 'on',
    };
    return { data };
  };

  render() {
    return (
      <ReactTable
        entity="maintenanceWindows"
        columns={this.columns}
        defaultSortField="startTime"
        defaultSortOrder="desc"
        renderForm={this.renderForm}
        validateForm={this.validateForm}
        translations={this.props.translations}
        setPortalAlertStatus={this.props.setPortalAlertStatus}
      />
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

export default connect(mapStateToProps, mapDispatchToProps)(App);
