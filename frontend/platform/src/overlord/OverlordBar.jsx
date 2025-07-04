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
import { Nav, Navbar, NavbarBrand } from 'reactstrap';

import OctopodeDotPng from '../imgs/octopod.png';
import { isDevelopment } from '../services';
import { OverlordRight, hasRight } from '../services/Rights';
import Crumbs from './Crumbs';
import Logout from './Logout';
import OverMenu from './OverMenu';

const optOutEes = ['nvanaartsen'];
const optedOutOfNiceThings = user => user && optOutEes.indexOf(user.userName) >= 0;

class OverlordBar extends React.Component {
  render() {
    const { history, lo_platform, page, simple } = this.props;

    const clusterName = lo_platform.clusterName
      .split('-')
      .map(s => s.substring(0, 1).toUpperCase() + s.substring(1))
      .join(' ');

    const divs = i => <div> {i > 0 ? divs(i - 1) : null} </div>;

    const overlord = isDevelopment || (!simple && hasRight(lo_platform.user, OverlordRight));

    return (
      <Navbar
        light
        expand
        color="charming"
        style={{ padding: '.25rem .5rem' }}
      >
        <Nav navbar>
          <Crumbs
            page={page}
            simple={simple}
          />
        </Nav>
        <NavbarBrand>{clusterName}</NavbarBrand>
        <Nav
          navbar
          className="ms-auto"
        >
          {overlord && <OverMenu />}
          <Logout history={history} />
        </Nav>
        {optedOutOfNiceThings(lo_platform.user) ? (
          <img
            className="octo-wannabe"
            src={OctopodeDotPng}
          />
        ) : (
          <div className="octo-wrapper">
            {[...Array(8)].map((_, i) => (
              <div
                key={'key-' + i}
                className={'tentacle tentacle-' + (i + 1)}
              >
                {' '}
                {divs(20)}{' '}
              </div>
            ))}
            <div className="octo-head" />
          </div>
        )}
      </Navbar>
    );
  }
}

OverlordBar.propTypes = {
  page: PropTypes.string,
  history: PropTypes.object,
  simple: PropTypes.bool,
  T: PropTypes.shape({
    /* translations.func here can be transiently undefined, sadly. */
    t: PropTypes.func,
  }),
  lo_platform: PropTypes.object,
};

function mapStateToProps(state) {
  return {
    T: state.main.translations,
    lo_platform: state.main.lo_platform,
  };
}

export default connect(mapStateToProps, null)(OverlordBar);
