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

import moment from 'moment-timezone';
import React from 'react';

import ReactTable from '../components/reactTable/ReactTable';
import { useTranslations } from '../redux/state';
import { inCurrTimeZone } from '../services/moment';
import EditAddForm from './EditAddForm';

interface MaintenanceWindowRow {
  id: number;
  startTime?: string;
  duration?: number;
  disabled?: boolean;
  [key: string]: unknown;
}

interface MaintenanceWindowForm {
  startTime?: string;
  duration?: string;
  status?: string;
}

const MaintenanceWindows: React.FC = () => {
  const T = useTranslations();

  const formatTime = (t: string) => {
    const dateTimeFormat = T.t('format.dateTime.full');
    return inCurrTimeZone(moment(t)).format(dateTimeFormat);
  };

  const formatDuration = (d: number) => {
    return T.t('adminPage.maintenanceWindows.duration.minutes', { duration: d });
  };

  const formatStatus = (_: unknown, row: MaintenanceWindowRow) => {
    return row.disabled
      ? T.t('adminPage.maintenanceWindows.status.suspended')
      : T.t('adminPage.maintenanceWindows.status.active');
  };

  const columns = [
    { dataField: 'id', isKey: true },
    {
      dataField: 'startTime',
      sortable: true,
      required: true,
      searchable: false,
      dataFormat: formatTime,
    },
    {
      dataField: 'duration',
      sortable: false,
      required: true,
      searchable: false,
      dataFormat: formatDuration,
    },
    {
      dataField: 'status',
      sortable: false,
      required: true,
      searchable: false,
      dataFormat: formatStatus,
    },
  ];

  const renderForm = (row: MaintenanceWindowRow, validationErrors: Record<string, string>) => {
    return (
      <EditAddForm
        row={row}
        validationErrors={validationErrors}
        T={T}
      />
    );
  };

  const validateForm = (form: MaintenanceWindowForm) => {
    const missingF = (field: string) => {
      const params = { field: T.t(`adminPage.maintenanceWindows.fieldName.${field}`) };
      return { validationErrors: { [field]: T.t('adminForm.validation.fieldIsRequired', params) } };
    };
    const invalidF = (field: string) => {
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

  return (
    <ReactTable
      entity="maintenanceWindows"
      columns={columns}
      defaultSortField="startTime"
      defaultSortOrder="desc"
      renderForm={renderForm}
      validateForm={validateForm}
      translations={T}
    />
  );
};

export default MaintenanceWindows;
