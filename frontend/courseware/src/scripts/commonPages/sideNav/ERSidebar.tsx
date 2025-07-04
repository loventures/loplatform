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

import Course from '../../bootstrap/course';
import ERContentNavItems from '../../commonPages/sideNav/ERContentNavItems';
import ERInstructorNavItems from '../../commonPages/sideNav/ERInstructorNavItems';
import ERLearnerNavItems from '../../commonPages/sideNav/ERLearnerNavItems';
import { ERSidebarLandmarkContext } from '../../commonPages/sideNav/ERSidebarLandmarkProvider';
import { LeftNavListItem } from '../../commonPages/sideNav/LeftNavListItem';
import SideNavStateService from '../../commonPages/sideNav/SideNavStateService';
import { useCourseSelector } from '../../loRedux';
import { CourseDashboardLink } from '../../utils/pageLinks';
import {
  INTERNAL_RETURN_NAME,
  INTERNAL_RETURN_URL,
} from '../../contentPlayerComponents/activityViews/resource/ResourceActivityCourseLink';
import { useTranslation } from '../../i18n/translationContext';
import { timeoutEffect } from '../../utilities/effectUtils';
import { selectCurrentUser } from '../../utilities/rootSelectors';
import React, { useCallback, useContext, useEffect } from 'react';
import { AiOutlineArrowLeft } from 'react-icons/ai';
import { FiHome } from 'react-icons/fi';
import { ListGroup } from 'reactstrap';

// TODO: when you are in a module and scroll the nav sidebar to the top, the bottom
// sticky should be covered...

/**
 * Sidebar container.
 * */
const ERSidebar: React.FC = () => {
  const translate = useTranslation();
  const user = useCourseSelector(selectCurrentUser);
  const { activeDiv, containerDiv, topPaddingDiv, bottomPaddingDiv } =
    useContext(ERSidebarLandmarkContext);

  const sidePanelOpen = SideNavStateService.showSideNav;

  // This is used to scroll the "current" element on next up navigation, plus also the focused
  // element as you tab through the sidenav - because often the focused element will
  // be hidden by the current/next module sticky.
  const scrollIntoView = useCallback(
    (element: HTMLElement) => {
      if (containerDiv && element !== bottomPaddingDiv && element !== topPaddingDiv) {
        // add top padding if there is a top sticky above you
        const topPadding =
          topPaddingDiv &&
          topPaddingDiv.compareDocumentPosition(element) & Node.DOCUMENT_POSITION_FOLLOWING
            ? topPaddingDiv.offsetHeight
            : 0;
        // add bottom padding if there is a bottom sticky below you
        const bottomPadding =
          bottomPaddingDiv &&
          bottomPaddingDiv.compareDocumentPosition(element) & Node.DOCUMENT_POSITION_PRECEDING
            ? bottomPaddingDiv.offsetHeight
            : 0;
        // This extra space makes it more obvious that there are other elements above/below you.
        // The domtastic check is because you don't need more space on the first or last nav element
        // in a group. The focus element is always an A child of LI child of UL.
        const extraSpaceAbove = element.parentNode?.previousSibling ? 15 : 0;
        const extraSpaceBelow = element.parentNode?.nextSibling ? 15 : 0;
        const maxTop = element.offsetTop - topPadding - extraSpaceAbove;
        const minTop =
          element.offsetTop +
          element.offsetHeight +
          extraSpaceBelow +
          bottomPadding -
          containerDiv.offsetHeight;
        if (containerDiv.scrollTop > maxTop) {
          containerDiv.scrollTo({ top: maxTop, behavior: 'smooth' });
        } else if (containerDiv.scrollTop < minTop) {
          containerDiv.scrollTo({ top: minTop, behavior: 'smooth' });
        }
      }
    },
    [topPaddingDiv, bottomPaddingDiv, containerDiv]
  );

  useEffect(
    timeoutEffect(() => (activeDiv ? scrollIntoView(activeDiv) : void 0), 0),
    [activeDiv, scrollIntoView, sidePanelOpen]
  );

  /* eslint jsx-a11y/no-static-element-interactions: "off" */
  return (
    <nav
      id="er-sidebar"
      className="er-sidebar-nav"
      style={{ position: 'relative', visibility: sidePanelOpen ? 'visible' : 'hidden' }}
      onFocus={e => scrollIntoView(e.target)}
      aria-label={translate('CONTENT_NAV_LABEL')}
    >
      <ListGroup flush>
        <ERInternalReturnButton />
        <LeftNavListItem to={CourseDashboardLink}>
          <FiHome
            aria-hidden={true}
            strokeWidth={1.5}
            stroke="#2e4954"
            className="me-2"
          />
          <span>{translate('ER_HOME_TITLE')}</span>
        </LeftNavListItem>
        {user.isInstructor ? <ERInstructorNavItems /> : <ERLearnerNavItems />}
        <ERContentNavItems />
      </ListGroup>
    </nav>
  );
};

const ERInternalReturnButton: React.FC = () => {
  const returnName = sessionStorage.getItem(`${INTERNAL_RETURN_NAME}:${Course.branch_id}`);
  const returnUrl = sessionStorage.getItem(`${INTERNAL_RETURN_URL}:${Course.branch_id}`);
  return returnUrl && returnName ? (
    <a
      className="btn btn-primary py-2"
      style={{ borderRadius: 0 }}
      href={returnUrl}
    >
      <AiOutlineArrowLeft className="me-2" />
      {`Back to ${returnName}`}
    </a>
  ) : null;
};

export default ERSidebar;
