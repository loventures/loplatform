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
import { useNavigate } from 'react-router-dom';
import { Dropdown, DropdownItem, DropdownMenu, DropdownToggle } from 'reactstrap';

import useRouteBasePath from '../components/useRouteBasePath';

import ReactTable, { clearSavedTableState } from '../components/reactTable/ReactTable';
import { setPortalAlertStatus } from '../redux/actions/MainActions';
import { useLoPlatform, useTranslations } from '../redux/state';
import EditAddEnrollment from './EditAddEnrollment';

interface Role {
  id: number;
  name: string;
}

interface Course {
  id?: number;
  fjœr?: boolean;
  [key: string]: unknown;
}

interface DropdownItemConfig {
  key: string;
  name: string;
  onClick: () => void;
}

interface RosterRow {
  id: number;
  user_type?: string;
  disabled?: string;
  fullName?: string;
  userName?: string;
}

interface EnrolledUser {
  id: number;
  enrollments?: { context_id?: number; disabled: boolean }[];
  roles: Role[];
  [key: string]: unknown;
}

interface RosterProps {
  controllerValue: string;
  sudoUrl: (course: any, user?: any) => string;
  courseId: string;
  includeRights: boolean;
  readOnly: boolean;
}

const Roster: React.FC<RosterProps> = props => {
  const { controllerValue, courseId, sudoUrl, includeRights, readOnly } = props;
  const navigate = useNavigate();
  const matchUrl = useRouteBasePath();
  const T = useTranslations();
  const lo_platform = useLoPlatform();
  const dispatch = useDispatch();

  const [course, setCourse] = useState<Course>({});
  const [domainRoles, setDomainRoles] = useState<Role[]>([]);
  const [dropdownItems, setDropdownItems] = useState<DropdownItemConfig[]>([]);
  const [roleId, setRoleId] = useState<number | null>(null);
  const [loaded, setLoaded] = useState(false);
  const [searchBy, setSearchBy] = useState('userName');
  const [type, setType] = useState<string | undefined>(undefined);
  const [dropdownOpen, setDropdownOpen] = useState(false);

  const customFilters = [
    {
      property: 'includeInactive',
      value: '',
      prefilter: true,
    },
  ];

  const trClassFormat = ({ user_type, disabled }: RosterRow) =>
    user_type === 'Preview' ? 'row-preview' : disabled === 'Inactive' ? 'row-disabled' : '';

  const formatRole = (role: string, row: RosterRow) => {
    return row.user_type === 'Preview' ? 'Preview ' + role : role;
  };

  const getColumns = () => {
    const roleFilterOptions = domainRoles.map(role => {
      return (
        <option
          key={role.id}
          value={role.id}
        >
          {role.name}
        </option>
      );
    });
    return [
      { dataField: 'id', isKey: true },
      {
        dataField: 'role',
        sortable: false,
        searchable: false,
        filterable: true,
        filterOptions: roleFilterOptions,
        baseFilter: 'Any Role',
        filterProperty: 'role_id',
        prefilter: true,
        dataFormat: formatRole,
        width: '15%',
      },
      { dataField: 'disabled', sortable: false, searchable: false, hidden: true },
      { dataField: 'fullName', sortable: true, searchable: true, searchOperator: 'ts' },
      { dataField: 'emailAddress', sortable: true, searchable: true, searchOperator: 'sw' },
      { dataField: 'userName', sortable: true, searchable: true, searchOperator: 'sw' },
      { dataField: 'externalId', sortable: true, searchable: true, searchOperator: 'sw' },
    ];
  };

  const setPortalAlert = (error: boolean, success: boolean, message: string) =>
    dispatch(setPortalAlertStatus(error, success, message));

  const unexpectedError = (e: unknown) => {
    console.log(e);
    setPortalAlert(true, false, T.t('error.unexpectedError'));
  };

  useEffect(() => {
    const getCourse = axios.get(`/api/v2/${controllerValue}/${courseId}`);
    const getRoles = axios.get(`/api/v2/roles/byContext/${courseId}`);
    Promise.all([getCourse, getRoles])
      .then(([courseRes, domainRolesRes]) => {
        const roles: Role[] = domainRolesRes.data.objects
          .map((role: { roleType: Role }) => role.roleType)
          .sort((a: Role, b: Role) => a.name.toLowerCase().localeCompare(b.name.toLowerCase()));
        const items: DropdownItemConfig[] = roles.map(role => ({
          key: role.name,
          name: role.name,
          onClick: () => {
            setType(role.name);
            setRoleId(role.id);
          },
        }));
        setCourse(courseRes.data);
        setDomainRoles(roles);
        setDropdownItems(items);
        setLoaded(true);
      })
      .catch(unexpectedError);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const userEnrollments = ({ id }: RosterRow) => {
    clearSavedTableState('enrollments.userEnrollments');
    const path = `${matchUrl}/${id}`;
    const state = {
      course: course,
      controllerValue: controllerValue,
    };
    navigate(path, { state });
    return Promise.resolve(false);
  };

  const rights = () => {
    const path = `${matchUrl}/Rights`;
    const state = {
      course: course,
      controllerValue: controllerValue,
    };
    navigate(path, { state });
    return Promise.resolve(false);
  };

  const sudo = (selectedRow: RosterRow) => {
    const returnUrl = window.location.pathname;
    return axios
      .post(`/api/v2/users/${selectedRow.id}/sudo?returnUrl=${encodeURIComponent(returnUrl)}`)
      .then(() => {
        window.history.replaceState(
          {},
          'Exit',
          `/sys/eunt/domus${returnUrl}?user=${lo_platform.user.id}`
        );
        window.top!.location.href = sudoUrl(course, selectedRow);
        return false;
      })
      .catch(unexpectedError);
  };

  const transition = (selectedRow: RosterRow) => {
    const userId = selectedRow.id;
    const url = `/api/v2/courses/${course.id}/enrollments/byUser/${userId}/transition`;
    const data = {
      disabled: selectedRow.disabled === 'Active',
    };
    return axios
      .post(url, data)
      .then(() => true)
      .catch(unexpectedError);
  };

  const getButtonInfo = (selectedRow: RosterRow | false) => {
    const { fjœr } = course;

    const changeStateButton = readOnly
      ? []
      : [
          {
            name: !selectedRow || selectedRow.disabled === 'Active' ? 'suspend' : 'reinstate',
            iconName:
              !selectedRow || selectedRow.disabled === 'Active' ? 'not_interested' : 'check',
            onClick: transition,
          },
        ];

    const buttons = [
      ...changeStateButton,
      {
        name: 'enrollments',
        iconName: 'list',
        onClick: userEnrollments,
      },
      {
        name: 'sudo',
        iconName: 'directions_run',
        onClick: sudo,
        disabled: !fjœr || (selectedRow && selectedRow.user_type === 'Preview'),
      },
    ];
    if (includeRights && !readOnly) {
      buttons.push({
        name: 'rights',
        iconName: 'gavel',
        onClick: rights,
        alwaysEnabled: true,
      } as never);
    }
    return buttons;
  };

  const renderForm = (row: RosterRow, validationErrors: Record<string, string>) => {
    const editing = Object.keys(row).length > 0;
    return (
      <EditAddEnrollment
        T={T}
        row={row}
        editing={editing}
        validationErrors={validationErrors}
        courseId={courseId}
        searchBy={searchBy}
      />
    );
  };

  const validateForm = (form: Record<string, any>, row: RosterRow) => {
    const editing = Object.keys(row).length > 0;
    if (editing) {
      if (!form.userId) {
        const params = { field: T.t(`adminPage.enrollments.fieldName.user`) };
        return { validationErrors: { user: T.t('adminForm.validation.fieldIsRequired', params) } };
      } else if (!form.role) {
        const params = { field: T.t(`adminPage.enrollments.fieldName.role`) };
        return { validationErrors: { role: T.t('adminForm.validation.fieldIsRequired', params) } };
      } else {
        const data = {
          userId: parseInt(form.userId.trim(), 10),
          roleId: parseInt(form.role.trim(), 10),
        };
        return { data };
      }
    } else {
      if (!form.users) {
        const params = { field: T.t(`adminPage.enrollments.fieldName.users`) };
        return { validationErrors: { users: T.t('adminForm.validation.fieldIsRequired', params) } };
      } else {
        const ids = (typeof form.users === 'string' ? [form.users] : form.users).map((id: string) =>
          parseInt(id, 10)
        );
        const data = {
          ids: ids,
          roleId: roleId,
        };
        return { data };
      }
    }
  };

  const createDeleteDTO = (_id: number) => ({ data: {}, headers: {} });

  const submitForm = ({ data, create }: { data: any; create: boolean }) => {
    if (create) {
      return axios.post(`/api/v2/courses/${course.id}/enrollments/batch`, data).then(res => res);
    } else {
      const { userId, ...rest } = data;
      return axios
        .put(`/api/v2/courses/${course.id}/enrollments/byUser/${userId}`, rest)
        .then(res => res);
    }
  };

  const parseEnrolledUser = (enrolledUser: EnrolledUser) => {
    const enrollments = enrolledUser.enrollments || [];
    const enabled = enrollments
      .filter(enrollment => enrollment.context_id === course.id)
      .map(enrollment => enrollment.disabled)
      .includes(false);
    const status = enabled ? 'active' : 'inactive';
    const role = enrolledUser.roles.map(r => r.name).join(', ');
    return {
      ...enrolledUser,
      role: role || T.t('adminPage.enrollments.role.none'),
      roleId: enrolledUser.roles.length && enrolledUser.roles[0].id,
      disabled: T.t(`adminPage.enrollments.status.${status}`),
    };
  };

  const getDeleteUrl = (id: number) => {
    return `/api/v2/courses/${course.id}/enrollments/byUser/${id}`;
  };

  const getModalTitle = (modalType: string | null) => {
    if (modalType === 'create') {
      return T.t('adminPage.enrollments.modal.create.title', { type: type as string });
    }
    return undefined;
  };

  const searchPicker = (_row: RosterRow, modalType: string) => {
    const searchOptions = ['fullName', 'userName', 'externalId', 'emailAddress'];
    const onSearch = (by: string) => setSearchBy(by);
    const toggle = () => setDropdownOpen(o => !o);
    const entity = 'enrollments';
    const field = 'users';
    return (
      modalType === 'create' && (
        <Dropdown
          isOpen={dropdownOpen}
          toggle={toggle}
          style={{ position: 'absolute', top: '.75rem', right: '1rem' }}
        >
          <DropdownToggle
            id={`${entity}-${field}-searchDropdown`}
            caret
            outline
          >
            {T.t(`adminPage.enrollments.searchBy.${searchBy}`)}
          </DropdownToggle>
          <DropdownMenu id={`${entity}-${field}-searchMenu`}>
            {searchOptions.map(o => (
              <DropdownItem
                id={`${entity}-${field}-searchBy-${o}`}
                key={o}
                onClick={() => onSearch(o)}
              >
                {T.t(`adminPage.enrollments.fieldName.${o}`)}
              </DropdownItem>
            ))}
          </DropdownMenu>
        </Dropdown>
      )
    );
  };

  if (!loaded) return null;
  const handleDelete = {
    createDeleteDTO: createDeleteDTO,
    deleteMethod: 'delete',
    getDeleteUrl: getDeleteUrl,
  };
  return (
    <ReactTable
      entity="enrollments"
      baseUrl={`/api/v2/contexts/${courseId}/roster`}
      columns={getColumns()}
      defaultSortField="fullName"
      defaultSearchField="fullName"
      parseEntity={parseEnrolledUser}
      renderForm={renderForm}
      validateForm={validateForm}
      translations={T}
      submitForm={submitForm}
      setPortalAlertStatus={setPortalAlert}
      embed="enrollments,roles"
      getButtons={getButtonInfo}
      handleDelete={handleDelete}
      customFilters={customFilters}
      trClassFormat={trClassFormat}
      createButton={false}
      createDropdown={!readOnly}
      dropdownItems={dropdownItems}
      deleteButton={!readOnly}
      updateButton={!readOnly}
      getModalTitle={getModalTitle}
      openRow={sudo}
      headerExtra={searchPicker}
    />
  );
};

export default Roster;
