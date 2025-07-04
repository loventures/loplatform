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
import PropTypes from 'prop-types';
import React, { Component } from 'react';

import AdminFormFile from '../components/adminForm/AdminFormFile';
import ReactTable from '../components/reactTable/ReactTable';
import LoPropTypes from '../react/loPropTypes';

const Columns = [
  { dataField: 'id', isKey: true },
  { dataField: 'fileName', sortable: true, searchable: true, required: true, width: '50%' },
  { dataField: 'createTime', sortable: true, searchable: false, required: true },
  {
    dataField: 'creator',
    sortable: false,
    searchable: false,
    required: false,
    dataFormat: u => u.fullName,
  },
];
const Entity = 'zipSites.revision';

class RevisionView extends Component {
  constructor(props) {
    super(props);
    this.state = {
      activeId: 0,
      uploading: false,
    };
    this.baseUrl = `/api/v2/zipSites/${props.siteId}`;
  }

  componentDidMount() {
    this._update();
  }

  _update = () => {
    const { setLastCrumb } = this.props;
    axios.get(this.baseUrl).then(res => {
      this.setState({ activeId: res.data.siteId });
      setLastCrumb(res.data.name);
    });
  };

  _getButtons = row => [
    {
      name: 'activate',
      iconName: 'check',
      disabled: row && row.id === this.state.activeId,
      onClick: this._activateRevision,
    },
    {
      name: 'download',
      iconName: 'file_download',
      href: row ? `${this.baseUrl}/revisions/${row.id}/view?download=true` : '/',
      download: true,
    },
    {
      name: 'preview',
      iconName: 'play_arrow',
      href: row ? `${this.baseUrl}/render?revision=${row.id}` : '/',
      target: '_top',
    },
  ];

  _activateRevision = row =>
    axios
      .put(this.baseUrl, { revision: row.id })
      .then(() => this._update())
      .then(() => true); // reload it

  _canDeleteRow = row => row && row.id !== this.state.activeId;

  _renderForm = (row, validationErrors) => {
    const { translations: T } = this.props;
    return (
      <AdminFormFile
        required
        field="site"
        entity={Entity}
        accept={['.zip']}
        invalid={validationErrors.site}
        onChange={({ uploading }) => this.setState({ uploading })}
        T={T}
      />
    );
  };

  _validateForm = form => {
    const {
      props: { translations: T },
      state: { uploading },
    } = this;

    const missingUploadMsg = T.t(
      `adminForm.validation.fieldIs${uploading ? 'Uploading' : 'Required'}`,
      { field: T.t('adminPage.zipSites.fieldName.site') }
    );

    return form.siteUpload && !uploading
      ? { data: { site: form.siteUpload } }
      : { validationErrors: { site: missingUploadMsg } };
  };

  _submitForm = ({ data: { site }, config, create }) => {
    if (!create) {
      console.log('update on revision table not supported (!?)');
      return new Promise((_, reject) => reject(null));
    } else {
      const payload = { site };
      return axios.put(this.baseUrl, payload, config).then(res => {
        this._update();
        return res;
      }); // dude where's my tap?
    }
  };

  render() {
    const { setPortalAlertStatus, translations: T } = this.props;
    const { activeId } = this.state;

    const trClassFormat = row => (row.id === activeId ? 'row-active' : '');

    return (
      <ReactTable
        entity={Entity}
        columns={Columns}
        baseUrl={`${this.baseUrl}/revisions`}
        defaultSortField="createTime"
        defaultSortOrder="desc"
        defaultSearchField="fileName"
        getButtons={this._getButtons}
        updateButton={false}
        canDeleteRow={this._canDeleteRow}
        renderForm={this._renderForm}
        validateForm={this._validateForm}
        submitForm={this._submitForm}
        trClassFormat={trClassFormat}
        setPortalAlertStatus={setPortalAlertStatus}
        translations={T}
      />
    );
  }
}

RevisionView.propTypes = {
  siteId: PropTypes.number.isRequired,
  setPortalAlertStatus: PropTypes.func.isRequired,
  translations: LoPropTypes.translations.isRequired,
  setLastCrumb: PropTypes.func.isRequired,
};

export default RevisionView;
