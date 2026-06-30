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

import { Moment } from 'moment';
import moment from 'moment-timezone';
import Polyglot from 'node-polyglot';
import React, { useState } from 'react';

import { AdminFormCheck, AdminFormDateTime, AdminFormField } from '../components/adminForm';
import { inCurrTimeZone } from '../services/moment';

interface EditAddFormRow {
  duration?: number | null;
  startTime?: string | null;
  disabled?: boolean;
  [key: string]: unknown;
}

interface EditAddFormProps {
  row: EditAddFormRow;
  validationErrors: Record<string, string>;
  T: Polyglot;
}

const EditAddForm: React.FC<EditAddFormProps> = ({ row, validationErrors, T }) => {
  const [duration, setDuration] = useState<number | null>(row.duration ?? null);
  const [startTime, setStartTime] = useState<Moment | string | null>(row.startTime ?? null);

  const renderStartTime = () => {
    const field = 'startTime';
    return (
      <AdminFormDateTime
        key={field}
        required={true}
        field={field}
        value={row[field] as string | undefined}
        entity="maintenanceWindows"
        invalid={validationErrors[field]}
        T={T}
        onChange={d => setStartTime(d)}
      />
    );
  };

  const onDurationChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const dur = parseInt(e.target.value, 10);
    setDuration(dur > 0 ? dur : null);
  };

  const formatEndTime = () => {
    if (startTime && duration) {
      const date = inCurrTimeZone(moment(startTime))
        .add(duration, 'minute')
        .format(T.t('format.dateTime.full'));
      return T.t('adminPage.maintenanceWindows.duration.endsOn', { date });
    } else {
      return null;
    }
  };

  const renderDuration = () => {
    const field = 'duration';
    return (
      <AdminFormField
        key={field}
        entity="maintenanceWindows"
        addOn={T.t('adminPage.maintenanceWindows.fieldLabel.minutes')}
        type="number"
        field={field}
        value={row[field] ? String(row[field]) : ''}
        required={true}
        invalid={validationErrors[field]}
        help={formatEndTime()}
        T={T}
        onChange={onDurationChange}
      />
    );
  };

  const renderStatus = () => {
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

  return (
    <React.Fragment>
      {renderStartTime()}
      {renderDuration()}
      {renderStatus()}
    </React.Fragment>
  );
};

export default EditAddForm;
