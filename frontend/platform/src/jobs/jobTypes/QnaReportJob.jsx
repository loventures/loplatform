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
import { FormText } from 'reactstrap';

import { AdminFormDateTime, AdminFormField } from '../../components/adminForm';

class QnaReportJob extends React.Component {
  render() {
    const { T, row, validationErrors } = this.props;
    const sectionIdHelp = (
      <FormText>{T.t('adminPage.jobs.fieldName.sectionIdPrefix.help')}</FormText>
    );
    return (
      <React.Fragment>
        <AdminFormDateTime
          key="startTime"
          entity="jobs"
          field="startTime"
          value={row.startTime && row.startTime.toString()}
          invalid={validationErrors.startTime}
          T={T}
        />
        <AdminFormDateTime
          key="endTime"
          entity="jobs"
          field="endTime"
          value={row.endTime && row.endTime.toString()}
          invalid={validationErrors.endTime}
          T={T}
        />
        <AdminFormField
          key="sectionIdPrefix"
          entity="jobs"
          field="sectionIdPrefix"
          value={row.sectionIdPrefix && row.sectionIdPrefix.toString()}
          invalid={validationErrors.sectionIdPrefix}
          help={sectionIdHelp}
          T={T}
        />
      </React.Fragment>
    );
  }
}

QnaReportJob.propTypes = {
  T: PropTypes.object.isRequired,
  row: PropTypes.object.isRequired,
  validationErrors: PropTypes.object.isRequired,
};

const validator = form => {
  return {
    data: {
      startTime: form.startTime || null,
      endTime: form.endTime || null,
      sectionIdPrefix: form.sectionIdPrefix || null,
    },
  };
};

export default {
  id: 'qnaReportJob',
  component: QnaReportJob,
  validator: validator,
};
