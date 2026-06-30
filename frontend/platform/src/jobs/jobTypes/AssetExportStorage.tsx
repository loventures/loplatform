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

import React from 'react';

import { AdminFormField } from '../../components/adminForm';
import { JobTypeProps, JobValidator, ValidatorResult } from './index';

const AssetExportStorage: React.FC<JobTypeProps> = ({ T, row, validationErrors }) => {
  return (
    <>
      {['batchSizeLimit', 'timeLimitInSeconds'].map(field => (
        <AdminFormField
          key={field}
          entity="jobs"
          field={field}
          value={row.config && row.config[field] && row.config[field].toString()}
          invalid={validationErrors[field]}
          T={T}
        />
      ))}
    </>
  );
};

const validator: JobValidator = (form, T) => {
  if (form.batchSizeLimit && Number.isNaN(parseInt(form.batchSizeLimit, 10))) {
    const params = { field: T.t(`adminPage.jobs.fieldName.batchSizeLimit`) };
    const result: ValidatorResult = {
      validationErrors: { batchSizeLimit: T.t('adminForm.validation.fieldMustBeValid', params) },
    };
    return result;
  } else if (form.timeLimitInSeconds && Number.isNaN(parseInt(form.timeLimitInSeconds, 10))) {
    const params = { field: T.t(`adminPage.jobs.fieldName.timeLimitInSeconds`) };
    const result: ValidatorResult = {
      validationErrors: {
        timeLimitInSeconds: T.t('adminForm.validation.fieldMustBeValid', params),
      },
    };
    return result;
  }
  const data = {
    config: {
      batchSizeLimit: form.batchSizeLimit,
      timeLimitInSeconds: form.timeLimitInSeconds,
      runForever: false,
      type: 'loi.cp.job.dataconsistency.DataConsistencyJobConfig',
    },
  };
  return { data };
};

export default {
  id: 'assetExportStorageJob',
  component: AssetExportStorage,
  validator: validator,
};
