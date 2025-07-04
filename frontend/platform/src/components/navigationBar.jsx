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

import axios from 'axios';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router-dom';
import {
  Dropdown,
  DropdownItem,
  DropdownMenu,
  DropdownToggle,
  Nav,
  NavItem,
  NavLink,
  Navbar,
  NavbarBrand,
} from 'reactstrap';
import { bindActionCreators } from 'redux';

import LoPropTypes from '../react/loPropTypes';
import * as MainActions from '../redux/actions/MainActions';
import { ExitUrl, LogoutUrl } from '../services/URLs';
import ClusterLabel from './clusterLabel';
import { clearSavedTableState } from './reactTable/ReactTable';
import classnames from 'classnames';

class NavigationBar extends React.Component {
  constructor() {
    super();
    this.state = {
      dropdownOpen: false,
    };
  }

  isSudoed = () => {
    const {
      lo_platform: { session },
    } = this.props;
    return session && session.sudoed;
  };

  toggle = () => {
    this.setState(prevState => ({
      dropdownOpen: !prevState.dropdownOpen,
    }));
  };

  handleExit = () => {
    const sudoed = this.isSudoed();
    if (!sudoed) clearSavedTableState();
    const promise = sudoed ? axios.post(ExitUrl, {}) : axios.post(LogoutUrl, {});
    promise
      .then(({ data }) => {
        document.location.href = data || '/';
      })
      .catch(e => console.log(e));
  };

  render() {
    const { lo_platform: platform, translations: T, nonAdmin, domainApp, noFade } = this.props;
    const { domain, isOverlord, user } = platform;
    const rights = (user && user.rights) || [];
    const canViewAuthoring =
      rights.indexOf('loi.authoring.security.right$AccessAuthoringAppRight') >= 0;
    const isAdmin = !!platform.adminLink;
    const yeahNo = T.t('APP_HEADER_ADMIN').length < 6 ? 'wut-no' : '';
    const DomainLink = domainApp ? Link : 'a';
    return Object.keys(platform).length > 0 && Object.keys(T).length > 0 ? (
      <div id="main-nav-bar">
        {isOverlord && !nonAdmin ? <ClusterLabel lo_platform={platform} /> : null}
        <Navbar
          id="main-nav-bar-base"
          light
          expand
          className={classnames('navbar-toggleable-xl px-3', noFade && 'no-fade')}
          container={false}
        >
          {domain.logo?.url ? (
            <DomainLink
              id="main-nav-bar-logo-link"
              className="lo-domain-link"
              to=""
              href="/"
            >
              <img
                id="main-nav-bar-logo"
                alt={domain.name}
                className={classnames('navbar-brand', domain.logo2?.url && 'dark-only')}
                src={domain.logo.url}
              />
              {domain.logo2?.url && (
                <img
                  id="main-nav-bar-logo2"
                  alt={domain.name}
                  className="navbar-brand light-only"
                  src={domain.logo2.url}
                />
              )}
            </DomainLink>
          ) : (
            <NavbarBrand
              id="main-nav-bar-text"
              className="lo-domain-link"
              tag={DomainLink}
              to=""
              href="/"
            >
              {domain.name}
            </NavbarBrand>
          )}
          <Nav className="ms-auto">
            <NavItem className="admin-portal-authoring-link d-none d-md-block">
              {canViewAuthoring && platform.authoringLink && (
                <NavLink
                  href={platform.authoringLink}
                  className="lo-authoring-link"
                >
                  {T.t('APP_HEADER_AUTHORING')}
                </NavLink>
              )}
            </NavItem>

            {isAdmin && (
              <NavItem className="admin-portal-admin-link d-none d-md-block">
                <NavLink
                  href="/Administration/"
                  className={`lo-admin-link ${yeahNo} ${nonAdmin ? '' : 'active'}`}
                  aria-current={nonAdmin ? null : 'location'}
                >
                  {T.t('APP_HEADER_ADMIN')}
                </NavLink>
              </NavItem>
            )}

            {user && (
              <Dropdown
                className="admin-portal-user-link"
                isOpen={this.state.dropdownOpen}
                toggle={this.toggle}
              >
                <DropdownToggle
                  nav
                  id="navbar-dropdown"
                  className="admin-portal-nav-dropdown"
                  caret
                >
                  <span className="nav-username d-none d-sm-inline-block">{user?.fullName}</span>
                  <i
                    className="material-icons d-sm-none py-2"
                    aria-hidden="true"
                  >
                    menu
                  </i>
                </DropdownToggle>
                <DropdownMenu
                  id="navbar-dropdown-menu"
                  end
                >
                  {canViewAuthoring && platform.authoringLink && (
                    <DropdownItem
                      href={platform.authoringLink}
                      className="lo-authoring-link dropdown-item admin-portal-authoring-link d-md-none"
                      role="menuitem"
                    >
                      {T.t('APP_HEADER_AUTHORING')}
                    </DropdownItem>
                  )}

                  {isAdmin && (
                    <DropdownItem
                      href="/Administration/"
                      className="dropdown-item lo-admin-link d-md-none"
                      aria-current="location"
                      role="menuitem"
                    >
                      {T.t('APP_HEADER_ADMIN')}
                    </DropdownItem>
                  )}
                  <DropdownItem
                    id="navbar-dropdown-courses"
                    tag={DomainLink}
                    to=""
                    href="/"
                  >
                    {T.t('APP_HEADER_COURSES')}
                  </DropdownItem>
                  <DropdownItem
                    id="navbar-dropdown-profile"
                    tag={DomainLink}
                    to="Profile"
                    href="/Profile"
                  >
                    {T.t('APP_HEADER_PROFILE')}
                  </DropdownItem>
                  <DropdownItem divider />
                  <DropdownItem
                    id="navbar-dropdown-logout"
                    onClick={this.handleExit}
                    className="lo-logout-link dropdown-item"
                  >
                    {this.isSudoed() ? T.t('APP_HEADER_EXIT') : T.t('APP_HEADER_LOGOUT')}
                  </DropdownItem>
                </DropdownMenu>
              </Dropdown>
            )}
          </Nav>
        </Navbar>
      </div>
    ) : null;
  }
}

NavigationBar.propTypes = {
  containerWidth: PropTypes.number,
  lo_platform: LoPropTypes.lo_platform,
  translations: LoPropTypes.translations,
  setLoPlatform: PropTypes.func,
  nonAdmin: PropTypes.bool,
  domainApp: PropTypes.bool,
  noFade: PropTypes.bool,
};

const mapStateToProps = state => ({
  translations: state.main.translations,
  lo_platform: state.main.lo_platform,
});

const mapDispatchToProps = dispatch => bindActionCreators({ ...MainActions }, dispatch);

export default connect(mapStateToProps, mapDispatchToProps)(NavigationBar);
