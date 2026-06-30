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
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useDispatch } from 'react-redux';
import { Button, Col, Input, InputGroup, Row } from 'reactstrap';

import { setPortalAlertStatus } from '../redux/actions/MainActions';
import { useTranslations } from '../redux/state';
import RightsTree from '../rights/RightsTree';

interface Role {
  id: number | string;
  name: string;
}

interface CourseRightsTreeProps {
  courseId: string;
}

const CourseRightsTree: React.FC<CourseRightsTreeProps> = props => {
  const { courseId } = props;
  const T = useTranslations();
  const dispatch = useDispatch();
  const [addRoleOptions, setAddRoleOptions] = useState<Role[]>([]);
  const [rolesLoaded, setRolesLoaded] = useState(false);
  const [role, setRole] = useState<string | number>('');
  const refreshRef = useRef<() => void>(() => undefined);

  const initState = useCallback(() => {
    const getKnownRoles = axios.get('/api/v2/domain/knownRoles');
    const getSupportedRoles = axios.get(`/api/v2/roles/byContext/${courseId}`);
    Promise.all([getKnownRoles, getSupportedRoles])
      .then(([knownRolesRes, supportedRolesRes]) => {
        const knownRoles: Role[] = knownRolesRes.data.objects.sort((a: Role, b: Role) =>
          a.name.localeCompare(b.name)
        );
        const supportedRoles = supportedRolesRes.data.objects;
        const supportedIds = supportedRoles.map((r: { roleType: Role }) => r.roleType.id);
        const filtered = knownRoles.filter(r => !supportedIds.includes(r.id));
        const guestSupported = supportedRoles
          .map((r: { roleType: Role }) => r.roleType.id)
          .includes(-1);
        const guest = T.t('adminPage.enrollments.rightsTree.roles.guest');
        const baseOptions: Role[] = !guestSupported
          ? [
              {
                id: '',
                name: guest,
              },
            ]
          : [];
        const options = baseOptions.concat(filtered);
        setRolesLoaded(true);
        setRole(!guestSupported ? guest : (options[0] && options[0].id) || '');
        setAddRoleOptions(options);
      })
      .catch(err => {
        console.log(err);
        const msg = T.t('error.unexpectedError');
        dispatch(setPortalAlertStatus(true, false, msg));
      });
  }, [courseId, T, dispatch]);

  useEffect(() => {
    initState();
  }, [initState]);

  const refreshTree = () => {
    refreshRef.current();
    initState();
  };

  const roleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setRole(e.target.value);
  };

  const addRole = () => {
    if (role) {
      const roleId = parseInt(role.toString(), 10);
      const data = {
        roleId: roleId,
      };
      axios
        .post(`/api/v2/roles/byContext/${courseId}`, data)
        .then(() => {
          refreshTree();
          const msg = T.t('adminPage.enrollments.rightsTree.roleAddedAlert');
          dispatch(setPortalAlertStatus(false, true, msg));
        })
        .catch(err => {
          console.log(err);
          const msg = T.t('error.unexpectedError');
          dispatch(setPortalAlertStatus(true, false, msg));
        });
    }
  };

  if (!rolesLoaded) return null;
  const options = addRoleOptions.map(r => {
    return (
      <option
        id={r.id.toString()}
        key={r.id}
        value={r.id}
      >
        {r.name}
      </option>
    );
  });
  return (
    <Row style={{ margin: '0.5em' }}>
      <Col sm={8}>
        <RightsTree
          {...props}
          translations={T}
          setPortalAlertStatus={(error, success, message) =>
            dispatch(setPortalAlertStatus(error, success, message))
          }
          rolesUrl={`/api/v2/roles/byContext/${courseId}`}
          rightTreeUrl="/api/v2/rights/course"
          rightsUrl="/api/v2/rights/course/all"
          postUrl="/api/v2/rights"
          contextId={courseId as unknown as number}
          refreshRef={(refresh: () => void) => (refreshRef.current = refresh)}
        />
      </Col>
      <Col sm={4}>
        <InputGroup>
          <Input
            type="select"
            onChange={roleChange}
          >
            {options}
          </Input>
          <Button
            color="secondary"
            onClick={addRole}
            className="ms-2"
          >
            {T.t('adminPage.enrollments.rightsTree.addRoleBtn.label')}
          </Button>
        </InputGroup>
      </Col>
    </Row>
  );
};

export default CourseRightsTree;
