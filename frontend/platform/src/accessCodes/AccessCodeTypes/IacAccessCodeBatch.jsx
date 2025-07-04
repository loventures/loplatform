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
import { asjax } from '../../services';
import AccessCodeBatch from './AccessCodeBatch';

class IacAccessCodeBatch extends React.Component {
  renderISBN = () => {
    const { T } = this.props;
    return (
      <AdminFormField
        entity="accessCodes"
        field="isbn"
        required
        T={T}
      />
    );
  };

  render() {
    return (
      <AccessCodeBatch
        {...this.props}
        type="entitlementAccessCodeBatch"
        componentIdentifier="loi.cp.iac.IacAccessCodeBatch"
        extraFormFields={this.renderISBN}
      />
    );
  }
}

IacAccessCodeBatch.propTypes = {
  T: PropTypes.object.isRequired,
};

const validateForm = (form, T) => {
  const generating = form.generating;
  const baseReqs = ['name', 'isbn'];
  const data = {
    name: form.name,
    duration: 'unlimited',
    disabled: false,
    redemptionLimit: 1,
    isbn: form.isbn,
  };
  let missing = baseReqs.find(field => !form[field]);
  if (!missing) {
    if (generating) {
      missing = ['prefix', 'quantity'].find(field => !form[field]);
    } else if (!form.guid) {
      missing = 'csv';
    }
  }
  const params = missing && { field: T.t(`adminPage.accessCodes.fieldName.${missing}`) };
  return missing
    ? { validationErrors: { [missing]: T.t('adminForm.validation.fieldIsRequired', params) } }
    : { data };
};

const afterCreateOrUpdate = (res, form) => {
  const generating = form.generating;
  const submitPath = generating ? 'generate' : 'import';
  const url = `/api/v2/accessCodes/batches/${res.data.id}/${submitPath}`;
  const queryString = generating
    ? `?prefix=${form.prefix}&quantity=${form.quantity}`
    : `?upload=${form.guid}&skipHeader=${form.skipFirstRow === 'on'}`;
  return asjax(url + queryString, {}).then(() => res);
};

export default {
  component: IacAccessCodeBatch,
  validateForm: validateForm,
  afterCreateOrUpdate: afterCreateOrUpdate,
  id: 'iacAccessCodeBatch',
};
