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

import PropTypes from 'prop-types';
import React from 'react';
import { Link } from 'react-router-dom';

import { clearSavedTableState } from './reactTable/ReactTable';

class AdminPageWidget extends React.Component {
  render() {
    const { entity, href, link, identifier, icon: Icon, iconName, title, description } = this.props;
    return (
      <div className="admin-page-widget">
        {href ? (
          <a
            id={'adminPageLink-' + identifier}
            className="admin-page-link-wrapper"
            href={href}
          >
            {iconName ? (
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
            ) : (
              <Icon className="admin-page-icon" />
            )}
            <div className="admin-page-text">
              <span className="admin-page-link">{title}</span>
              <p className="admin-page-description">{description}</p>
            </div>
          </a>
        ) : (
          <Link
            id={'adminPageLink-' + identifier}
            className="admin-page-link-wrapper"
            to={link}
            onClick={() => entity && clearSavedTableState(entity)}
          >
            {iconName ? (
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
            ) : (
              <Icon className="admin-page-icon" />
            )}
            <div className="admin-page-text">
              <span className="admin-page-link">{title}</span>
              <p className="admin-page-description">{description ?? 'UNKNOWN'}</p>
            </div>
          </Link>
        )}
      </div>
    );
  }
}

AdminPageWidget.propTypes = {
  identifier: PropTypes.string.isRequired,
  icon: PropTypes.oneOf([PropTypes.string, PropTypes.elementType]),
  iconName: PropTypes.string,
  link: PropTypes.string,
  href: PropTypes.string,
  description: PropTypes.string,
  title: PropTypes.string.isRequired,
  entity: PropTypes.string,
};

export default AdminPageWidget;
