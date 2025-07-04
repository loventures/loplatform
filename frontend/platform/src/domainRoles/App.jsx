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
import React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

import ReactTable from '../components/reactTable/ReactTable';
import * as MainActions from '../redux/actions/MainActions';
import EditAddDomainRole from './EditAddDomainRoles';
import RightsInfo from './RightsInfo';
import { IoPeopleOutline } from 'react-icons/io5';

class App extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      loaded: false,
      rightsMap: {},
      rightsInfo: null,
    };
  }

  componentDidMount() {
    axios.get('/api/v2/domain/rights').then(res => {
      const rightsMap = res.data.objects.reduce((obj, right) => {
        return {
          ...obj,
          [right.identifier]: {
            name: right.name,
            description: right.description,
          },
        };
      }, {});
      this.setState({ loaded: true, rightsMap: rightsMap });
    });
  }

  onRightsInfoClick = row => {
    this.setState({ rightsInfo: row });
    return Promise.resolve(false);
  };

  getButtonInfo = () => {
    return [
      {
        name: 'viewRights',
        iconName: 'info',
        onClick: this.onRightsInfoClick,
      },
    ];
  };

  renderModal = () => {
    const { lo_platform, translations: T } = this.props;
    const { rightsInfo } = this.state;
    if (!rightsInfo) return null;
    return (
      <RightsInfo
        row={rightsInfo}
        lo_platform={lo_platform}
        T={T}
        close={() => this.setState({ rightsInfo: null })}
      />
    );
  };

  columns = [
    { dataField: 'id', isKey: true },
    { dataField: 'roleId', sortable: false, searchable: false, required: true, width: '20%' },
    { dataField: 'name', sortable: false, searchable: false, required: true, width: '20%' },
    { dataField: 'rights', sortable: false, searchable: false, required: false, width: '60%' },
  ];

  parseRole = role => {
    const rightsMap = this.state.rightsMap;
    return {
      ...role,
      name: role.roleType.name,
      roleId: role.roleType.roleId,
      rights: role.rightIds.length ? role.rightIds.map(id => rightsMap[id].name).join(', ') : '',
    };
  };

  renderForm = (row, validationErrors) => {
    const T = this.props.translations;
    const isEditing = Object.keys(row).length > 0;
    return (
      <EditAddDomainRole
        T={T}
        columns={this.columns}
        row={row}
        validationErrors={validationErrors}
        editing={isEditing}
      />
    );
  };

  validateForm = form => {
    const T = this.props.translations;
    if (form.addingSupported) {
      if (form.supportedRole) {
        const data = {
          addingSupported: true,
          supportedRole: form.supportedRole,
          name: '',
          roleId: '',
        };
        return { data };
      } else {
        const param = { field: T.t(`adminPage.roles.fieldName.supportedRoleId`) };
        return {
          validationErrors: { supportedRoleId: T.t('adminForm.validation.fieldIsRequired', param) },
        };
      }
    } else {
      const data = {
        addingSupported: false,
        roleId: form.roleId,
        name: form.name,
      };
      const missing = this.columns.find(col => col.required && !data[col.dataField]);
      const params = missing && { field: T.t(`adminPage.roles.fieldName.${missing.dataField}`) };
      return missing
        ? {
            validationErrors: {
              [missing.dataField]: T.t('adminForm.validation.fieldIsRequired', params),
            },
          }
        : { data };
    }
  };

  render() {
    if (!this.state.loaded) return null;
    return (
      <React.Fragment>
        <ReactTable
          entity="roles"
          paginate={false}
          columns={this.columns}
          defaultSortField="roleId"
          defaultSearchField="roleId"
          renderForm={this.renderForm}
          translations={this.props.translations}
          filter={false}
          setPortalAlertStatus={this.props.setPortalAlertStatus}
          validateForm={this.validateForm}
          parseEntity={this.parseRole}
          getButtons={this.getButtonInfo}
        />
        {this.renderModal()}
      </React.Fragment>
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

App.propTypes = {
  translations: PropTypes.object.isRequired,
  setPortalAlertStatus: PropTypes.func.isRequired,
};

const DomainRoles = connect(mapStateToProps, mapDispatchToProps)(App);

DomainRoles.pageInfo = {
  identifier: 'roles',
  icon: IoPeopleOutline,
  link: '/DomainRoles',
  group: 'users',
  right: 'loi.cp.admin.right.RoleAdminRight',
  entity: 'roles',
};

export default DomainRoles;
