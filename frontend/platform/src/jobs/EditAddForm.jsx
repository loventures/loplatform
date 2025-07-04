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
import moment from 'moment-timezone';
import PropTypes from 'prop-types';
import React from 'react';
import { FormText, InputGroupText } from 'reactstrap';

import { AdminFormCheck, AdminFormField } from '../components/adminForm';
import { inCurrTimeZone } from '../services/moment.js';
import jobTypes from './jobTypes';

class EditAddForm extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      help: false,
      scheduleValidationErr: null,
      nextDate: null,
      ready: false,
    };
  }

  componentDidMount() {
    const { row, T } = this.props;
    if (row.schedule) {
      const data = { schedule: row.schedule };
      axios
        .post('/api/v2/jobs/validateCron', data)
        .then(res => {
          this.setState({ nextDate: new Date(res.data) });
        })
        .catch(err => {
          console.log(err);
          this.setState({
            scheduleValidationErr: T.t('adminPage.jobs.fieldName.schedule.invalid'),
            nextDate: null,
          });
        });
    }
  }

  renderName = () => {
    const { T, row, validationErrors } = this.props;
    const field = 'name';
    return (
      <AdminFormField
        key={field}
        entity="jobs"
        field={field}
        value={row[field]}
        required={true}
        invalid={validationErrors[field]}
        T={T}
      />
    );
  };

  onScheduleChange = evt => this.setState({ schedule: evt.target.value });

  renderSchedule = () => {
    const { T, row, validationErrors } = this.props;
    const { help, scheduleValidationErr, nextDate } = this.state;
    const field = 'schedule';
    const dateTimeFormat = T.t('format.dateTime.full');
    const nextRun = nextDate && inCurrTimeZone(moment(nextDate)).format(dateTimeFormat);
    const nextDateHelp = nextDate ? (
      <FormText id="next-run">{`${T.t('adminPage.jobs.schedule.nextRun', { nextRun })}`}</FormText>
    ) : null;
    const scheduleExamples = help && this.getHelp();
    const scheduleHelp = (
      <React.Fragment>
        {scheduleExamples}
        {nextDateHelp}
      </React.Fragment>
    );
    return (
      <AdminFormField
        key={field}
        entity="jobs"
        field={field}
        value={row[field]}
        required={true}
        invalid={scheduleValidationErr || validationErrors[field]}
        help={scheduleHelp}
        addOn={this.getScheduleAddOn()}
        onBlur={this.onScheduleBlur}
        onChange={this.onScheduleChange}
        T={T}
      />
    );
  };

  getHelp = () => {
    const { T } = this.props;
    const examples = ['everyDay', 'everyMonday', 'firstOfMonth', 'firstOfTwoMonths', 'manual'];
    return examples.map(type => {
      return (
        <FormText key={type}>
          <code>{T.t(`adminPage.jobs.schedule.examples.${type}`)}</code>
          {T.t(`adminPage.jobs.schedule.examples.${type}.explanation`)}
        </FormText>
      );
    });
  };

  toggleHelp = () => this.setState({ help: !this.state.help });

  getScheduleAddOn = () => {
    return (
      <InputGroupText
        className="clickable"
        onClick={this.toggleHelp}
        addonType="append"
      >
        <span className="material-icons md-18">help</span>
      </InputGroupText>
    );
  };

  renderStatus = () => {
    const { row, T } = this.props;
    return (
      <AdminFormCheck
        entity="jobs"
        field="active"
        label={T.t('adminPage.jobs.state.active')}
        value={!row.disabled}
        T={T}
      />
    );
  };

  renderEmailAddresses = () => {
    const { T, row, validationErrors } = this.props;
    const field = 'emailAddresses';
    return (
      <AdminFormField
        key={field}
        entity="jobs"
        field={field}
        value={row[field]}
        required={false}
        invalid={validationErrors[field]}
        type="textarea"
        T={T}
      />
    );
  };

  onScheduleBlur = () => {
    const { schedule } = this.state;
    const { T, row } = this.props;
    const data = {
      schedule: schedule || row.schedule,
    };
    axios
      .post('/api/v2/jobs/validateCron', data)
      .then(res => {
        this.setState({
          scheduleValidationErr: null,
          nextDate: res.data && new Date(res.data),
        });
      })
      .catch(err => {
        console.log(err);
        this.setState({
          scheduleValidationErr: T.t('adminPage.jobs.fieldName.schedule.invalid'),
          nextDate: null,
        });
      });
  };

  render() {
    const { type } = this.props;
    const { T, row, validationErrors, isEmailJob } = this.props;
    const Component = jobTypes[type] && jobTypes[type].component;
    const props = (jobTypes[type] && jobTypes[type].props) || {};
    return (
      <React.Fragment>
        {row.id && <div className="entity-id">{row.id}</div>}
        {this.renderName()}
        {this.renderSchedule()}
        {this.renderStatus()}
        {isEmailJob && this.renderEmailAddresses()}
        {Component && (
          <Component
            row={row}
            T={T}
            validationErrors={validationErrors}
            {...props}
          />
        )}
      </React.Fragment>
    );
  }
}

EditAddForm.propTypes = {
  T: PropTypes.object.isRequired,
  row: PropTypes.object.isRequired,
  validationErrors: PropTypes.object.isRequired,
  isEmailJob: PropTypes.bool.isRequired,
};

export default EditAddForm;
