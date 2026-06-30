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

import { AdminFormCheck, AdminFormField, AdminFormFile } from '../components/adminForm';
import ReactTable from '../components/reactTable/ReactTable';
import { AdminPage } from '../adminPortal/types';
import { useTranslations } from '../redux/state';
import { TbArrowBounce } from 'react-icons/tb';

const CsvSample = [
  ['/from', '/to'],
  ['/from?param=value', '/elsewhere'],
  ['/external/redirect', 'http://example.org/'],
]
  .map(a => a.map(o => `"${o}"`).join(','))
  .join('\n');

const Redirects: React.FC & AdminPage = () => {
  const T = useTranslations();

  const formatStatus = (_a: any, row: any) => {
    return row.disabled
      ? T.t('adminPage.redirects.status.inactive')
      : T.t('adminPage.redirects.status.active');
  };

  const columns = [
    { dataField: 'id', isKey: true },
    {
      dataField: 'status',
      sortable: false,
      searchable: false,
      dataFormat: formatStatus,
      width: '30%',
    },
    { dataField: 'name', sortable: true, searchable: true, required: true },
  ];

  const renderForm = (row: any, validationErrors: any) => {
    const help = (
      <React.Fragment>
        {T.t('adminPage.redirects.title.csvFormat')}
        <pre className="m-0 p-2 bg-light border border-secondary rounded">{CsvSample}</pre>
      </React.Fragment>
    );
    const csvUrl = `/api/v2/redirects/${row.id}/csv`;
    return [
      <AdminFormField
        key="name"
        entity="redirects"
        field="name"
        value={row.name}
        required
        invalid={validationErrors.name}
        T={T}
      />,
      <AdminFormFile
        key="csv"
        entity="redirects"
        field="csv"
        fieldUrl={csvUrl}
        value={row.csv}
        required={!row.id}
        invalid={validationErrors.csv}
        T={T}
        help={help}
        accept={['.csv']}
      />,
      <AdminFormCheck
        key="active"
        entity="redirects"
        field="active"
        value={!row.disabled}
        T={T}
      />,
    ];
  };

  const validateForm = (form: any, row: any) => {
    const data = {
      name: (form.name || '').trim(),
      csvUpload: form.csvUpload,
      disabled: !form.active,
    };
    const missing = !data.name ? 'name' : !row.id && !data.csvUpload ? 'csv' : null;
    const params = missing && { field: T.t(`adminPage.redirects.fieldName.${missing}`) };
    return missing
      ? { validationErrors: { [missing]: T.t('adminForm.validation.fieldIsRequired', params || undefined) } }
      : { data };
  };

  return (
    <ReactTable
      entity="redirects"
      columns={columns}
      defaultSortField="name"
      defaultSearchField="name"
      renderForm={renderForm}
      validateForm={validateForm}
      translations={T}
    />
  );
};

Redirects.pageInfo = {
  identifier: 'redirects',
  icon: TbArrowBounce,
  link: '/Redirects',
  group: 'media',
  right: 'loi.cp.redirect.RedirectAdminRight',
  entity: 'redirects',
};

export default Redirects;
