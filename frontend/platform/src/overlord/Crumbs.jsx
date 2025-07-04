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
import { connect } from 'react-redux';
import { Link } from 'react-router-dom';
import { Breadcrumb, BreadcrumbItem } from 'reactstrap';

import LoPropTypes from '../react/loPropTypes';

class Crumbs extends React.Component {
  render() {
    const { page, simple, T } = this.props;
    return (
      <Breadcrumb
        style={{ background: 'transparent' }}
        className="m-0"
      >
        {simple ? (
          <BreadcrumbItem active={!page}>
            <a
              href="/Administration"
              title="Administration"
            >
              <i
                className="material-icons md-24"
                aria-hidden="true"
              >
                admin_panel_settings
              </i>
            </a>
          </BreadcrumbItem>
        ) : (
          <BreadcrumbItem active={!page}>
            <Link
              to="/"
              className="overHome d-flex"
              title="Höm"
            >
              <i
                className="material-icons md-24"
                aria-hidden="true"
              >
                home
              </i>
            </Link>
          </BreadcrumbItem>
        )}
        {page && <BreadcrumbItem active>{T.t(`overlord.page.${page}.name`)}</BreadcrumbItem>}
      </Breadcrumb>
    );
  }
}

Crumbs.propTypes = {
  T: LoPropTypes.translations,
  page: PropTypes.string,
  simple: PropTypes.bool,
};

function mapStateToProps(state) {
  return {
    T: state.main.translations,
  };
}

export default connect(mapStateToProps, null)(Crumbs);
