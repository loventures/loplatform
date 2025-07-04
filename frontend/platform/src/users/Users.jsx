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
import moment from 'moment/moment';
import PropTypes from 'prop-types';
import React from 'react';

import ReactTable, { clearSavedTableState } from '../components/reactTable/ReactTable';
import { EmailRE, trim } from '../services';
import { ConnectorNamesUrl, SubtenantNamesUrl, UsersDomainRolesUrl } from '../services/URLs';
import EditAddForm from './EditAddForm';

class Users extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      domainRoles: [],
      externalSystems: [],
      subtenants: {},
      subtenantsArr: [],
      loaded: false,
    };
  }

  componentDidMount() {
    const fetchen = [
      axios.get(UsersDomainRolesUrl),
      axios.get(ConnectorNamesUrl),
      axios.get(SubtenantNamesUrl),
    ];
    axios
      .all(fetchen)
      .then(
        axios.spread((domainRolesRes, externalSystemsRes, subtenantsRes) => {
          const subtenants = subtenantsRes.data.objects.reduce(
            (o, sub) => ({ ...o, [sub.id]: sub }),
            {}
          );
          this.setState({
            domainRoles: domainRolesRes.data.objects.sort((a, b) =>
              a.name.toLowerCase().localeCompare(b.name.toLowerCase())
            ),
            loaded: true,
            externalSystems: externalSystemsRes.data.objects,
            subtenants: subtenants,
            subtenantsArr: subtenantsRes.data.objects,
          });
        })
      )
      .catch(e => {
        console.log(e);
        const T = this.props.translations;
        this.props.setPortalAlertStatus(true, false, T.t('error.unexpectedError'));
      });
  }

  generateColumns = () => {
    const T = this.props.translations;
    const roleFilterOptions = [
      <option
        key="any-admin"
        value={this.state.domainRoles
          .filter(r => r.admin)
          .map(r => r.id)
          .join(',')}
      >
        {T.t('adminPage.users.anyAdminRole')}
      </option>,
      ...this.state.domainRoles.map(role => {
        return (
          <option
            key={role.id}
            value={role.id}
          >
            {role.name}
          </option>
        );
      }),
    ];
    const subtenantFilterOptions = this.state.subtenantsArr.map(sub => {
      return (
        <option
          key={sub.id}
          value={sub.id}
        >
          {sub.name}
        </option>
      );
    });
    const columns = [
      { dataField: 'id', isKey: true },
      {
        dataField: 'fullName',
        sortable: true,
        searchable: true,
        required: false,
        filterable: false,
        searchOperator: 'ts',
        width: '20%',
      },
      {
        dataField: 'givenName',
        sortable: true,
        searchable: false,
        required: false,
        filterable: false,
        hidden: true,
      },
      {
        dataField: 'middleName',
        sortable: true,
        searchable: false,
        required: false,
        filterable: false,
        hidden: true,
      },
      {
        dataField: 'familyName',
        sortable: true,
        searchable: false,
        required: false,
        filterable: false,
        hidden: true,
      },
      {
        dataField: 'emailAddress',
        sortable: true,
        searchable: true,
        required: false,
        filterable: false,
        searchOperator: 'sw',
        width: '20%',
      },
      {
        dataField: 'roles',
        sortable: false,
        searchable: false,
        required: false,
        filterable: true,
        filterProperty: 'domainRole',
        filterOptions: roleFilterOptions,
        filterOperator: 'in',
        baseFilter: 'Any Role',
        width: '10%',
      },
      {
        dataField: 'userName',
        sortable: true,
        searchable: true,
        required: true,
        filterable: false,
        searchOperator: 'sw',
      },
      {
        dataField: 'externalId',
        sortable: true,
        searchable: true,
        required: false,
        filterable: false,
        searchOperator: 'sw',
      },
      {
        dataField: 'uniqueId',
        sortable: false,
        searchable: true,
        required: false,
        filterable: false,
        searchOperator: 'sw',
      },
    ];
    if (this.state.subtenantsArr.length > 0 && !this.props.lo_platform.user.subtenant_id) {
      columns.push({
        dataField: 'subtenant',
        sortable: false,
        searchable: false,
        required: false,
        filterable: true,
        filterProperty: 'subtenant_id',
        filterOptions: subtenantFilterOptions,
        baseFilter: 'Any Subtenant',
      });
    }
    columns.push({
      dataField: 'accessTime',
      sortable: true,
      nullsOpposite: true,
      searchable: false,
      required: false,
      filterable: false,
      dataFormat: this.formatAccessTime,
    });
    return columns;
  };

  formatAccessTime = (t, row) => {
    const { translations: T } = this.props;
    const fmt = T.t('adminPage.users.momentFormat.accessTime');
    // Operations like sudo allow access time without login time but
    // there is now no case for login time without access time
    return row.times.accessTime
      ? moment(row.times.accessTime).format(fmt)
      : '(' + moment(row.times.createTime).format(fmt) + ')';
  };

  renderForm = (row, validationErrors) => {
    return (
      <EditAddForm
        translations={this.props.translations}
        row={row}
        validationErrors={validationErrors}
        subtenants={this.state.subtenantsArr}
        externalSystems={this.state.externalSystems}
        domainRoles={this.state.domainRoles}
        columns={this.generateColumns()}
        lo_platform={this.props.lo_platform}
      />
    );
  };

  validateForm = (form, row, el) => {
    const parse = s => parseInt(s, 10) || null;
    const data = {
      middleName: trim(form.middleName),
      externalId: trim(form.externalId) || null,
      familyName: trim(form.familyName),
      userName: trim(form.userName),
      givenName: trim(form.givenName),
      emailAddress: trim(form.emailAddress),
      subtenantId: parse(form.subtenantId),
      roles: [],
      password: form.pass ? trim(form.password) : null,
      emailPassword: !!form.email,
    };
    if (form.roles) data.roles = Array.isArray(form.roles) ? form.roles : [form.roles];
    // serialize behavious is malfeasant when fields are empty, even with empty: true
    const integrations = [];
    for (let i = 0; el[`integrationId-${i}`]; ++i) {
      integrations.push({
        integrationId: parse(el[`integrationId-${i}`].value),
        systemId: parse(el[`systemId-${i}`].value),
        uniqueId: trim(el[`uniqueId-${i}`].value),
      });
    }
    data.uniqueIds = integrations.filter(i => i.systemId && i.uniqueId);
    const cols = this.generateColumns(); // ugh
    const isRequired = field => {
      const col = cols.find(col => col.dataField === field);
      return (
        (col && col.required) ||
        (field === 'emailAddress' && form.email) ||
        (field === 'password' && form.pass)
      );
    };
    const missing = Array.from(el.elements)
      .map(el => el.name)
      .find(field => isRequired(field) && !form[field]); // in order of elements for uxiness
    const T = this.props.translations;
    if (missing) {
      const params = { field: T.t(`adminPage.users.fieldName.${missing}`) };
      return {
        validationErrors: { [missing]: T.t('adminForm.validation.fieldIsRequired', params) },
      };
    } else if (data.emailAddress && !EmailRE.test(data.emailAddress)) {
      const params = { field: T.t('adminPage.users.fieldName.emailAddress') };
      return {
        validationErrors: { emailAddress: T.t('adminForm.validation.fieldMustBeValid', params) },
      };
    } else {
      return { data };
    }
  };

  flattenUser = user => {
    return {
      ...user,
      roles: user.roles.length === 0 ? '' : user.roles.join(', '),
      uniqueId:
        user.integrations.length === 0
          ? ''
          : user.integrations.map(integration => integration.uniqueId).join(', '),
      subtenant: user.subtenant_id ? this.state.subtenants[user.subtenant_id].name : null,
    };
  };

  transition = selectedRows => {
    const allSuspended = selectedRows.every(sel => sel.userState !== 'Active');
    const ids = selectedRows.map(row => `id=${row.id}`).join('&');
    return axios
      .post(`/api/v2/users/transition?${ids}`, {
        state: !allSuspended ? 'Suspended' : 'Active',
      })
      .catch(e => {
        console.log(e);
        const T = this.props.translations;
        this.props.setPortalAlertStatus(true, false, T.t('error.unexpectedError'));
      });
  };

  sudo = selectedRow => {
    const returnUrl = window.location.pathname;
    return axios
      .post(`/api/v2/users/${selectedRow.id}/sudo?returnUrl=${encodeURIComponent(returnUrl)}`)
      .then(() => {
        window.history.replaceState(
          {},
          'Exit',
          `/sys/eunt/domus${returnUrl}?user=${this.props.lo_platform.user.id}`
        );
        window.top.location.href = '/';
        return false;
      });
  };

  logout = selectedRow => {
    return axios.post('/api/v2/users/' + selectedRow.id + '/logout').catch(e => {
      console.log(e);
      const T = this.props.translations;
      this.props.setPortalAlertStatus(true, false, T.t('error.unexpectedError'));
    });
  };

  viewCourseSections = ({ id }) => {
    clearSavedTableState('courseSections');
    this.props.history.push(`/Users/${id}/CourseSections`);
    return Promise.resolve(false);
  };

  getButtonInfo = selectedRows => {
    const allSuspended = selectedRows.every(sel => sel.userState !== 'Active');
    return [
      {
        name: !allSuspended ? 'suspend' : 'reinstate',
        iconName: !allSuspended ? 'not_interested' : 'check',
        onClick: this.transition,
        multiSelect: true,
      },
      {
        name: 'courseSections',
        iconName: 'school',
        onClick: this.viewCourseSections,
      },
      {
        name: 'sudo',
        iconName: 'directions_run',
        onClick: this.sudo,
      },
      {
        name: 'logout',
        iconName: 'logout',
        onClick: this.logout,
      },
      {
        name: 'adminReport',
        iconName: 'psychology',
        alwaysEnabled: true,
        className: 'ms-3',
        href: '/api/v2/users/adminReport',
        solo: true,
      },
    ];
  };

  trClassFormat = ({ userState }) => (userState === 'Suspended' ? 'row-disabled' : '');

  render() {
    if (!this.state.loaded) return <div />;
    const columns = this.generateColumns();
    const tdClassFormat = (v, col, row) =>
      col === 'accessTime' && !row.times[col] ? 'create-time' : '';
    return (
      <ReactTable
        entity="users"
        autoComplete="off"
        columns={columns}
        defaultSortField="fullName"
        defaultSearchField="fullName"
        parseEntity={this.flattenUser}
        embed="roles,integrations,times"
        renderForm={this.renderForm}
        validateForm={this.validateForm}
        translations={this.props.translations}
        setPortalAlertStatus={this.props.setPortalAlertStatus}
        getButtons={this.getButtonInfo}
        trClassFormat={this.trClassFormat}
        tdClassFormat={tdClassFormat}
        openRow={this.sudo}
        multiSelect={true}
        multiDelete={true}
      />
    );
  }
}

Users.propTypes = {
  translations: PropTypes.object.isRequired,
  lo_platform: PropTypes.object.isRequired,
  setPortalAlertStatus: PropTypes.func.isRequired,
};

export default Users;
