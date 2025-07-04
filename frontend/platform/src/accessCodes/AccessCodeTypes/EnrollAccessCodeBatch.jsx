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
import React from 'react';
import PropTypes from 'prop-types';
import AccessCodeBatch from './AccessCodeBatch';
import { AdminFormCombobox, AdminFormField, AdminFormSelect } from '../../components/adminForm';

class EnrollAccessCodeBatch extends React.Component {
  state = {
    course: null,
    roleOptions: [],
  };

  onCourseChange = course => {
    if (course) {
      axios.get(`/api/v2/roles/byContext/${course.id}`).then(res => {
        const roles = res.data.objects
          .sort((a, b) =>
            a.roleType.name.toLowerCase().localeCompare(b.roleType.name.toLowerCase())
          )
          .map(role => ({
            id: role.roleType.id,
            key: role.roleType.id,
            text: role.roleType.name,
            roleId: role.roleType.roleId,
          }));
        this.setState({
          course: course,
          roleOptions: roles,
        });
      });
    } else {
      this.setState({ course: course, roleOptions: [] });
    }
  };

  renderRedemptionLimit = validationErrors => {
    return (
      <AdminFormField
        entity="accessCodes"
        field="redemptionLimit"
        type="number"
        value="1"
        T={this.props.T}
        required={true}
        invalid={validationErrors.redemptionLimit}
      />
    );
  };

  renderRole = validationErrors => {
    const { course, roleOptions } = this.state;
    return (
      <AdminFormSelect
        key={course ? course.id : 'course-role'}
        entity="accessCodes"
        field="role"
        options={roleOptions}
        value={roleOptions.length ? roleOptions.find(r => r.roleId === 'student').id + '' : null}
        T={this.props.T}
        required={true}
        disabled={!course}
        invalid={validationErrors.role}
      />
    );
  };

  renderCourseSections = validationErrors => {
    const matrixFilter = value => ({ property: 'name', operator: 'co', value });
    return (
      <AdminFormCombobox
        entity="accessCodes"
        field="courseId"
        targetEntity="courseSections"
        matrixFilter={matrixFilter}
        onChange={this.onCourseChange}
        value={this.state.course}
        T={this.props.T}
        required={true}
        invalid={validationErrors.courseId}
        dataFormat={course => `${course.name} (${course.groupId})`}
      />
    );
  };

  extraFormFields = validationErrors => {
    return (
      <React.Fragment>
        {this.renderCourseSections(validationErrors)}
        {this.renderRole(validationErrors)}
        {this.renderRedemptionLimit(validationErrors)}
      </React.Fragment>
    );
  };

  render() {
    return (
      <AccessCodeBatch
        {...this.props}
        type="enrollAccessCodeBatch"
        componentIdentifier="loi.cp.context.accesscode.EnrollAccessCodeBatch"
        extraFormFields={this.extraFormFields}
        canGenerate
        hasDuration
      />
    );
  }
}

EnrollAccessCodeBatch.propTypes = {
  T: PropTypes.object.isRequired,
};

const validateForm = (form, T) => {
  console.log(form);
  const generating = form.generating;
  const baseReqs = ['name', 'duration', 'redemptionLimit', 'courseId', 'role'];
  const data = {
    name: form.name,
    duration: form.duration,
    courseId: parseInt(form.courseId, 10),
    role: form.role,
    disabled: false,
    redemptionLimit: parseInt(form.redemptionLimit, 10),
  };
  let missing = baseReqs.find(field => !form[field]);
  if (!missing) {
    if (generating) {
      missing = ['prefix', 'quantity'].find(field => !form[field]);
    } else if (!form.guid) {
      missing = 'csv';
    }
  }
  const params = missing && { field: T.t(`adminPage.accessCodes.fieldName.${missing}`) };
  return missing
    ? { validationErrors: { [missing]: T.t('adminForm.validation.fieldIsRequired', params) } }
    : { data };
};

const afterCreateOrUpdate = (res, form) => {
  const generating = form.generating;
  const submitPath = generating ? 'generate' : 'import';
  const url = `/api/v2/accessCodes/batches/${res.data.id}/${submitPath}`;
  const queryString = generating
    ? `?prefix=${form.prefix}&quantity=${form.quantity}`
    : `?upload=${form.guid}&skipHeader=${form.skipFirstRow === 'on'}`;
  return axios({
    method: 'post',
    url: url + queryString,
    data: {},
    config: { headers: { 'Content-Type': 'multipart/form-data' } },
  }).then(() => res);
};

export default {
  component: EnrollAccessCodeBatch,
  validateForm: validateForm,
  afterCreateOrUpdate: afterCreateOrUpdate,
  id: 'enrollmentAccessCodeBatch',
};
