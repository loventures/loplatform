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

import classnames from 'classnames';
import gretchen from '../grfetchen/';
import React from 'react';
import { useDispatch } from 'react-redux';
import { Link } from 'react-router-dom';
import {
  Badge,
  DropdownItem,
  DropdownMenu,
  DropdownToggle,
  Nav,
  Navbar,
  NavbarBrand,
  NavItem,
  NavLink,
  UncontrolledDropdown,
} from 'reactstrap';

import {
  confirmSaveProjectGraphEdits,
  confirmSaveProjectGraphEditsLink,
  useGraphEditSelector,
} from '../graphEdit';
import { useDcmSelector, usePolyglot } from '../hooks';
import { PREFERENCES_OPEN, Project } from '../layout/dcmLayoutReducer';
import PresenceChat from '../presence/PresenceChat';
import PresenceService from '../presence/services/PresenceService';
import { Thunk } from '../types/dcmState';
import { Polyglot } from '../types/polyglot';
import PreferencesModal from './PreferencesModal';
import { MdMenu } from 'react-icons/md';

const Navigation: React.FC<{ hidden: boolean }> = () => {
  const { chatEnabled } = useDcmSelector(s => s.configuration);
  const title = useDcmSelector(s => s.configuration?.domain?.name);
  const logoUrl = useDcmSelector(s => s.configuration?.domain?.logo?.url);
  const logo2Url = useDcmSelector(s => s.configuration?.domain?.logo2?.url);
  const { project, platform, probableAdmin, preferencesOpen } = useDcmSelector(s => s.layout);
  const { profile: user } = useDcmSelector(s => s.user);
  const sudoed = useDcmSelector(s => s.user?.session?.sudoed || false);
  const polyglot = usePolyglot();
  const production = user?.user_type === 'Overlord' && platform?.isProduction;
  const dirty = useGraphEditSelector(state => state.dirty);
  const dispatch = useDispatch();

  return (
    <Navbar
      id="navbar"
      className={classnames(
        'grid-topnav d-flex flex-row justify-content-between align-items-center px-3 py-2',
        production && 'production'
      )}
      container={false}
    >
      <NavbarBrand
        href="/"
        className={classnames('p-0 text-dark', project?.id && 'd-none d-md-block')}
      >
        {logoUrl ? (
          <img
            className={classnames('main-navbar-logo', logo2Url && 'dark-only')}
            src={logoUrl}
            alt={title}
          />
        ) : (
          title
        )}
        {logo2Url && (
          <img
            className="main-navbar-logo light-only"
            src={logo2Url}
            alt={title}
          />
        )}
      </NavbarBrand>

      {!!project?.id && (
        <div
          className="text-truncate"
          style={{ marginBottom: 0, fontWeight: 500, fontSize: '18px', maxWidth: '30rem' }}
        >
          <div
            id="hdr-project-code"
            className="text-truncate"
            style={{ fontWeight: 400, fontSize: '.8rem', marginBottom: -3 }}
          >
            <ProjectCodeHeader
              project={project}
              polyglot={polyglot}
            />
          </div>
          <div
            id="hdr-project-name"
            className="text-truncate"
          >
            {project.archived && (
              <Badge
                id="retired-badge"
                color="warning"
                className="me-1 text-dark"
              >
                Retired
              </Badge>
            )}
            <span>{project.name}</span>
          </div>
        </div>
      )}

      <Nav className="ms-auto">
        {chatEnabled && <PresenceChat />}
        <NavItem className="d-none d-lg-block">
          <Link
            className="nav-link active"
            to="/"
            onClick={e => confirmSaveProjectGraphEditsLink(e, dirty, dispatch)}
          >
            {polyglot.t('APP_HEADER_AUTHORING')}
          </Link>
        </NavItem>
        {probableAdmin && (
          <NavItem className="d-none d-lg-block">
            <NavLink href="/Administration">{polyglot.t('APP_HEADER_ADMIN')}</NavLink>
          </NavItem>
        )}
        <UncontrolledDropdown>
          <DropdownToggle
            target="_self"
            nav
            caret
            className="pe-0"
            id="navbar-dropdown"
          >
            <span className="d-none d-sm-inline-block">
              {user?.fullName || user?.userName || polyglot.t('USER')}
            </span>
            <MdMenu
              size="1.5rem"
              className="d-sm-none"
            />
          </DropdownToggle>
          <DropdownMenu end>
            <DropdownItem
              tag={Link}
              className="d-lg-none d-flex"
              to="/"
            >
              {polyglot.t('APP_HEADER_AUTHORING')}
            </DropdownItem>
            {probableAdmin && (
              <DropdownItem
                tag="a"
                className="d-lg-none d-flex"
                href="/Administration"
              >
                {polyglot.t('APP_HEADER_ADMIN')}
              </DropdownItem>
            )}
            <DropdownItem
              tag="a"
              className="d-flex"
              href="/"
              onClick={e => confirmSaveProjectGraphEditsLink(e, dirty, dispatch)}
            >
              {polyglot.t('APP_HEADER_COURSES')}
            </DropdownItem>
            <DropdownItem
              tag="a"
              className="d-flex"
              href="/Profile"
              onClick={e => confirmSaveProjectGraphEditsLink(e, dirty, dispatch)}
            >
              {polyglot.t('APP_HEADER_PROFILE')}
            </DropdownItem>
            {/*<DropdownItem
              className="d-flex"
              onClick={() => dispatch(preferencesAction(true))}
            >
              {polyglot.t('APP_HEADER_PREFERENCES')}...
            </DropdownItem>*/}
            <DropdownItem divider />
            <DropdownItem
              className="d-flex"
              onClick={() => dispatch(logoutAction())}
            >
              {polyglot.t(sudoed ? 'APP_HEADER_EXIT' : 'APP_HEADER_LOGOUT')}
            </DropdownItem>
          </DropdownMenu>
        </UncontrolledDropdown>
      </Nav>
      {preferencesOpen && <PreferencesModal toggle={() => dispatch(preferencesAction(false))} />}
    </Navbar>
  );
};

