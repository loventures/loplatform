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

import gretchen from '../../../grfetchen/';
import { omit, sortBy } from 'lodash';
import React, { useMemo, useState } from 'react';
import { IoClose } from 'react-icons/io5';
import Select from 'react-select';
import AsyncSelect from 'react-select/async';
import { Button, Col, Label, Row } from 'reactstrap';

import { useDcmSelector } from '../../../hooks';
import { encodeQuery } from '../../../srs/apiQuery';
import { User } from '../../../types/user';
import { stockClassnames } from '../../AlignmentEditor/Aligner';

const userStr = (user: User) =>
  user
    ? `${user.givenName} ${user.familyName} <${user.emailAddress || user.userName}>`
    : 'Unknown User';

const fetchUsers = (value: string) => {
  const query = encodeQuery({
    limit: 10,
    filter: [
      { property: 'fullName', operator: 'ts', value },
      { property: 'emailAddress', operator: 'sw', value },
      { property: 'userName', operator: 'sw', value },
    ],
    filterOp: 'or',
  });
  return gretchen
    .get(`/api/v2/authoring/authors${query}`)
    .exec()
    .then(res =>
      res.objects.map(user => ({
        ...user,
        label: `${user.fullName} <${user.emailAddress}>`,
      }))
    );
};
export const ProjectContributors: React.FC<{
  editMode: boolean;
  users: Record<number, User>;
  contributors: Record<number, string | null>;
  setContributors: (contributors: Record<number, string | null>) => void;
}> = ({ editMode, users, contributors, setContributors }) => {
  const roles = useDcmSelector(state => state.configuration.authoringRoles);
  const roleOptions = useMemo(
    () =>
      [{ value: 'Owner', label: 'Project Owner' }].concat(
        sortBy(
          Object.entries(roles).map(([value, label]) => ({ value, label })),
          'label'
        )
      ),
    [roles]
  );
  const [extraUsers, setExtraUsers] = useState<Record<number, User>>({});

  return (
    <div className="d-flex flex-column gap-2 mt-3">
      <Label
        className="gray-700 mb-2 "
        style={{ fontSize: '1.1rem' }}
      >
        Project Members
      </Label>

      {Object.entries(contributors).map(([id, role]) => (
        <Row
          key={id}
          className="contributor-row"
        >
          <Col
            lg={7}
            className="d-flex align-items-center"
          >
            {userStr(users[id] ?? extraUsers[id])}
          </Col>
          <Col
            lg={5}
            className="d-flex align-items-center"
          >
            <Select
              className="Select secretly no-indicators flex-grow-1"
              classNames={stockClassnames}
              value={roleOptions.find(r => r.value === role) ?? null}
              options={roleOptions}
              isDisabled={!editMode}
              isClearable={true}
              onChange={o => {
                const role = o?.value ?? null;
                setContributors({
                  ...contributors,
                  [id]: role,
                });
              }}
            />
            {editMode && (
              <Button
                outline
                color="danger"
                className="ms-1 p-2 d-flex align-items-center border-0 remove-contributor"
                onClick={() => setContributors(omit(contributors, [id]))}
              >
                <IoClose />
              </Button>
            )}
          </Col>
        </Row>
      ))}
      {editMode && (
        <Row>
          <Col className="d-flex align-items-lg-center text-muted flex-column flex-lg-row align-items-start">
            <span className="me-3">Add new project member:</span>
            <AsyncSelect<User>
              id="new-contributor"
              className="Select secretly flex-grow-1"
              classNames={stockClassnames}
              loadOptions={fetchUsers}
              styles={{ container: s => ({ ...s, marginRight: '2.25rem' }) }}
              placeholder="Full name, username or email address..."
              onChange={o => {
                setExtraUsers({ [o.id]: o, ...extraUsers });
                setContributors({ [o.id]: null, ...contributors });
              }}
              value={null}
            />
          </Col>
        </Row>
      )}

      {Object.values(contributors).filter(role => role === 'Owner').length !== 1 && (
        <div
          id="owner-alert"
          className="text-danger small text-right"
        >
          There must be exactly one Project Owner.
        </div>
      )}
    </div>
  );
};
