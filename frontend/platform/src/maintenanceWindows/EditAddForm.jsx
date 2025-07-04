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
import React from 'react';

import { AdminFormCheck, AdminFormDateTime, AdminFormField } from '../components/adminForm';
import { inCurrTimeZone } from '../services/moment.js';

export default class EditAddForm extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      duration: props.row.duration,
      startTime: props.row.startTime,
    };
  }

  renderStartTime = (validationErrors, row) => {
    const { T } = this.props;
    const field = 'startTime';
    return (
      <AdminFormDateTime
        key={field}
        required={true}
        field={field}
        value={row[field]}
        entity="maintenanceWindows"
        invalid={validationErrors[field]}
        T={T}
        onChange={d => this.setState({ startTime: d })}
      />
    );
  };

  onDurationChange = e => {
    const dur = parseInt(e.target.value, 10);
    this.setState({ duration: dur > 0 ? dur : null });
  };

  formatEndTime = () => {
    const { T } = this.props;
    const { duration, startTime } = this.state;
    if (startTime && duration) {
      const date = inCurrTimeZone(moment(startTime))
        .add(duration, 'minute')
        .format(T.t('format.dateTime.full'));
      return T.t('adminPage.maintenanceWindows.duration.endsOn', { date });
    } else {
      return null;
    }
  };

  renderDuration = (validationErrors, row) => {
    const { T } = this.props;
    const field = 'duration';
    return (
      <AdminFormField
        key={field}
        entity="maintenanceWindows"
        addOn={T.t('adminPage.maintenanceWindows.fieldLabel.minutes')}
        type="number"
        field={field}
        value={row[field] ? row[field].toString() : ''}
        required={true}
        invalid={validationErrors[field]}
        help={this.formatEndTime(row)}
        T={T}
        onChange={this.onDurationChange}
      />
    );
  };

  renderStatus = (validationErrors, row) => {
    const { T } = this.props;
    return (
      <AdminFormCheck
        entity="maintenanceWindows"
        field="status"
        value={!row.disabled}
        label={T.t('adminPage.maintenanceWindows.status.active')}
        T={T}
      />
    );
  };

  render() {
    const { row, validationErrors } = this.props;
    return (
      <React.Fragment>
        {this.renderStartTime(validationErrors, row)}
        {this.renderDuration(validationErrors, row)}
        {this.renderStatus(validationErrors, row)}
      </React.Fragment>
    );
  }
}
