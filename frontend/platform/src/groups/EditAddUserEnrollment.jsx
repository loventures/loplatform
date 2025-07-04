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
import { Col } from 'reactstrap';

import { AdminFormCheck, AdminFormDateTime, AdminFormSelect } from '../components/adminForm';

class EditAddUserEnrollment extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      loaded: false,
      roles: [],
    };
  }

  componentDidMount() {
    const { courseId } = this.props;
    axios.get(`/api/v2/roles/byContext/${courseId}`).then(res => {
      const roles = [{ id: '', name: '' }].concat(
        res.data.objects
          .map(role => role.roleType)
          .sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase()))
      );
      this.setState({ roles: roles, loaded: true });
    });
  }

  renderRole = () => {
    const { T, validationErrors, row } = this.props;
    const { roles } = this.state;
    return (
      <AdminFormSelect
        entity="enrollments.userEnrollments"
        field="role"
        value={row.role_id && row.role_id.toString()}
        options={roles}
        invalid={validationErrors.role}
        required={true}
        T={T}
      />
    );
  };

  renderTimes = () => {
    const { T, validationErrors, row } = this.props;
    return ['startTime', 'stopTime'].map(field => {
      return (
        <AdminFormDateTime
          key={field}
          field={field}
          value={row[field]}
          entity="enrollments.userEnrollments"
          invalid={validationErrors[field]}
          T={T}
        />
      );
    });
  };

  renderActive = () => {
    const { T, row } = this.props;
    return (
      <AdminFormCheck
        entity="enrollments.userEnrollments"
        field="active"
        label={T.t('adminPage.enrollments.userEnrollments.disabledLabel')}
        value={row.disabled !== 'Inactive'}
        T={T}
      />
    );
  };

  renderChangeNotes = notes => {
    const { T } = this.props;
    const params = { notes: notes };
    return (
      <Col lg={{ size: 10, offset: 2 }}>
        <p>
          <small>{T.t('adminPage.enrollments.userEnrollments.changeNotes', params)}</small>
        </p>
      </Col>
    );
  };

  render() {
    const { row } = this.props;
    const { loaded } = this.state;
    if (!loaded) return null;
    return (
      <React.Fragment>
        {this.renderRole()}
        {this.renderTimes()}
        {this.renderActive()}
        {row && row.changeNotes && this.renderChangeNotes(row.changeNotes)}
        <input
          type="hidden"
          name="id"
          value={row.id}
        />
      </React.Fragment>
    );
  }
}

EditAddUserEnrollment.propTypes = {
  T: PropTypes.object.isRequired,
  validationErrors: PropTypes.object.isRequired,
  row: PropTypes.object.isRequired,
  courseId: PropTypes.string.isRequired,
};

export default EditAddUserEnrollment;
