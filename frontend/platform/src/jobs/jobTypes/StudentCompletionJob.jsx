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

import { AdminFormCheck, AdminFormField } from '../../components/adminForm';

class StudentCompletionJob extends React.Component {
  render() {
    const { T, row, validationErrors } = this.props;
    return (
      <React.Fragment>
        <AdminFormCheck
          key="returnOnlyCompletedStudents"
          entity="jobs"
          field="returnOnlyCompletedStudents"
          value={row.returnOnlyCompletedStudents}
          invalid={validationErrors.returnOnlyCompletedStudents}
          T={T}
        />
        <AdminFormField
          key="studentEmailAddresses"
          entity="jobs"
          field="studentEmailAddresses"
          value={row.studentEmailAddresses && row.studentEmailAddresses.toString()}
          invalid={validationErrors.studentEmailAddresses}
          T={T}
        />
      </React.Fragment>
    );
  }
}

StudentCompletionJob.propTypes = {
  T: PropTypes.object.isRequired,
  row: PropTypes.object.isRequired,
  validationErrors: PropTypes.object.isRequired,
};

const validator = (form, T) => {
  if (!form.returnOnlyCompletedStudents && !form.studentEmailAddresses) {
    const params = { field: T.t(`adminPage.jobs.fieldName.studentEmailAddresses`) };
    return {
      validationErrors: {
        studentEmailAddresses: T.t('adminPage.jobs.fieldName.studentEmailAddresses', params),
      },
    };
  }
  return {
    data: {
      studentEmailAddresses: form.studentEmailAddresses,
      returnOnlyCompletedStudents: !!form.returnOnlyCompletedStudents,
    },
  };
};

export default {
  id: 'studentCompletionJob',
  component: StudentCompletionJob,
  validator: validator,
};
