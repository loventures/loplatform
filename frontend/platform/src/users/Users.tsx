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
import moment from 'moment/moment';
import Polyglot from 'node-polyglot';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import ReactTable, { clearSavedTableState } from '../components/reactTable/ReactTable';
import { LoPlatform } from '../types/loPlatform';
import { EmailRE, trim } from '../services';
import { ConnectorNamesUrl, SubtenantNamesUrl, UsersDomainRolesUrl } from '../services/URLs';
import EditAddForm from './EditAddForm';

interface UsersProps {
  translations: Polyglot;
  lo_platform: LoPlatform;
  setPortalAlertStatus: (error: boolean, success: boolean, message: string) => void;
}

const Users = (props: UsersProps) => {
  const { translations: T, lo_platform, setPortalAlertStatus } = props;
  const navigate = useNavigate();

  const [domainRoles, setDomainRoles] = useState<any[]>([]);
  const [externalSystems, setExternalSystems] = useState<any[]>([]);
  const [subtenants, setSubtenants] = useState<Record<string, any>>({});
  const [subtenantsArr, setSubtenantsArr] = useState<any[]>([]);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    const fetchen = [
      axios.get(UsersDomainRolesUrl),
      axios.get(ConnectorNamesUrl),
      axios.get(SubtenantNamesUrl),
    ];
    axios
      .all(fetchen)
      .then(
        axios.spread((domainRolesRes: any, externalSystemsRes: any, subtenantsRes: any) => {
          const subtenants = subtenantsRes.data.objects.reduce(
            (o: Record<string, any>, sub: any) => ({ ...o, [sub.id]: sub }),
            {}
          );
          setDomainRoles(
            domainRolesRes.data.objects.sort((a: any, b: any) =>
              a.name.toLowerCase().localeCompare(b.name.toLowerCase())
            )
          );
          setLoaded(true);
          setExternalSystems(externalSystemsRes.data.objects);
          setSubtenants(subtenants);
          setSubtenantsArr(subtenantsRes.data.objects);
        })
      )
      .catch(e => {
        console.log(e);
        setPortalAlertStatus(true, false, T.t('error.unexpectedError'));
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const formatAccessTime = (_t: any, row: any) => {
    const fmt = T.t('adminPage.users.momentFormat.accessTime');
    // Operations like sudo allow access time without login time but
    // there is now no case for login time without access time
    return row.times.accessTime
      ? moment(row.times.accessTime).format(fmt)
      : '(' + moment(row.times.createTime).format(fmt) + ')';
  };

  const generateColumns = () => {
    const roleFilterOptions = [
      <option
        key="any-admin"
        value={domainRoles
          .filter(r => r.admin)
          .map(r => r.id)
          .join(',')}
      >
        {T.t('adminPage.users.anyAdminRole')}
      </option>,
      ...domainRoles.map(role => {
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
    const subtenantFilterOptions = subtenantsArr.map(sub => {
      return (
        <option
          key={sub.id}
          value={sub.id}
        >
          {sub.name}
        </option>
      );
    });
    const columns: any[] = [
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
    if (subtenantsArr.length > 0 && !lo_platform.user.subtenant_id) {
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
      dataFormat: formatAccessTime,
    });
    return columns;
  };

  const renderForm = (row: any, validationErrors: any) => {
    return (
      <EditAddForm
        translations={T}
        row={row}
        validationErrors={validationErrors}
        subtenants={subtenantsArr}
        externalSystems={externalSystems}
        domainRoles={domainRoles}
        columns={generateColumns()}
        lo_platform={lo_platform}
      />
    );
  };

  const validateForm = (form: any, _row: any, el: any) => {
    const parse = (s: any) => parseInt(s, 10) || null;
    const data: any = {
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
    const cols = generateColumns(); // ugh
    const isRequired = (field: string) => {
      const col = cols.find(col => col.dataField === field);
      return (
        (col && col.required) ||
        (field === 'emailAddress' && form.email) ||
        (field === 'password' && form.pass)
      );
    };
    const missing = Array.from(el.elements)
      .map((el: any) => el.name)
      .find((field: string) => isRequired(field) && !form[field]); // in order of elements for uxiness
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

  const flattenUser = (user: any) => {
    return {
      ...user,
      roles: user.roles.length === 0 ? '' : user.roles.join(', '),
      uniqueId:
        user.integrations.length === 0
          ? ''
          : user.integrations.map((integration: any) => integration.uniqueId).join(', '),
      subtenant: user.subtenant_id ? subtenants[user.subtenant_id].name : null,
    };
  };

  const transition = (selectedRows: any[]) => {
    const allSuspended = selectedRows.every(sel => sel.userState !== 'Active');
    const ids = selectedRows.map(row => `id=${row.id}`).join('&');
    return axios
      .post(`/api/v2/users/transition?${ids}`, {
        state: !allSuspended ? 'Suspended' : 'Active',
      })
      .catch(e => {
        console.log(e);
        setPortalAlertStatus(true, false, T.t('error.unexpectedError'));
      });
  };

  const sudo = (selectedRow: any) => {
    const returnUrl = window.location.pathname;
    return axios
      .post(`/api/v2/users/${selectedRow.id}/sudo?returnUrl=${encodeURIComponent(returnUrl)}`)
      .then(() => {
        window.history.replaceState(
          {},
          'Exit',
          `/sys/eunt/domus${returnUrl}?user=${lo_platform.user.id}`
        );
        if (window.top) window.top.location.href = '/';
        return false;
      });
  };

  const logout = (selectedRow: any) => {
    return axios.post('/api/v2/users/' + selectedRow.id + '/logout').catch(e => {
      console.log(e);
      setPortalAlertStatus(true, false, T.t('error.unexpectedError'));
    });
  };

  const viewCourseSections = ({ id }: any) => {
    clearSavedTableState('courseSections');
    navigate(`/Users/${id}/CourseSections`);
    return Promise.resolve(false);
  };

  const getButtonInfo = (selectedRows: any[]) => {
    const allSuspended = selectedRows.every(sel => sel.userState !== 'Active');
    return [
      {
        name: !allSuspended ? 'suspend' : 'reinstate',
        iconName: !allSuspended ? 'not_interested' : 'check',
        onClick: transition,
        multiSelect: true,
      },
      {
        name: 'courseSections',
        iconName: 'school',
        onClick: viewCourseSections,
      },
      {
        name: 'sudo',
        iconName: 'directions_run',
        onClick: sudo,
      },
      {
        name: 'logout',
        iconName: 'logout',
        onClick: logout,
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

  const trClassFormat = ({ userState }: any) => (userState === 'Suspended' ? 'row-disabled' : '');

  if (!loaded) return <div />;
  const columns = generateColumns();
  const tdClassFormat = (_v: any, col: string, row: any) =>
    col === 'accessTime' && !row.times[col] ? 'create-time' : '';
  return (
    <ReactTable
      entity="users"
      autoComplete="off"
      columns={columns}
      defaultSortField="fullName"
      defaultSearchField="fullName"
      parseEntity={flattenUser}
      embed="roles,integrations,times"
      renderForm={renderForm}
      validateForm={validateForm}
      translations={T}
      setPortalAlertStatus={setPortalAlertStatus}
      getButtons={getButtonInfo}
      trClassFormat={trClassFormat}
      tdClassFormat={tdClassFormat}
      openRow={sudo}
      multiSelect={true}
      multiDelete={true}
    />
  );
};

export default Users;
