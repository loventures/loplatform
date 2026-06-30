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

import { AdminFormField, AdminFormFile } from '../components/adminForm';
import ReactTable from '../components/reactTable/ReactTable';
import { AdminPage } from '../adminPortal/types';
import { useTranslations } from '../redux/state';
import { trim } from '../services';
import { IoBusinessOutline } from 'react-icons/io5';

const fieldUrl = (id: number) => `/api/v2/subtenants/${id}/icon`;

const columns = [
  { dataField: 'id', isKey: true },
  { dataField: 'tenantId', sortable: true, searchable: true, required: true },
  { dataField: 'name', sortable: true, searchable: true, required: true },
  { dataField: 'shortName', sortable: true, searchable: true, required: false },
  { dataField: 'logo', sortable: false, searchable: false, required: false, hidden: true },
];

const Subtenants: React.FC & AdminPage = () => {
  const T = useTranslations();

  const renderForm = (row: any, validationErrors: any) => {
    return columns
      .filter(x => !x.isKey)
      .map(col => {
        const field = col.dataField;
        if (field === 'logo') {
          return (
            <AdminFormFile
              key={field}
              entity={'subtenants'}
              field={field}
              fieldUrl={fieldUrl(row.id)}
              value={row[field]}
              invalid={validationErrors[field]}
              image={true}
              T={T}
            />
          );
        }
        return (
          <AdminFormField
            key={field}
            entity="subtenants"
            field={field}
            value={row[field]}
            required={col.required}
            autoFocus={field === 'tenantId'}
            invalid={validationErrors[field]}
            T={T}
          />
        );
      });
  };

  const validateForm = (form: any) => {
    const data: Record<string, any> = {
      tenantId: trim(form.tenantId),
      name: trim(form.name),
      shortName: trim(form.shortName),
      logoUpload: trim(form.logoUpload),
    };
    const missing = columns.find(col => col.required && data[col.dataField] === '');
    const params = missing && { field: T.t(`adminPage.subtenants.fieldName.${missing.dataField}`) };
    return missing
      ? {
          validationErrors: {
            [missing.dataField]: T.t('adminForm.validation.fieldIsRequired', params),
          },
        }
      : { data };
  };

  return (
    <ReactTable
      entity="subtenants"
      columns={columns}
      defaultSortField="name"
      defaultSearchField="name"
      renderForm={renderForm}
      validateForm={validateForm}
      translations={T}
    />
  );
};

Subtenants.pageInfo = {
  identifier: 'subtenants',
  icon: IoBusinessOutline,
  link: '/Subtenants',
  group: 'integrations',
  right: 'loi.cp.admin.right.AdminRight',
  entity: 'subtenants',
};

export default Subtenants;
