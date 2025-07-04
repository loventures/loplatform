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
import { Dropdown, DropdownItem, DropdownMenu, DropdownToggle } from 'reactstrap';
import { bindActionCreators } from 'redux';

import ReactTable, { clearSavedTableState } from '../components/reactTable/ReactTable';
import LoPropTypes from '../react/loPropTypes';
import * as MainActions from '../redux/actions/MainActions';
import EditAddEnrollment from './EditAddEnrollment';

class Roster extends React.Component {
  constructor() {
    super();
    this.state = {
      course: {},
      domainRoles: [],
      dropdownItems: [],
      userNameToId: {},
      roleId: null,
      loaded: false,
      searchBy: 'userName',
    };
    this.customFilters = [
      {
        property: 'includeInactive',
        value: '',
        prefilter: true,
      },
    ];
  }

  trClassFormat = ({ user_type, disabled }) =>
    user_type === 'Preview' ? 'row-preview' : disabled === 'Inactive' ? 'row-disabled' : '';

  formatRole = (role, row) => {
    return row.user_type === 'Preview' ? 'Preview ' + role : role;
  };

  getColumns = () => {
    const roleFilterOptions = this.state.domainRoles.map(role => {
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
        dataFormat: this.formatRole,
        width: '15%',
      },
      { dataField: 'disabled', sortable: false, searchable: false, hidden: true },
      { dataField: 'fullName', sortable: true, searchable: true, searchOperator: 'ts' },
      { dataField: 'emailAddress', sortable: true, searchable: true, searchOperator: 'sw' },
      { dataField: 'userName', sortable: true, searchable: true, searchOperator: 'sw' },
      { dataField: 'externalId', sortable: true, searchable: true, searchOperator: 'sw' },
    ];
  };

  unexpectedError = e => {
    console.log(e);
    const T = this.props.translations;
    this.props.setPortalAlertStatus(true, false, T.t('error.unexpectedError'));
  };

