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

class LtiUsageStatistics extends React.Component {
  render() {
    const { T, row, validationErrors } = this.props;
    return (
      <AdminFormField
        key="systemId"
        entity="jobs"
        field="systemId"
        value={row.systemId}
        invalid={validationErrors.systemId}
        required={true}
        T={T}
      />
    );
  }
}

LtiUsageStatistics.propTypes = {
  T: PropTypes.object.isRequired,
  row: PropTypes.object.isRequired,
  validationErrors: PropTypes.object.isRequired,
};

const validator = (form, T) => {
  if (!form.systemId) {
    const params = { field: T.t(`adminPage.jobs.fieldName.systemId`) };
    return { validationErrors: { systemId: T.t('adminForm.validation.fieldIsRequired', params) } };
  }
  return { data: { systemId: form.systemId } };
};

export default {
  id: 'ltiUsageStatisticsJob',
  component: LtiUsageStatistics,
  validator: validator,
};
