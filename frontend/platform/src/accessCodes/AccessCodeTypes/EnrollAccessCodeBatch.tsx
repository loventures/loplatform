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

import axios, { AxiosResponse } from 'axios';
import Polyglot from 'node-polyglot';
import React, { useState } from 'react';

import { AdminFormCombobox, AdminFormField, AdminFormSelect } from '../../components/adminForm';
import AccessCodeBatch, {
  AccessCodeBatchType,
  AccessCodeForm,
  ValidateFormResult,
  ValidationErrors,
} from './AccessCodeBatch';

interface Course {
  id: number;
  name: string;
  groupId: string;
  [key: string]: unknown;
}

interface RoleOption {
  id: number;
  key: number;
  text: string;
  roleId: string;
}

interface EnrollAccessCodeBatchProps {
  T: Polyglot;
  [key: string]: unknown;
}

const EnrollAccessCodeBatch: React.FC<EnrollAccessCodeBatchProps> = props => {
  const { T } = props;
  const [course, setCourse] = useState<Course | null>(null);
  const [roleOptions, setRoleOptions] = useState<RoleOption[]>([]);

  const onCourseChange = (selected: Course | null) => {
    if (selected) {
      axios.get(`/api/v2/roles/byContext/${selected.id}`).then(res => {
        const roles: RoleOption[] = res.data.objects
          .sort((a: any, b: any) =>
            a.roleType.name.toLowerCase().localeCompare(b.roleType.name.toLowerCase())
          )
          .map((role: any) => ({
            id: role.roleType.id,
            key: role.roleType.id,
            text: role.roleType.name,
            roleId: role.roleType.roleId,
          }));
        setCourse(selected);
        setRoleOptions(roles);
      });
    } else {
      setCourse(selected);
      setRoleOptions([]);
    }
  };

  const renderRedemptionLimit = (validationErrors: ValidationErrors) => {
    return (
      <AdminFormField
        entity="accessCodes"
        field="redemptionLimit"
        type="number"
        value="1"
        T={T}
        required={true}
        invalid={validationErrors.redemptionLimit}
      />
    );
  };

  const renderRole = (validationErrors: ValidationErrors) => {
    return (
      <AdminFormSelect
        key={course ? course.id : 'course-role'}
        entity="accessCodes"
        field="role"
        options={roleOptions}
        value={
          roleOptions.length ? roleOptions.find(r => r.roleId === 'student')!.id + '' : undefined
        }
        T={T}
        required={true}
        disabled={!course}
        invalid={validationErrors.role}
      />
    );
  };

  const renderCourseSections = (validationErrors: ValidationErrors) => {
    const matrixFilter = (value: string) => ({ property: 'name', operator: 'co', value });
    return (
      <AdminFormCombobox
        entity="accessCodes"
        field="courseId"
        targetEntity="courseSections"
        matrixFilter={matrixFilter}
        onChange={onCourseChange}
        value={course}
        T={T}
        required={true}
        invalid={validationErrors.courseId}
        dataFormat={(c: Course) => `${c.name} (${c.groupId})`}
      />
    );
  };

  const extraFormFields = (validationErrors: ValidationErrors) => {
    return (
      <React.Fragment>
        {renderCourseSections(validationErrors)}
        {renderRole(validationErrors)}
        {renderRedemptionLimit(validationErrors)}
      </React.Fragment>
    );
  };

  return (
    <AccessCodeBatch
      {...(props as any)}
      type="enrollAccessCodeBatch"
      componentIdentifier="loi.cp.context.accesscode.EnrollAccessCodeBatch"
      extraFormFields={extraFormFields}
      canGenerate
      hasDuration
    />
  );
};

const validateForm = (form: AccessCodeForm, T: Polyglot): ValidateFormResult => {
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
    ? {
        validationErrors: {
          [missing]: T.t('adminForm.validation.fieldIsRequired', params || {}),
        },
      }
    : { data };
};

const afterCreateOrUpdate = (res: AxiosResponse, form: AccessCodeForm): Promise<AxiosResponse> => {
  const generating = form.generating;
  const submitPath = generating ? 'generate' : 'import';
  const url = `/api/v2/accessCodes/batches/${res.data.id}/${submitPath}`;
  const queryString = generating
    ? `?prefix=${form.prefix}&quantity=${form.quantity}`
    : `?upload=${form.guid}&skipHeader=${form.skipFirstRow === 'on'}`;
  // NB: `config` here is not an axios option; preserved verbatim as a latent quirk.
  return axios({
    method: 'post',
    url: url + queryString,
    data: {},
    config: { headers: { 'Content-Type': 'multipart/form-data' } },
  } as any).then(() => res);
};

const EnrollAccessCodeBatchType: AccessCodeBatchType = {
  component: EnrollAccessCodeBatch,
  validateForm: validateForm,
  afterCreateOrUpdate: afterCreateOrUpdate,
  id: 'enrollmentAccessCodeBatch',
};

export default EnrollAccessCodeBatchType;
