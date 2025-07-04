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

import { AdminFormField } from '../../components/adminForm';

class InstructorMetricsReport extends React.Component {
  render() {
    const { T, row, validationErrors } = this.props;
    const excludeEmailsHelp = (
      <FormText>{T.t('adminPage.jobs.fieldName.excludeEmails.help')}</FormText>
    );
    const excludeEmailDomainsHelp = (
      <FormText>{T.t('adminPage.jobs.fieldName.excludeEmailDomains.help')}</FormText>
    );

    return (
      <React.Fragment>
        <AdminFormField
          key="excludeEmails"
          entity="jobs"
          field="excludeEmails"
          value={row.excludeEmails && row.excludeEmails.toString()}
          invalid={validationErrors.excludeEmails}
          help={excludeEmailsHelp}
          T={T}
        />
        <AdminFormField
          key="excludeEmailDomains"
          entity="jobs"
          field="excludeEmailDomains"
          value={row.excludeEmailDomains && row.excludeEmailDomains.toString()}
          invalid={validationErrors.excludeEmailDomains}
          help={excludeEmailDomainsHelp}
          T={T}
        />
      </React.Fragment>
    );
  }
}

InstructorMetricsReport.propTypes = {
  T: PropTypes.object.isRequired,
  row: PropTypes.object.isRequired,
  validationErrors: PropTypes.object.isRequired,
};

const string2Array = string => {
  return string ? string.split(',').map(e => e.trim()) : [];
};

const validator = form => {
  return {
    data: {
      excludeEmails: string2Array(form.excludeEmails),
      excludeEmailDomains: string2Array(form.excludeEmailDomains),
    },
  };
};

export default {
  id: 'instructorMetricsReport',
  component: InstructorMetricsReport,
  validator: validator,
};
