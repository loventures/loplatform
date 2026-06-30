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
import React, { useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import { ButtonDropdown, DropdownItem, DropdownMenu, DropdownToggle } from 'reactstrap';

import { AdminPage } from '../adminPortal/types';
import {
  AdminFormCheck,
  AdminFormField,
  AdminFormFile,
  AdminFormSelect,
} from '../components/adminForm';
import ReactTable from '../components/reactTable/ReactTable';
import { setPortalAlertStatus } from '../redux/actions/MainActions';
import { useTranslations } from '../redux/state';
import { ContentTypeURLEncoded } from '../services';
import { LanguagesUrl, LocalesUrl } from '../services/URLs';
import { IoLanguageOutline } from 'react-icons/io5';

const Languages: React.FC & AdminPage = () => {
  const T = useTranslations();
  const dispatch = useDispatch();

  const [loaded, setLoaded] = useState(false);
  const [downloadOpen, setDownloadOpen] = useState(false);
  const [languages, setLanguages] = useState<any[]>([]);

  useEffect(() => {
    axios
      .get(LocalesUrl)
      .then(res => {
        setLoaded(true);
        setLanguages(res.data.objects);
      })
      .catch(e => {
        console.log(e);
        dispatch(setPortalAlertStatus(true, false, T.t('error.unexpectedError')));
      });
  }, []);

  const formatStatus = (_a: any, row: any) => {
    return row.disabled
      ? T.t('adminPage.languages.status.suspended')
      : T.t('adminPage.languages.status.active');
  };

  const tableColumns: any[] = [
    { dataField: 'id', isKey: true },
    {
      dataField: 'status',
      sortable: false,
      searchable: false,
      width: '15%',
      dataFormat: formatStatus,
    },
    { dataField: 'name', sortable: true, searchable: true, width: '40%' },
    { dataField: 'languageName', sortable: true, searchable: true, width: '25%' },
    { dataField: 'languageCode', sortable: true, searchable: true, searchOperator: 'sw' },
  ];

  const renderForm = (row: any, validationErrors: any) => {
    return (
      <React.Fragment>
        <AdminFormField
          entity="languages"
          field="name"
          value={row.name}
          required
          autoFocus
          invalid={validationErrors.name}
          T={T}
        />
        <AdminFormSelect
          entity="languages"
          field="languageName"
          inputName="language"
          value={row.languageCode || 'en'}
          options={languages}
          T={T}
        />
        <AdminFormFile
          entity="languages"
          field="upload"
          required={!row.id}
          invalid={validationErrors.upload}
          T={T}
        />
        <AdminFormCheck
          entity="languages"
          field="active"
          value={!row.disabled}
          T={T}
        />
      </React.Fragment>
    );
  };

  const validateForm = (form: any, row: any) => {
    const hyphen = form.language.indexOf('-');
    const data = {
      name: (form.name || '').trim(),
      language: hyphen < 0 ? form.language : form.language.substring(0, hyphen),
      country: hyphen < 0 ? null : form.language.substring(1 + hyphen),
      disabled: !form.active,
    };
    const extras = {
      guid: form.uploadUpload,
    };
    const missing = !data.name ? 'name' : !row.id && !extras.guid ? 'upload' : null;
    const params = missing && { field: T.t(`adminPage.languages.fieldName.${missing}`) };
    return missing
      ? { validationErrors: { [missing]: T.t('adminForm.validation.fieldIsRequired', params || undefined) } }
      : { data, extras };
  };

  const uploadLanguage = (response: any, extras: any) => {
    if (extras && extras.guid) {
      const data = 'guid=' + encodeURIComponent(extras.guid);
      return axios
        .post(`${LanguagesUrl}/${response.data.id}/upload`, data, ContentTypeURLEncoded)
        .then(() => response); // return original response
    } else {
      return response;
    }
  };

  // passing in togglePopover is verging awful
  const getButtonInfo = (selectedRow: any, togglePopover: any) => {
    const id = selectedRow ? selectedRow.id : '';
    const toggleDownload = () => {
      setDownloadOpen(open => !open);
      togglePopover('download', false);
    };
    return [
      <ButtonDropdown
        key="download"
        isOpen={!!selectedRow && downloadOpen}
        toggle={toggleDownload}
      >
        <DropdownToggle
          caret
          className="glyphButton"
          disabled={!selectedRow}
          id="react-table-download-button"
          onMouseOver={() => togglePopover('download', !downloadOpen)}
          onMouseOut={() => togglePopover('download', false)}
        >
          <i
            className="material-icons md-18"
            aria-hidden="true"
          >
            file_download
          </i>
        </DropdownToggle>
        <DropdownMenu id="languages-download-menu">
          {['csv', 'json', 'properties'].map(f => (
            <DropdownItem
              key={f}
              id={`languages-download-format-${f}`}
              href={`${LanguagesUrl}/${id}/download.${f}`}
              download
            >
              {T.t(`adminPage.languages.format.${f}`)}
            </DropdownItem>
          ))}
        </DropdownMenu>
      </ButtonDropdown>,
    ];
  };

  return !loaded ? (
    <div />
  ) : (
    <ReactTable
      entity="languages"
      getButtons={getButtonInfo}
      schema="language"
      columns={tableColumns}
      defaultSortField="name"
      defaultSearchField="name"
      renderForm={renderForm}
      validateForm={validateForm}
      afterCreateOrUpdate={uploadLanguage}
      translations={T}
    />
  );
};

Languages.pageInfo = {
  identifier: 'languages',
  icon: IoLanguageOutline,
  link: '/Languages',
  group: 'domain',
  right: 'loi.cp.admin.right.AdminRight',
  entity: 'languages',
};

export default Languages;