  componentDidMount() {
    const { controllerValue, courseId } = this.props;
    const getCourse = axios.get(`/api/v2/${controllerValue}/${courseId}`);
    const getRoles = axios.get(`/api/v2/roles/byContext/${courseId}`);
    Promise.all([getCourse, getRoles])
      .then(([courseRes, domainRolesRes]) => {
        const roles = domainRolesRes.data.objects
          .map(role => role.roleType)
          .sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase()));
        const dropdownItems = roles.map(role => ({
          key: role.name,
          name: role.name,
          onClick: () => {
            this.setState({ type: role.name, roleId: role.id });
          },
        }));
        this.setState({
          course: courseRes.data,
          domainRoles: roles,
          dropdownItems: dropdownItems,
          loaded: true,
        });
      })
      .catch(this.unexpectedError);
  }

  userEnrollments = ({ id }) => {
    const { controllerValue, match } = this.props;
    const { course } = this.state;
    clearSavedTableState('enrollments.userEnrollments');
    const path = `${match.url}/${id}`;
    const state = {
      course: course,
      controllerValue: controllerValue,
    };
    this.props.history.push(path, state);
    return Promise.resolve(false);
  };

  rights = () => {
    const { controllerValue, match } = this.props;
    const { course } = this.state;
    const path = `${match.url}/Rights`;
    const state = {
      course: course,
      controllerValue: controllerValue,
    };
    this.props.history.push(path, state);
    return Promise.resolve(false);
  };

  sudo = selectedRow => {
    const { course } = this.state;
    const returnUrl = window.location.pathname;
    return axios
      .post(`/api/v2/users/${selectedRow.id}/sudo?returnUrl=${encodeURIComponent(returnUrl)}`)
      .then(() => {
        window.history.replaceState(
          {},
          'Exit',
          `/sys/eunt/domus${returnUrl}?user=${this.props.lo_platform.user.id}`
        );
        window.top.location.href = this.props.sudoUrl(course, selectedRow);
        return false;
      })
      .catch(this.unexpectedError);
  };

  transition = selectedRow => {
    const {
      course: { id },
    } = this.state;
    const userId = selectedRow.id;
    const url = `/api/v2/courses/${id}/enrollments/byUser/${userId}/transition`;
    const data = {
      disabled: selectedRow.disabled === 'Active',
    };
    return axios
      .post(url, data)
      .then(() => true)
      .catch(this.unexpectedError);
  };

  getButtonInfo = selectedRow => {
    const {
      props: { includeRights, readOnly },
      state: {
        course: { fjœr },
      },
    } = this;

    const changeStateButton = readOnly
      ? []
      : [
          {
            name: !selectedRow || selectedRow.disabled === 'Active' ? 'suspend' : 'reinstate',
            iconName:
              !selectedRow || selectedRow.disabled === 'Active' ? 'not_interested' : 'check',
            onClick: this.transition,
          },
        ];

    const buttons = [
      ...changeStateButton,
      {
        name: 'enrollments',
        iconName: 'list',
        onClick: this.userEnrollments,
      },
      {
        name: 'sudo',
        iconName: 'directions_run',
        onClick: this.sudo,
        disabled: !fjœr || selectedRow?.user_type === 'Preview',
      },
    ];
    if (includeRights && !readOnly) {
      buttons.push({
        name: 'rights',
        iconName: 'gavel',
        onClick: this.rights,
        alwaysEnabled: true,
      });
    }
    return buttons;
  };

  renderForm = (row, validationErrors) => {
    const editing = Object.keys(row).length > 0;
    const {
      props: { translations: T, courseId },
      state: { searchBy },
    } = this;
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

  validateForm = (form, row) => {
    const { translations: T } = this.props;
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
      const { roleId } = this.state;
      if (!form.users) {
        const params = { field: T.t(`adminPage.enrollments.fieldName.users`) };
        return { validationErrors: { users: T.t('adminForm.validation.fieldIsRequired', params) } };
      } else {
        const ids = (typeof form.users === 'string' ? [form.users] : form.users).map(id =>
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

  createDeleteDTO = () => ({ data: {} });

  submitForm = ({ data, create }) => {
    const { course } = this.state;
    if (create) {
      return axios.post(`/api/v2/courses/${course.id}/enrollments/batch`, data).then(res => res);
    } else {
      const { userId, ...rest } = data;
      return axios
        .put(`/api/v2/courses/${course.id}/enrollments/byUser/${userId}`, rest)
        .then(res => res);
    }
  };

  parseEnrolledUser = enrolledUser => {
    const { translations: T } = this.props;
    const { course } = this.state;
    const enrollments = enrolledUser.enrollments || [];
    const enabled = enrollments
      .filter(enrollment => enrollment.context_id === course.id)
      .map(enrollment => enrollment.disabled)
      .includes(false);
    const status = enabled ? 'active' : 'inactive';
    const role = enrolledUser.roles.map(role => role.name).join(', ');
    return {
      ...enrolledUser,
      role: role || T.t('adminPage.enrollments.role.none'),
      roleId: enrolledUser.roles.length && enrolledUser.roles[0].id,
      disabled: T.t(`adminPage.enrollments.status.${status}`),
    };
  };

  getDeleteUrl = id => {
    const { course } = this.state;
    return `/api/v2/courses/${course.id}/enrollments/byUser/${id}`;
  };

  getModalTitle = modalType => {
    const { translations: T } = this.props;
    const { type } = this.state;
    if (modalType === 'create') {
      return T.t('adminPage.enrollments.modal.create.title', { type: type });
    }
    return null;
  };

  searchPicker = (row, modalType) => {
    const {
      props: { translations: T },
      state: { dropdownOpen, searchBy },
    } = this;
    const searchOptions = ['fullName', 'userName', 'externalId', 'emailAddress'];
    const onSearch = searchBy => this.setState({ searchBy });
    const toggle = () => this.setState(s => ({ dropdownOpen: !s.dropdownOpen }));
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

  render() {
    const { translations, setPortalAlertStatus, courseId, readOnly } = this.props;
    const { loaded, dropdownItems } = this.state;
    if (!loaded) return null;
    const handleDelete = {
      createDeleteDTO: this.createDeleteDTO,
      deleteMethod: 'delete',
      getDeleteUrl: this.getDeleteUrl,
    };
    return (
      <ReactTable
        entity="enrollments"
        baseUrl={`/api/v2/contexts/${courseId}/roster`}
        columns={this.getColumns()}
        defaultSortField="fullName"
        defaultSearchField="fullName"
        parseEntity={this.parseEnrolledUser}
        renderForm={this.renderForm}
        validateForm={this.validateForm}
        translations={translations}
        submitForm={this.submitForm}
        setPortalAlertStatus={setPortalAlertStatus}
        embed="enrollments,roles"
        getButtons={this.getButtonInfo}
        handleDelete={handleDelete}
        customFilters={this.customFilters}
        trClassFormat={this.trClassFormat}
        createButton={false}
        createDropdown={!readOnly}
        dropdownItems={dropdownItems}
        deleteButton={!readOnly}
        updateButton={!readOnly}
        getModalTitle={this.getModalTitle}
        openRow={this.sudo}
        headerExtra={this.searchPicker}
      />
    );
  }
}

Roster.propTypes = {
  lo_platform: PropTypes.object.isRequired,
  translations: LoPropTypes.translations,
  setPortalAlertStatus: PropTypes.func.isRequired,
  controllerValue: PropTypes.string.isRequired,
  match: PropTypes.object.isRequired,
  sudoUrl: PropTypes.func.isRequired,
  courseId: PropTypes.string.isRequired,
  includeRights: PropTypes.bool.isRequired,
  readOnly: PropTypes.bool.isRequired,
};

function mapStateToProps(state) {
  return {
    lo_platform: state.main.lo_platform,
    translations: state.main.translations,
  };
}

function mapDispatchToProps(dispatch) {
  return bindActionCreators(MainActions, dispatch);
}

export default connect(mapStateToProps, mapDispatchToProps)(Roster);
