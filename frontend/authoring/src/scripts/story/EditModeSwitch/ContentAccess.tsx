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

import React from 'react';

import { ContentStatusConfiguration, contentAccessRights } from '../contentStatus';

export const ContentAccess: React.FC<{
  label: string;
  role: string;
  status: string;
  config: ContentStatusConfiguration;
}> = ({ label, role, status, config }) => {
  const roleAccess = config.contentAccessByRoleAndStatus[role];
  const access = roleAccess?.['*'] ?? roleAccess?.[status];
  const accesses =
    access === '*' ? Object.keys(contentAccessRights) : (config.contentAccesses[access] ?? []);
  const accessLabel = !access
    ? 'No Access'
    : access === '*'
      ? 'Full Access'
      : (config.accessNames[access] ?? '???');
  const rights = accesses.map(access => contentAccessRights[access] ?? '???');
  return (
    <div className="hanging-indent">
      <span>{label}</span> – <span>{accessLabel}</span>
      {rights.length ? (
        <span
          className="text-muted font-italic"
          style={{ fontSize: '.85rem' }}
        >
          {' '}
          ({rights.join(', ')})
        </span>
      ) : null}
    </div>
  );
};
