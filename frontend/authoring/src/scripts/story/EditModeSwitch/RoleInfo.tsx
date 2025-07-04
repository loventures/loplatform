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

import classNames from 'classnames';
import React from 'react';
import { BsChevronRight } from 'react-icons/bs';
import { IoCheckmarkOutline, IoEyeOutline } from 'react-icons/io5';
import { useDispatch } from 'react-redux';
import { Button, Card, CardBody, CardHeader, Collapse } from 'reactstrap';

import { useDcmSelector } from '../../hooks';
import { editModeAction, editRoleAction } from '../storyActions';
import { ContentAccess } from './ContentAccess';
import { ProjectAccess } from './ProjectAccess';

export const RoleInfo: React.FC<{
  role: string;
  label: string;
  tab: string;
  admin: boolean;
  setTab: (tab: string | null) => void;
  toggle: () => void;
}> = ({ role, label, tab, setTab, toggle, admin }) => {
  const dispatch = useDispatch();
  const config = useDcmSelector(state => state.configuration);
  const current = useDcmSelector(state => state.layout.role === role);
  const expanded = tab === role;
  return (
    <Card>
      <CardHeader
        className="d-flex align-items-center"
        onClick={() => setTab(tab === role ? null : role)}
      >
        {admin && (
          <Button
            size="small"
            color="transparent"
            className={classNames(
              'mini-button p-1 d-inline-flex align-items-center justify-content-center role-toggle',
              expanded && 'expanded'
            )}
            style={{ lineHeight: 1 }}
            onClick={e => {
              e.stopPropagation();
              setTab(tab === role ? null : role);
            }}
          >
            <BsChevronRight size="1rem" />
          </Button>
        )}
        <span className="fw-bold d-flex">
          {label}
          {current && admin && (
            <div className="ms-1">
              <IoCheckmarkOutline />
            </div>
          )}
        </span>
        <div className="flex-grow-1"></div>
        {admin && (
          <Button
            color="dark"
            outline
            className="d-flex align-items-center"
            title={`Preview as ${label}`}
            onClick={e => {
              e.stopPropagation();
              dispatch(editModeAction(true));
              dispatch(editRoleAction(role));
              toggle();
            }}
          >
            <IoEyeOutline />
          </Button>
        )}
      </CardHeader>
      <Collapse isOpen={expanded}>
        <CardBody>
          <h6 className="fw-bold">Project Access</h6>
          {Object.entries(config.projectStatuses).map(([status, label]) => (
            <ProjectAccess
              key={status}
              label={label}
              role={role}
              status={status}
              config={config}
            />
          ))}
          <h6 className="fw-bold mt-3">Content Access</h6>
          {Object.entries(config.contentStatuses).map(([status, label]) => (
            <ContentAccess
              key={status}
              label={label}
              role={role}
              status={status}
              config={config}
            />
          ))}
        </CardBody>
      </Collapse>
    </Card>
  );
};
