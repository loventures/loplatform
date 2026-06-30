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

import { AxiosResponse } from 'axios';
import Polyglot from 'node-polyglot';
import React from 'react';

import { AdminFormField } from '../../components/adminForm';
import { asjax } from '../../services';
import AccessCodeBatch, {
  AccessCodeBatchType,
  AccessCodeForm,
  ValidateFormResult,
} from './AccessCodeBatch';

interface IacAccessCodeBatchProps {
  T: Polyglot;
  [key: string]: unknown;
}

const IacAccessCodeBatch: React.FC<IacAccessCodeBatchProps> = props => {
  const { T } = props;
  const renderISBN = () => {
    return (
      <AdminFormField
        entity="accessCodes"
        field="isbn"
        required
        T={T}
      />
    );
  };

  return (
    <AccessCodeBatch
      {...(props as any)}
      type="entitlementAccessCodeBatch"
      componentIdentifier="loi.cp.iac.IacAccessCodeBatch"
      extraFormFields={renderISBN}
    />
  );
};

const validateForm = (form: AccessCodeForm, T: Polyglot): ValidateFormResult => {
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
    ? {
        validationErrors: {
          [missing]: T.t('adminForm.validation.fieldIsRequired', params || {}),
        },
      }
    : { data };
};

const afterCreateOrUpdate = (res: AxiosResponse, form: AccessCodeForm): Promise<AxiosResponse> => {
  const generating = form.generating;
  const submitPath = generating ? 'generate' : 'import';
  const url = `/api/v2/accessCodes/batches/${res.data.id}/${submitPath}`;
  const queryString = generating
    ? `?prefix=${form.prefix}&quantity=${form.quantity}`
    : `?upload=${form.guid}&skipHeader=${form.skipFirstRow === 'on'}`;
  return asjax(url + queryString, {}).then(() => res);
};

const IacAccessCodeBatchType: AccessCodeBatchType = {
  component: IacAccessCodeBatch,
  validateForm: validateForm,
  afterCreateOrUpdate: afterCreateOrUpdate,
  id: 'iacAccessCodeBatch',
};

export default IacAccessCodeBatchType;
