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

import { AdminFormCombobox, AdminFormField, AdminFormSelect } from '../components/adminForm';

interface Role {
  id: number;
  name: string;
}

interface EnrollmentRow {
  id: number;
  fullName?: string;
  userName?: string;
  roleId?: number;
}

interface EditAddEnrollmentProps {
  T: Polyglot;
  validationErrors: Record<string, string>;
  row: EnrollmentRow;
  courseId: string;
  searchBy: string;
  editing: boolean;
}

const EditAddEnrollment: React.FC<EditAddEnrollmentProps> = ({
  T,
  validationErrors,
  row,
  courseId,
  searchBy,
  editing,
}) => {
  const [roles, setRoles] = useState<Role[]>([]);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    axios.get(`/api/v2/roles/byContext/${courseId}`).then(roleRes => {
      const fetched: Role[] = roleRes.data.objects
        .map((role: { roleType: Role }) => role.roleType)
        .sort((a: Role, b: Role) => a.name.toLowerCase().localeCompare(b.name.toLowerCase()));
      setRoles(fetched);
      setLoaded(true);
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const renderUserNames = () => {
    const entity = 'enrollments';
    const field = 'users';
    const op = searchBy === 'fullName' ? 'ts' : 'sw';
    const formatUser = (suggestion: unknown) =>
      T.t(`adminPage.enrollments.userInput.displayName.${searchBy}`, suggestion as Polyglot.InterpolationOptions);
    return (
      <React.Fragment>
        <AdminFormCombobox
          entity={entity}
          field={field}
          targetEntity="users"
          matrixFilter={(value: string) => ({ property: searchBy, operator: op, value })}
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

  const renderUser = () => {
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

  const renderRoles = () => {
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

  if (!loaded) return null;
  return (
    <React.Fragment>
      {editing ? renderUser() : renderUserNames()}
      {editing ? renderRoles() : null}
    </React.Fragment>
  );
};

export default EditAddEnrollment;
