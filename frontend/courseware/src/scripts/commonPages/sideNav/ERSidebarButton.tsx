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

import SideNavStateService, {
  useSidepanelOpen,
} from '../../commonPages/sideNav/SideNavStateService';
import { useTranslation } from '../../i18n/translationContext';
import React, { useEffect, useRef } from 'react';
import { FiMenu, FiX } from 'react-icons/fi';
import { Button } from 'reactstrap';
import { IoCloseOutline, IoMenu, IoMenuOutline } from 'react-icons/io5';
import { useMedia } from 'react-use';
import classnames from 'classnames';

const ERSidebarButton: React.FC<{ header?: boolean, dark?: boolean }> = ({ header, dark }) => {
  const translate = useTranslation();
  const [sideNavOpen] = useSidepanelOpen();
  const ref = useRef<HTMLButtonElement>();

  const mediumScreen = useMedia('(min-width: 48em)');

  const onClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    sideNavOpen ? SideNavStateService.closeSideNav() : SideNavStateService.openSideNav();
  };

  useEffect(() => {
    if (sideNavOpen && !mediumScreen)
      ref.current?.focus({preventScroll: true})
    // arguably should refocus the mobile header sidebar button if you use the
    // sidebar close button to close the sidebar
  }, [sideNavOpen, mediumScreen]);

  return header && !(sideNavOpen || mediumScreen) ? (
    <Button
      color="primary"
      outline={!dark}
      className={classnames("sidebar-open-button", !dark && "border-white")}
      onClick={onClick}
      aria-controls="er-sidebar"
      aria-expanded={false}
      title={translate('ER_OPEN_NAV_SIDEBAR')}
      aria-label={translate('ER_CONTENT_NAV_TOGGLE')}
    >
      <IoMenuOutline size="2rem" />
    </Button>
  ) : !header && (sideNavOpen || mediumScreen) ? (
    <Button
      innerRef={ref}
      color="primary"
      className={classnames("d-print-none", sideNavOpen ? "sidebar-close-button" : "sidebar-open-button")}
      onClick={onClick}
      aria-controls="er-sidebar"
      aria-expanded={sideNavOpen}
      title={translate(sideNavOpen ? 'ER_CLOSE_NAV_SIDEBAR' : 'ER_OPEN_NAV_SIDEBAR')}
      aria-label={translate('ER_CONTENT_NAV_TOGGLE')}
    >
      {sideNavOpen ? <IoCloseOutline size="1.5rem" /> : <IoMenuOutline size="1.5rem" />}
    </Button>
  ) : (
    <div className="sidebar-btn-esque"></div>
  );
};

export default ERSidebarButton;
