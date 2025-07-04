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

import { push } from 'connected-react-router';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import _ from 'underscore';

import { AdminFormCheck, AdminFormField, AdminFormFile } from '../components/adminForm';
import ReactTable, { clearSavedTableState } from '../components/reactTable/ReactTable';
import * as MainActions from '../redux/actions/MainActions';

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

class App extends Component {
  constructor(props) {
    super(props);
    this.state = { uploading: false };
  }

  _getButtons = row => [
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
      onClick: this._navToRevision,
    },
  ];

  _navToRevision = row => {
    clearSavedTableState('zipSites.revision');
    this.props.navigateToRevision(row.id);
    return new Promise(res => res(false));
  };

  _viewSite = ({ disabled, path }) => {
    if (!disabled) window.top.location.href = path;
  };

  _renderForm = (row, validationErrors) => {
    const { translations: T } = this.props;

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
          onChange={({ uploading }) => this.setState({ uploading })}
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

  _validateSite = () => true;

  _validateForm = (form, row) => {
    const {
      props: { translations: T },
      state: { uploading },
    } = this;

    const missingField = f =>
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
      !form.site || this._validateSite(form.site)
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

  render() {
    const { setPortalAlertStatus, translations: T } = this.props;

    const trClassFormat = row => (row.disabled ? 'row-disabled' : '');

    return (
      <ReactTable
        entity={Entity}
        columns={Columns}
        defaultSortField="name"
        defaultSearchField="name"
        getButtons={this._getButtons}
        openRow={this._viewSite}
        renderForm={this._renderForm}
        trClassFormat={trClassFormat}
        validateForm={this._validateForm}
        setPortalAlertStatus={setPortalAlertStatus}
        translations={T}
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
  return {
    navigateToRevision: id => dispatch(push(`/ZipSites/${id}`)),
    ...bindActionCreators(MainActions, dispatch),
  };
}

const ZipSiteTable = connect(mapStateToProps, mapDispatchToProps)(App);

export default ZipSiteTable;
