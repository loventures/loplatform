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
import { Badge, UncontrolledTooltip } from 'reactstrap';

import { useEditedAssetDatum } from '../../graphEdit';
import { useDcmSelector } from '../../hooks';
import { NodeName } from '../../types/asset';
import { contentAccessRights } from '../contentStatus';
import { useEffectiveContentStatus } from '../hooks';

export const ContentStatusPill: React.FC<{ name: NodeName; size?: 'sm'; effective?: boolean }> = ({
  name,
  size,
  effective,
}) => {
  const actualStatus = useEditedAssetDatum(name, data => data.contentStatus);
  const effectiveStatus = useEffectiveContentStatus(name);
  const status = effective ? effectiveStatus : actualStatus;
  return (
    status && (
      <ContentStatusPillImpl
        name={name}
        size={size}
        status={status}
        inherited={!actualStatus}
      />
    )
  );
};

const ContentStatusPillImpl: React.FC<{
  name: NodeName;
  size?: 'sm';
  status: string;
  inherited: boolean;
}> = ({ name, size, status, inherited }) => {
  const config = useDcmSelector(state => state.configuration);
  const { role } = useDcmSelector(state => state.layout);
  const id = `content-status-${name}`;

  return (
    <>
      <Badge
        id={id}
        color="grey"
        className={classNames(
          'authoring-stage',
          size === 'sm' ? 'small' : 'ms-2',
          inherited && 'inherited'
        )}
        pill
      >
        {config.contentStatuses?.[status] ?? '???'}
      </Badge>
      {role && (
        <UncontrolledTooltip
          target={id}
          placement="right"
        >
          <ContentStatusTooltip
            role={role}
            status={status}
          />
        </UncontrolledTooltip>
      )}
    </>
  );
};

const ContentStatusTooltip: React.FC<{ role: string; status: string }> = ({ role, status }) => {
  const config = useDcmSelector(state => state.configuration);
  const access = config.contentAccessByRoleAndStatus?.[role]?.[status];
  const accessLabel = !access
    ? 'No Access'
    : access === '*'
      ? 'Full Access'
      : `${config.accessNames?.[access] ?? '???'}`;
  const accesses = config.contentAccesses?.[access] ?? [];
  const rights = accesses.map(access => contentAccessRights[access] ?? '???');

  return (
    <>
      {' '}
      <strong>{accessLabel}</strong>
      {rights.length ? (
        <div
          className="font-italic gray-400"
          style={{ fontSize: '.85rem' }}
        >
          {rights.join(', ')}
        </div>
      ) : null}
    </>
  );
};