export const preferencesAction = (open: boolean) => ({ type: PREFERENCES_OPEN, open });

export const logoutAction = (): Thunk => (dispatch, getState) => {
  const sudoed = getState().user?.session?.sudoed;
  dispatch(
    confirmSaveProjectGraphEdits(() => {
      PresenceService.stop();
      gretchen
        .post(sudoed ? '/api/v2/sessions/exit' : '/api/v2/sessions/logout')
        .exec()
        .then(({ data }) => {
          window.location = data || '/';
        });
    })
  );
};

export default Navigation;

const ProjectCodeHeader: React.FC<{
  project: Project;
  polyglot: Polyglot;
}> = ({ project, polyglot }) => {
  const configuration = useDcmSelector(state => state.configuration);
  const status = Object.entries(configuration.projectStatuses ?? {}).find(
    ([, v]) => v === project.liveVersion
  )?.[0];
  const color = configuration.projectStatusColor?.[status] ?? 'muted';

  const elements = [];
  if (project.code) {
    elements.push(
      <React.Fragment key="code">
        <span className="pe-1">{polyglot.t('PROJECT_NAV.CODE')}</span>
        <span>{project.code}</span>
      </React.Fragment>
    );
  }
  if (project.productType) {
    if (project.code) {
      elements.push(
        <span
          key="projectTypePipe"
          className="px-1"
        >
          |
        </span>
      );
    }
    elements.push(
      <React.Fragment key="projectType">
        <span className="pe-1">{polyglot.t('PROJECT_NAV.TYPE')}</span>
        <span>{project.productType}</span>
      </React.Fragment>
    );
  }
  if (project.liveVersion) {
    if (elements.length > 0) {
      elements.push(
        <span
          key="liveVersionPipe"
          className="px-1"
        >
          |
        </span>
      );
    }
    elements.push(
      <React.Fragment key="liveVersion">
        <span className="pe-1">{polyglot.t('PROJECT_NAV.LIVE')}</span>
        <span className={`text-${color}`}>{project.liveVersion}</span>
      </React.Fragment>
    );
  }

  return <>{elements}</>;
};
