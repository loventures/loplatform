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

import { AdminFormField } from '../components/adminForm';
import ReactTable from '../components/reactTable/ReactTable';
import * as MainActions from '../redux/actions/MainActions';
import { trim } from '../services';

class App extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  columns = [
    { dataField: 'id', isKey: true },
    { dataField: 'ip', sortable: false, searchable: false, required: true },
  ];

  renderForm = (row, validationErrors) => {
    const { translations: T } = this.props;
    return (
      <AdminFormField
        key="ip"
        entity="bannedIps"
        field="ip"
        value={row.ip}
        invalid={validationErrors.ip}
        T={T}
      />
    );
  };

  validateForm = form => {
    const data = trim(form.ip);
    const missing = data === '';
    const T = this.props.translations;
    const params = { field: T.t(`adminPage.bannedIps.fieldName.ip`) };
    if (missing) {
      return { validationErrors: { ip: T.t('adminForm.validation.fieldIsRequired', params) } };
    }
    return {
      data: JSON.stringify(data),
      headers: { headers: { 'Content-Type': 'application/json' } },
    };
  };

  parseEntity = ip => ({ id: ip, ip: ip });

  createDeleteDTO = ip => {
    return {
      data: JSON.stringify(ip),
      headers: { 'Content-Type': 'application/json' },
    };
  };

  render() {
    const handleDelete = {
      createDeleteDTO: this.createDeleteDTO,
      deleteMethod: 'post',
      getDeleteUrl: () => '/api/v2/overlord/bannedIps/delete',
    };
    return (
      <ReactTable
        entity="bannedIps"
        columns={this.columns}
        renderForm={this.renderForm}
        validateForm={this.validateForm}
        translations={this.props.translations}
        setPortalAlertStatus={this.props.setPortalAlertStatus}
        parseEntity={this.parseEntity}
        paginate={false}
        updateButton={false}
        baseUrl="/api/v2/overlord/bannedIps"
        postUrl="/api/v2/overlord/bannedIps"
        handleDelete={handleDelete}
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

export default connect(mapStateToProps, mapDispatchToProps)(App);
