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

import Polyglot from 'node-polyglot';
import React from 'react';
import { UncontrolledTooltip } from 'reactstrap';

interface AdminFormSectionProps {
  children: React.ReactNode;
  page: string;
  section: string;
  translations: Polyglot;
}

const AdminFormSection: React.FC<AdminFormSectionProps> = ({
  children,
  page,
  section,
  translations: T,
}) => {
  const tooltipBodyKey = `adminPage.${page}.modal.help.${section}.body`;
  const tooltipHeaderKey = `adminPage.${page}.modal.help.${section}.header`;
  const tooltipId = `tooltip-${page}-${section}`;

  const header = T.has(tooltipHeaderKey) && (
    <h5 style={{ marginBottom: '1rem' }}>{T.t(tooltipHeaderKey)}</h5>
  );

  const tooltip = T.has(tooltipBodyKey) && (
    <UncontrolledTooltip
      placement="left"
      target={tooltipId}
    >
      {header}
      <span>{T.t(tooltipBodyKey)}</span>
    </UncontrolledTooltip>
  );

  const tooltipIcon = tooltip && (
    <div style={{ flex: 1, textAlign: 'right' }}>
      <i
        id={tooltipId}
        className="material-icons"
      >
        help
      </i>
      {tooltip}
    </div>
  );

  return (
    <React.Fragment>
      <h3 className="row block-header">
        {T.t(`adminPage.${page}.modal.headers.${section}`)}
        {tooltipIcon}
      </h3>
      <div className="my-3">{children}</div>
    </React.Fragment>
  );
};

export default AdminFormSection;
