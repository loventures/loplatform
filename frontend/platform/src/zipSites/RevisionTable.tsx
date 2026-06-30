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

import axios from 'axios';
import Polyglot from 'node-polyglot';
import React, { useEffect, useState } from 'react';

import AdminFormFile from '../components/adminForm/AdminFormFile';
import ReactTable from '../components/reactTable/ReactTable';

const Columns = [
  { dataField: 'id', isKey: true },
  { dataField: 'fileName', sortable: true, searchable: true, required: true, width: '50%' },
  { dataField: 'createTime', sortable: true, searchable: false, required: true },
  {
    dataField: 'creator',
    sortable: false,
    searchable: false,
    required: false,
    dataFormat: (u: { fullName: string }) => u.fullName,
  },
];
const Entity = 'zipSites.revision';

interface RevisionTableProps {
  siteId: number;
  setPortalAlertStatus: (error: boolean, success: boolean, message: string) => void;
  translations: Polyglot;
  setLastCrumb: (title: string) => void;
}

const RevisionView: React.FC<RevisionTableProps> = ({
  siteId,
  setPortalAlertStatus,
  translations: T,
  setLastCrumb,
}) => {
  const [activeId, setActiveId] = useState(0);
  const [uploading, setUploading] = useState(false);

  const baseUrl = `/api/v2/zipSites/${siteId}`;

  const _update = () => {
    axios.get(baseUrl).then(res => {
      setActiveId(res.data.siteId);
      setLastCrumb(res.data.name);
    });
  };

  useEffect(() => {
    _update();
  }, []);

  const _activateRevision = (row: any) =>
    axios
      .put(baseUrl, { revision: row.id })
      .then(() => _update())
      .then(() => true); // reload it

  const _getButtons = (row: any) => [
    {
      name: 'activate',
      iconName: 'check',
      disabled: row && row.id === activeId,
      onClick: _activateRevision,
    },
    {
      name: 'download',
      iconName: 'file_download',
      href: row ? `${baseUrl}/revisions/${row.id}/view?download=true` : '/',
      download: true,
    },
    {
      name: 'preview',
      iconName: 'play_arrow',
      href: row ? `${baseUrl}/render?revision=${row.id}` : '/',
      target: '_top',
    },
  ];

  const _canDeleteRow = (row: any) => row && row.id !== activeId;

  const _renderForm = (_row: any, validationErrors: any) => {
    return (
      <AdminFormFile
        required
        field="site"
        entity={Entity}
        accept={['.zip']}
        invalid={validationErrors.site}
        onChange={({ uploading }: { uploading: boolean }) => setUploading(uploading)}
        T={T}
      />
    );
  };

  const _validateForm = (form: any) => {
    const missingUploadMsg = T.t(
      `adminForm.validation.fieldIs${uploading ? 'Uploading' : 'Required'}`,
      { field: T.t('adminPage.zipSites.fieldName.site') }
    );

    return form.siteUpload && !uploading
      ? { data: { site: form.siteUpload } }
      : { validationErrors: { site: missingUploadMsg } };
  };

  const _submitForm = ({ data: { site }, config, create }: any) => {
    if (!create) {
      console.log('update on revision table not supported (!?)');
      return new Promise((_, reject) => reject(null));
    } else {
      const payload = { site };
      return axios.put(baseUrl, payload, config).then(res => {
        _update();
        return res;
      }); // dude where's my tap?
    }
  };

  const trClassFormat = (row: any) => (row.id === activeId ? 'row-active' : '');

  return (
    <ReactTable
      entity={Entity}
      columns={Columns}
      baseUrl={`${baseUrl}/revisions`}
      defaultSortField="createTime"
      defaultSortOrder="desc"
      defaultSearchField="fileName"
      getButtons={_getButtons}
      updateButton={false}
      canDeleteRow={_canDeleteRow}
      renderForm={_renderForm}
      validateForm={_validateForm}
      submitForm={_submitForm}
      trClassFormat={trClassFormat}
      setPortalAlertStatus={setPortalAlertStatus}
      translations={T}
    />
  );
};

export default RevisionView;
