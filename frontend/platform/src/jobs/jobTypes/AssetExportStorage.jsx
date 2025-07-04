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

import { AdminFormField } from '../../components/adminForm';

class AssetExportStorage extends React.Component {
  render() {
    const { T, row, validationErrors } = this.props;
    return ['batchSizeLimit', 'timeLimitInSeconds'].map(field => {
      return (
        <AdminFormField
          key={field}
          entity="jobs"
          field={field}
          value={row.config && row.config[field] && row.config[field].toString()}
          invalid={validationErrors[field]}
          T={T}
        />
      );
    });
  }
}

AssetExportStorage.propTypes = {
  T: PropTypes.object.isRequired,
  row: PropTypes.object.isRequired,
  validationErrors: PropTypes.object.isRequired,
};

const validator = (form, T) => {
  if (form.batchSizeLimit && Number.isNaN(parseInt(form.batchSizeLimit, 10))) {
    const params = { field: T.t(`adminPage.jobs.fieldName.batchSizeLimit`) };
    return {
      validationErrors: { batchSizeLimit: T.t('adminForm.validation.fieldMustBeValid', params) },
    };
  } else if (form.timeLimitInSeconds && Number.isNaN(parseInt(form.timeLimitInSeconds, 10))) {
    const params = { field: T.t(`adminPage.jobs.fieldName.timeLimitInSeconds`) };
    return {
      validationErrors: {
        timeLimitInSeconds: T.t('adminForm.validation.fieldMustBeValid', params),
      },
    };
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
