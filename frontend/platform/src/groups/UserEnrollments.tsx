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
import moment from 'moment-timezone';
import React, { useEffect } from 'react';
import { useDispatch } from 'react-redux';
import { useParams } from 'react-router-dom';

import ReactTable from '../components/reactTable/ReactTable';
import { setPortalAlertStatus } from '../redux/actions/MainActions';
import { useLoPlatform, useTranslations } from '../redux/state';
import { inCurrTimeZone } from '../services/moment';
import EditAddUserEnrollment from './EditAddUserEnrollment';

interface UserEnrollmentsProps {
  setLastCrumb: (title: string, documentTitle?: string) => void;
  controllerValue: string;
  courseId: string;
}

const UserEnrollments: React.FC<UserEnrollmentsProps> = props => {
  const { controllerValue, courseId, setLastCrumb } = props;
  const { userId = '' } = useParams<{ userId: string }>();
  const T = useTranslations();
  const lo_platform = useLoPlatform();
  const dispatch = useDispatch();
  const rights = lo_platform.user.rights || [];
  const readOnly =
    !rights.includes('loi.cp.admin.right.CourseAdminRight') &&
    !rights.includes('loi.cp.course.right.ManageCoursesAdminRight');

  const setPortalAlert = (error: boolean, success: boolean, message: string) =>
    dispatch(setPortalAlertStatus(error, success, message));

  useEffect(() => {
    axios.get(`/api/v2/users/${userId}`).then(res => {
      setLastCrumb(T.t(`adminPage.${controllerValue}.enrollments.userEnrollments.name`, res.data));
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const formatTime = (t?: string) => {
    const dateTimeFormat = T.t('format.dateTime.compact');
    return t ? inCurrTimeZone(moment(t)).format(dateTimeFormat) : '';
  };

  const columns = [
    { dataField: 'id', isKey: true },
    { dataField: 'role_name', sortable: false, searchable: false },
    { dataField: 'disabled', sortable: false, searchable: false },
    { dataField: 'startTime', sortable: false, searchable: false, dataFormat: formatTime },
    { dataField: 'stopTime', sortable: false, searchable: false, dataFormat: formatTime },
  ];

  const renderForm = (row: Record<string, any>, validationErrors: Record<string, string>) => {
    return (
      <EditAddUserEnrollment
        T={T}
        row={row}
        validationErrors={validationErrors}
        courseId={courseId}
      />
    );
  };

  const validateForm = (form: Record<string, any>) => {
    const invalidF = (field: string) => {
      const params = { field: T.t(`adminPage.enrollments.userEnrollments.fieldName.${field}`) };
      return {
        validationErrors: { [field]: T.t('adminForm.validation.fieldMustBeValid', params) },
      };
    };
    if (!form.role) {
      const params = { field: T.t(`adminPage.enrollments.fieldName.role`) };
      return { validationErrors: { role: T.t('adminForm.validation.fieldIsRequired', params) } };
    }
    if (form.startTime && !moment(form.startTime).isValid()) {
      return invalidF('startTime');
    }
    if (
      form.stopTime &&
      (!moment(form.stopTime).isValid() ||
        (form.startTime && !moment(form.stopTime).isAfter(form.startTime)))
    ) {
      return invalidF('stopTime');
    }
    const data = {
      roleId: parseInt(form.role.trim(), 10),
      startTime: form.startTime ? moment(form.startTime).toISOString() : null,
      stopTime: form.stopTime ? moment(form.stopTime).toISOString() : null,
      disabled: form.active !== 'on',
      id: parseInt(form.id, 10),
    };
    return { data };
  };

  const createDeleteDTO = (_id: number) => ({ data: {}, headers: {} });

  const submitForm = ({ data, create }: { data: any; create: boolean }) => {
    const { id, ...payload } = data;
    if (create) {
      return axios
        .post(`/api/v2/courses/${courseId}/enrollments/byUser/${userId}`, payload)
        .then(res => res);
    } else {
      return axios.put(`/api/v2/courses/${courseId}/enrollments/${id}`, payload).then(res => res);
    }
  };

  const parseEntity = (entity: { disabled?: boolean; [key: string]: unknown }) => {
    const status = entity.disabled ? 'inactive' : 'active';
    return {
      ...entity,
      disabled: T.t(`adminPage.enrollments.userEnrollments.status.${status}`),
    };
  };

  const getDeleteUrl = (id: number) => {
    return `/api/v2/courses/${courseId}/enrollments/${id}`;
  };

  const handleDelete = {
    createDeleteDTO: createDeleteDTO,
    deleteMethod: 'delete',
    getDeleteUrl: getDeleteUrl,
  };

  return (
    <ReactTable
      entity="enrollments.userEnrollments"
      baseUrl={`/api/v2/courses/${courseId}/enrollments/byUser/${userId}`}
      columns={columns}
      parseEntity={parseEntity}
      defaultSortField=""
      defaultSearchField=""
      renderForm={renderForm}
      validateForm={validateForm}
      translations={T}
      submitForm={submitForm}
      setPortalAlertStatus={setPortalAlert}
      handleDelete={handleDelete}
      paginate={false}
      createButton={!readOnly}
      updateButton={!readOnly}
      deleteButton={!readOnly}
    />
  );
};

export default UserEnrollments;
