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

import React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

import { AdminFormCheck, AdminFormField, AdminFormFile } from '../components/adminForm';
import ReactTable from '../components/reactTable/ReactTable';
import * as MainActions from '../redux/actions/MainActions';
import { TbArrowBounce } from 'react-icons/tb';

const CsvSample = [
  ['/from', '/to'],
  ['/from?param=value', '/elsewhere'],
  ['/external/redirect', 'http://example.org/'],
]
  .map(a => a.map(o => `"${o}"`).join(','))
  .join('\n');

class App extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  formatStatus = (a, row) => {
    const T = this.props.translations;
    return row.disabled
      ? T.t('adminPage.redirects.status.inactive')
      : T.t('adminPage.redirects.status.active');
  };

  columns = [
    { dataField: 'id', isKey: true },
    {
      dataField: 'status',
      sortable: false,
      searchable: false,
      dataFormat: this.formatStatus,
      width: '30%',
    },
    { dataField: 'name', sortable: true, searchable: true, required: true },
  ];

  renderForm = (row, validationErrors) => {
    const T = this.props.translations;
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

  validateForm = (form, row) => {
    const data = {
      name: (form.name || '').trim(),
      csvUpload: form.csvUpload,
      disabled: !form.active,
    };
    const missing = !data.name ? 'name' : !row.id && !data.csvUpload ? 'csv' : null;
    const T = this.props.translations;
    const params = missing && { field: T.t(`adminPage.redirects.fieldName.${missing}`) };
    return missing
      ? { validationErrors: { [missing]: T.t('adminForm.validation.fieldIsRequired', params) } }
      : { data };
  };

  render() {
    return (
      <ReactTable
        entity="redirects"
        columns={this.columns}
        defaultSortField="name"
        defaultSearchField="name"
        renderForm={this.renderForm}
        validateForm={this.validateForm}
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

const Redirects = connect(mapStateToProps, mapDispatchToProps)(App);

Redirects.pageInfo = {
  identifier: 'redirects',
  icon: TbArrowBounce,
  link: '/Redirects',
  group: 'media',
  right: 'loi.cp.redirect.RedirectAdminRight',
  entity: 'redirects',
};

export default Redirects;
