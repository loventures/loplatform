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

class AssessmentStatics extends React.Component {
  render() {
    const { T, row, validationErrors } = this.props;
    return (
      <AdminFormField
        key="assesmentId"
        entity="jobs"
        field="assessmentId"
        value={row.assessmentId && row.assessmentId.toString()}
        invalid={validationErrors.assessmentId}
        T={T}
      />
    );
  }
}

AssessmentStatics.propTypes = {
  T: PropTypes.object.isRequired,
  row: PropTypes.object.isRequired,
  validationErrors: PropTypes.object.isRequired,
};

const validator = (form, T) => {
  if (form.assessmentId && Number.isNaN(parseInt(form.assessmentId, 10))) {
    const params = { field: T.t(`adminPage.jobs.fieldName.assessmentId`) };
    return {
      validationErrors: { assessmentId: T.t('adminForm.validation.fieldMustBeValid', params) },
    };
  }
  return { data: { assessmentId: form.assessmentId } };
};

export default {
  id: 'assessmentStatisticsJob',
  component: AssessmentStatics,
  validator: validator,
};
