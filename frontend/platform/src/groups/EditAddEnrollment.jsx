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

import { AdminFormCombobox, AdminFormField, AdminFormSelect } from '../components/adminForm';

class EditAddEnrollment extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      tags: [],
      suggestions: [],
      roles: [],
      value: '',
      loaded: false,
    };
  }

  componentDidMount() {
    const { courseId } = this.props;
    axios.get(`/api/v2/roles/byContext/${courseId}`).then(roleRes => {
      const roles = roleRes.data.objects
        .map(role => role.roleType)
        .sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase()));
      this.setState({
        roles: roles,
        loaded: true,
      });
    });
  }

  renderUserNames = () => {
    const { searchBy, validationErrors, T } = this.props;
    const entity = 'enrollments';
    const field = 'users';
    const op = searchBy === 'fullName' ? 'ts' : 'sw';
    const formatUser = suggestion =>
      T.t(`adminPage.enrollments.userInput.displayName.${searchBy}`, suggestion);
    return (
      <React.Fragment>
        <AdminFormCombobox
          entity={entity}
          field={field}
          targetEntity="users"
          matrixFilter={value => ({ property: searchBy, operator: op, value })}
          matrixOrder={() => 'fullName'}
          dataFormat={formatUser}
          multiSelect={true}
          invalid={validationErrors[field]}
          required={true}
          T={T}
        />
      </React.Fragment>
    );
  };

  renderUser = () => {
    const { T, row, validationErrors } = this.props;
    const field = 'user';
    const value = `${row.fullName} (${row.userName})`;
    return (
      <React.Fragment>
        <AdminFormField
          label="User"
          key={field}
          required={true}
          invalid={validationErrors['user']}
          entity="enrollments"
          field={field}
          disabled={true}
          value={value}
          T={T}
        />
        <input
          type="hidden"
          name="userId"
          value={row.id.toString()}
        />
      </React.Fragment>
    );
  };

  renderRoles = () => {
    const { T, validationErrors, row } = this.props;
    const { roles } = this.state;
    return (
      <AdminFormSelect
        entity="enrollments"
        field="role"
        value={row.roleId ? row.roleId.toString() : ''}
        options={roles}
        invalid={validationErrors['role']}
        required={true}
        T={T}
      />
    );
  };

  render() {
    const { loaded } = this.state;
    const { editing } = this.props;
    if (!loaded) return null;
    return (
      <React.Fragment>
        {editing ? this.renderUser() : this.renderUserNames()}
        {editing ? this.renderRoles() : null}
      </React.Fragment>
    );
  }
}

EditAddEnrollment.propTypes = {
  T: PropTypes.object.isRequired,
  validationErrors: PropTypes.object.isRequired,
  row: PropTypes.object.isRequired,
  courseId: PropTypes.string.isRequired,
  searchBy: PropTypes.string.isRequired,
  editing: PropTypes.bool.isRequired,
};

export default EditAddEnrollment;
