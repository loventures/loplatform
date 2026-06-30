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
import Polyglot from 'node-polyglot';
import React, { useEffect, useState } from 'react';
import { Col } from 'reactstrap';

import { AdminFormCheck, AdminFormDateTime, AdminFormSelect } from '../components/adminForm';

interface Role {
  id: number | string;
  name: string;
}

interface UserEnrollmentRow {
  id?: number;
  role_id?: number;
  startTime?: string;
  stopTime?: string;
  disabled?: string;
  changeNotes?: string;
}

interface EditAddUserEnrollmentProps {
  T: Polyglot;
  validationErrors: Record<string, string>;
  row: UserEnrollmentRow;
  courseId: string;
}

const EditAddUserEnrollment: React.FC<EditAddUserEnrollmentProps> = ({
  T,
  validationErrors,
  row,
  courseId,
}) => {
  const [roles, setRoles] = useState<Role[]>([]);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    axios.get(`/api/v2/roles/byContext/${courseId}`).then(res => {
      const fetched: Role[] = [{ id: '', name: '' }].concat(
        res.data.objects
          .map((role: { roleType: Role }) => role.roleType)
          .sort((a: Role, b: Role) => a.name.toLowerCase().localeCompare(b.name.toLowerCase()))
      );
      setRoles(fetched);
      setLoaded(true);
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const renderRole = () => {
    return (
      <AdminFormSelect
        entity="enrollments.userEnrollments"
        field="role"
        value={row.role_id ? row.role_id.toString() : ''}
        options={roles}
        invalid={validationErrors.role}
        required={true}
        T={T}
      />
    );
  };

  const renderTimes = () => {
    return ['startTime', 'stopTime'].map(field => {
      return (
        <AdminFormDateTime
          key={field}
          field={field}
          value={row[field as 'startTime' | 'stopTime']}
          entity="enrollments.userEnrollments"
          invalid={validationErrors[field]}
          T={T}
        />
      );
    });
  };

  const renderActive = () => {
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

  const renderChangeNotes = (notes: string) => {
    const params = { notes: notes };
    return (
      <Col lg={{ size: 10, offset: 2 }}>
        <p>
          <small>{T.t('adminPage.enrollments.userEnrollments.changeNotes', params)}</small>
        </p>
      </Col>
    );
  };

  if (!loaded) return null;
  return (
    <React.Fragment>
      {renderRole()}
      {renderTimes()}
      {renderActive()}
      {row && row.changeNotes && renderChangeNotes(row.changeNotes)}
      <input
        type="hidden"
        name="id"
        value={row.id}
      />
    </React.Fragment>
  );
};

export default EditAddUserEnrollment;
