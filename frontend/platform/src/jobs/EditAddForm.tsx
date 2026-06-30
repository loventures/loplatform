/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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
import Polyglot from 'node-polyglot';
import React, { useEffect, useState } from 'react';
import { FormText, InputGroupText } from 'reactstrap';

import { AdminFormCheck, AdminFormField } from '../components/adminForm';
import { inCurrTimeZone } from '../services/moment';
import jobTypes, { JobRow, ValidationErrors } from './jobTypes';

interface EditAddFormProps {
  T: Polyglot;
  row: JobRow;
  validationErrors: ValidationErrors;
  type: string | null;
  isEmailJob: boolean;
}

const EditAddForm: React.FC<EditAddFormProps> = ({ T, row, validationErrors, type, isEmailJob }) => {
  const [help, setHelp] = useState(false);
  const [scheduleValidationErr, setScheduleValidationErr] = useState<string | null>(null);
  const [nextDate, setNextDate] = useState<Date | null>(null);
  const [schedule, setSchedule] = useState<string | undefined>();

  useEffect(() => {
    if (row.schedule) {
      const data = { schedule: row.schedule };
      axios
        .post('/api/v2/jobs/validateCron', data)
        .then(res => {
          setNextDate(new Date(res.data));
        })
        .catch(err => {
          console.log(err);
          setScheduleValidationErr(T.t('adminPage.jobs.fieldName.schedule.invalid'));
          setNextDate(null);
        });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const onScheduleChange = (evt: React.ChangeEvent<HTMLInputElement>) => setSchedule(evt.target.value);

  const onScheduleBlur = () => {
    const data = {
      schedule: schedule || row.schedule,
    };
    axios
      .post('/api/v2/jobs/validateCron', data)
      .then(res => {
        setScheduleValidationErr(null);
        setNextDate(res.data && new Date(res.data));
      })
      .catch(err => {
        console.log(err);
        setScheduleValidationErr(T.t('adminPage.jobs.fieldName.schedule.invalid'));
        setNextDate(null);
      });
  };

  const toggleHelp = () => setHelp(h => !h);

  const renderName = () => {
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

  const getHelp = () => {
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

  const getScheduleAddOn = () => {
    return (
      <InputGroupText
        className="clickable"
        onClick={toggleHelp}
        addonType="append"
      >
        <span className="material-icons md-18">help</span>
      </InputGroupText>
    );
  };

  const renderSchedule = () => {
    const field = 'schedule';
    const dateTimeFormat = T.t('format.dateTime.full');
    const nextRun = nextDate && inCurrTimeZone(moment(nextDate)).format(dateTimeFormat);
    const nextDateHelp = nextDate ? (
      <FormText id="next-run">{`${T.t('adminPage.jobs.schedule.nextRun', { nextRun })}`}</FormText>
    ) : null;
    const scheduleExamples = help && getHelp();
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
        addOn={getScheduleAddOn()}
        onBlur={onScheduleBlur}
        onChange={onScheduleChange}
        T={T}
      />
    );
  };

  const renderStatus = () => {
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

  const renderEmailAddresses = () => {
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

  const entry = type ? jobTypes[type] : undefined;
  const Component = entry && entry.component;
  return (
    <React.Fragment>
      {row.id && <div className="entity-id">{row.id}</div>}
      {renderName()}
      {renderSchedule()}
      {renderStatus()}
      {isEmailJob && renderEmailAddresses()}
      {Component && (
        <Component
          row={row}
          T={T}
          validationErrors={validationErrors}
        />
      )}
    </React.Fragment>
  );
};

export default EditAddForm;
