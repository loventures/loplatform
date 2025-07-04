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

import axios from 'axios';
import React from 'react';
import { connect } from 'react-redux';
import { ButtonDropdown, DropdownItem, DropdownMenu, DropdownToggle } from 'reactstrap';
import { bindActionCreators } from 'redux';

import {
  AdminFormCheck,
  AdminFormField,
  AdminFormFile,
  AdminFormSelect,
} from '../components/adminForm';
import ReactTable from '../components/reactTable/ReactTable';
import * as MainActions from '../redux/actions/MainActions';
import { ContentTypeURLEncoded } from '../services';
import { LanguagesUrl, LocalesUrl } from '../services/URLs';
import { IoLanguageOutline } from 'react-icons/io5';

class App extends React.Component {
  constructor(props) {
    super(props);
    this.state = { loaded: false, downloadOpen: false, languages: [] };
  }

  componentDidMount() {
    axios
      .get(LocalesUrl)
      .then(res => {
        this.setState({ loaded: true, languages: res.data.objects });
      })
      .catch(e => {
        console.log(e);
        const T = this.props.translations;
        this.props.setPortalAlertStatus(true, false, T.t('error.unexpectedError'));
      });
  }

  formatStatus = (a, row) => {
    const T = this.props.translations;
    return row.disabled
      ? T.t('adminPage.languages.status.suspended')
      : T.t('adminPage.languages.status.active');
  };

  columns = [
    { dataField: 'id', isKey: true },
    {
      dataField: 'status',
      sortable: false,
      searchable: false,
      width: '15%',
      dataFormat: this.formatStatus,
    },
    { dataField: 'name', sortable: true, searchable: true, width: '40%' },
    { dataField: 'languageName', sortable: true, searchable: true, width: '25%' },
    { dataField: 'languageCode', sortable: true, searchable: true, searchOperator: 'sw' },
  ];

  renderForm = (row, validationErrors) => {
    const T = this.props.translations;
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
          options={this.state.languages}
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

  validateForm = (form, row) => {
    const T = this.props.translations;
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
      ? { validationErrors: { [missing]: T.t('adminForm.validation.fieldIsRequired', params) } }
      : { data, extras };
  };

  uploadLanguage = (response, extras) => {
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
  getButtonInfo = (selectedRow, togglePopover) => {
    const T = this.props.translations;
    const id = selectedRow ? selectedRow.id : '';
    const toggleDownload = () => {
      this.setState({ downloadOpen: !this.state.downloadOpen });
      togglePopover('download', false);
    };
    return [
      <ButtonDropdown
        key="download"
        isOpen={!!selectedRow && this.state.downloadOpen}
        toggle={toggleDownload}
      >
        <DropdownToggle
          caret
          className="glyphButton"
          disabled={!selectedRow}
          id="react-table-download-button"
          onMouseOver={() => togglePopover('download', !this.state.downloadOpen)}
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

  render() {
    const { loaded } = this.state;
    return !loaded ? (
      <div />
    ) : (
      <ReactTable
        entity="languages"
        getButtons={this.getButtonInfo}
        schema="language"
        columns={this.columns}
        defaultSortField="name"
        defaultSearchField="name"
        renderForm={this.renderForm}
        validateForm={this.validateForm}
        afterCreateOrUpdate={this.uploadLanguage}
        translations={this.props.translations}
        setPortalAlertStatus={this.props.setPortalAlertStatus}
      />
    );
  }
}

function mapStateToProps(state) {
  return {
    translations: state.main.translations,
  };
}

function mapDispatchToProps(dispatch) {
  return bindActionCreators(MainActions, dispatch);
}

const Languages = connect(mapStateToProps, mapDispatchToProps)(App);

Languages.pageInfo = {
  identifier: 'languages',
  icon: IoLanguageOutline,
  link: '/Languages',
  group: 'domain',
  right: 'loi.cp.admin.right.AdminRight',
  entity: 'languages',
};

export default Languages;
