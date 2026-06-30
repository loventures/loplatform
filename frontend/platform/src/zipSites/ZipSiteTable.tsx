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

import React, { useState } from 'react';
import { useDispatch } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import _ from 'underscore';

import { AdminFormCheck, AdminFormField, AdminFormFile } from '../components/adminForm';
import ReactTable, { clearSavedTableState } from '../components/reactTable/ReactTable';
import { setPortalAlertStatus } from '../redux/actions/MainActions';
import { useTranslations } from '../redux/state';

const Columns = [
  { dataField: 'id', isKey: true },
  {
    dataField: 'name',
    sortable: true,
    searchable: true,
    required: true,
    filterable: true,
    width: '33%',
  },
  { dataField: 'path', sortable: false, searchable: true, required: true, filterable: true },
];
const Entity = 'zipSites';

const ZipSiteTable: React.FC = () => {
  const T = useTranslations();
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const [uploading, setUploading] = useState(false);

  const _navToRevision = (row: any) => {
    clearSavedTableState('zipSites.revision');
    navigate(`/ZipSites/${row.id}`);
    return new Promise(res => res(false));
  };

  const _getButtons = (row: any) => [
    {
      name: 'open',
      iconName: 'open_in_browser',
      href: row ? row.path : '/',
      target: '_top',
      disabled: row && row.disabled,
    },
    {
      name: 'revisions',
      iconName: 'history',
      onClick: _navToRevision,
    },
  ];

  const _viewSite = ({ disabled, path }: any) => {
    if (!disabled) window.top!.location.href = path;
  };

  const _renderForm = (row: any, validationErrors: any) => {
    return (
      <React.Fragment>
        <AdminFormField
          required
          autoFocus
          field="name"
          value={row.name}
          entity={Entity}
          invalid={validationErrors.name}
          T={T}
        />
        <AdminFormField
          required
          field="path"
          value={row.path}
          entity={Entity}
          help={T.t('adminPage.zipSites.fieldHelp.path')}
          invalid={validationErrors.path}
          T={T}
        />
        <AdminFormFile
          required={!row.id}
          field="site"
          entity={Entity}
          accept={['.zip']}
          invalid={validationErrors.site}
          onChange={({ uploading }: { uploading: boolean }) => setUploading(uploading)}
          T={T}
        />
        <AdminFormCheck
          field="active"
          entity={Entity}
          value={!row.disabled}
          T={T}
        />
      </React.Fragment>
    );
  };

  const _validateSite = () => true;

  const _validateForm = (form: any, row: any) => {
    const missingField = (f: string) =>
      T.t('adminForm.validation.fieldIsRequired', {
        field: T.t(`adminPage.zipSites.fieldName.${f}`),
      });

    const missing = _.object(['name', 'path'].filter(f => !form[f]).map(f => [f, missingField(f)]));

    const siteMissing =
      uploading || (!row.id && !form.siteUpload)
        ? {
            site: T.t(`adminForm.validation.fieldIs${uploading ? 'Uploading' : 'Required'}`, {
              field: T.t('adminPage.zipSites.fieldName.site'),
            }),
          }
        : {};

    const pathValid =
      !form.path || /^[/].*[^/]$/.test(form.path)
        ? {}
        : {
            path: T.t('adminForm.validation.fieldMustBeValid', {
              field: T.t('adminPage.zipSites.fieldName.path'),
            }),
          };

    const siteValid =
      !form.site || _validateSite()
        ? {}
        : { site: T.t('adminForm.zipSites.field.site.badFile') };

    const validationErrors = {
      ...missing,
      ...siteMissing,
      ...pathValid,
      ...siteValid,
    };

    const data = {
      name: form.name,
      path: form.path,
      site: form.siteUpload,
      disabled: form.active !== 'on',
    };

    return _.isEmpty(validationErrors) ? { data } : { validationErrors };
  };

  const trClassFormat = (row: any) => (row.disabled ? 'row-disabled' : '');

  return (
    <ReactTable
      entity={Entity}
      columns={Columns}
      defaultSortField="name"
      defaultSearchField="name"
      getButtons={_getButtons}
      openRow={_viewSite}
      renderForm={_renderForm}
      trClassFormat={trClassFormat}
      validateForm={_validateForm}
      setPortalAlertStatus={(error, success, message) =>
        dispatch(setPortalAlertStatus(error, success, message))
      }
      translations={T}
    />
  );
};

export default ZipSiteTable;
