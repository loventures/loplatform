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
import { trim } from '../../services';

class NodeAttachmentBlobRefUpgrade extends React.Component {
  render() {
    const { T, row, validationErrors } = this.props;
    return (
      <AdminFormField
        key="domainIds"
        entity="jobs"
        field="domainIds"
        value={row.domainIds && row.domainIds.join(', ')}
        invalid={validationErrors.domainIds}
        help={T.t('adminPage.jobs.help.domainIds')}
        T={T}
      />
    );
  }
}

NodeAttachmentBlobRefUpgrade.propTypes = {
  T: PropTypes.object.isRequired,
  row: PropTypes.object.isRequired,
  validationErrors: PropTypes.object.isRequired,
};

const validator = (form, T) => {
  if (trim(form.domainIds) === '') {
    const params = { field: T.t('adminPage.jobs.fieldName.domainIds') };
    return { validationErrors: { domainIds: T.t('adminForm.validation.fieldIsRequired', params) } };
  }
  return {
    data: {
      domainIds: form.domainIds ? form.domainIds.split(',').map(e => e.trim()) : [],
    },
  };
};

export default {
  id: 'nodeAttachmentBlobRefUpgrade',
  component: NodeAttachmentBlobRefUpgrade,
  validator: validator,
};
