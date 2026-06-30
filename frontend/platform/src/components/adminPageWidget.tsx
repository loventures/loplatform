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

import React from 'react';
import { Link as RouterLink } from 'react-router-dom';

import { clearSavedTableState } from './reactTable/ReactTable';

// @types/react-router-dom resolves a nested @types/react (v19) whose ReactNode
// (incl. bigint) does not unify with the project's @types/react 18, so the
// router components are not seen as valid JSX. Treat them as loose components.
const Link = RouterLink as unknown as React.ComponentType<any>;

interface AdminPageWidgetProps {
  identifier: string;
  icon?: string | React.ElementType;
  iconName?: string;
  link?: string;
  href?: string;
  description?: string;
  title: string;
  entity?: string;
}

const AdminPageWidget: React.FC<AdminPageWidgetProps> = ({
  entity,
  href,
  link,
  identifier,
  icon: Icon,
  iconName,
  title,
  description,
}) => {
  const renderIcon = () =>
    iconName ? (
      <i
        className="admin-page-icon material-icons md-36"
        aria-hidden="true"
      >
        {iconName}
      </i>
    ) : typeof Icon === 'string' ? (
      <img
        className="admin-page-image"
        src={Icon}
        alt=""
        aria-hidden="true"
      />
    ) : Icon ? (
      <Icon className="admin-page-icon" />
    ) : null;

  return (
    <div className="admin-page-widget">
      {href ? (
        <a
          id={'adminPageLink-' + identifier}
          className="admin-page-link-wrapper"
          href={href}
        >
          {renderIcon()}
          <div className="admin-page-text">
            <span className="admin-page-link">{title}</span>
            <p className="admin-page-description">{description}</p>
          </div>
        </a>
      ) : (
        <Link
          id={'adminPageLink-' + identifier}
          className="admin-page-link-wrapper"
          to={link ?? ''}
          onClick={() => entity && clearSavedTableState(entity)}
        >
          {renderIcon()}
          <div className="admin-page-text">
            <span className="admin-page-link">{title}</span>
            <p className="admin-page-description">{description ?? 'UNKNOWN'}</p>
          </div>
        </Link>
      )}
    </div>
  );
};

export default AdminPageWidget;
