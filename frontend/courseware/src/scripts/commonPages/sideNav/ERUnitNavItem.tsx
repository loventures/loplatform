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
import ERNavItemIcon from '../../commonPages/sideNav/ERNavItemIcon';
import { ERSidebarLandmarkContext } from '../../commonPages/sideNav/ERSidebarLandmarkProvider';
import useGatingTooltip from '../../commonPages/sideNav/useGatingTooltip';
import LoLink from '../../components/links/LoLink';
import { ModulePath } from '../../resources/LearningPathResource';
import { useNextUpInUnit } from '../../resources/useNextUp';
import { ContentPlayerPageLink, CoursePageLink } from '../../utils/pageLinks';
import { useTranslation } from '../../i18n/translationContext';
import React, { MouseEventHandler, useContext, useEffect, useRef } from 'react';
import { BsKey } from 'react-icons/bs';
import { IoTriangleSharp } from 'react-icons/io5';
import { ListGroupItem } from 'reactstrap';

interface ERUnitNavItemProps {
  modulePath: ModulePath;
  expanded: boolean;
  collapsed: boolean;
  setCollapsed: (collapsed: boolean) => void;
  previousExpanded: boolean;
  bookmarked: Set<string>;
}

/**
 * Side bar nav element that displays a unit. The children are all rendered
 * separately as elements in the learning path, so this just represents the
 * unit header.
 */
const ERUnitNavItem: React.FC<ERUnitNavItemProps> = ({
  modulePath,
  expanded,
  collapsed,
  setCollapsed,
  previousExpanded,
  bookmarked,
}) => {
  // This would all be a lot nicer if we had a hierarchical content tree: the handling of lessons
  // would be much more obvious.
  const { content: unit } = modulePath;
  const nextUp = useNextUpInUnit(modulePath, expanded);
  const translate = useTranslation();

  const {
    linkProps: { onClick, ...linkProps },
    iconProps,
    GatingTooltip,
    locked,
    showLockIcon,
    showKeyIcon,
    pointerEvents,
  } = useGatingTooltip(unit.id);
  const linkRef = useRef<HTMLAnchorElement>(null);
  const { setSidemark } = useContext(ERSidebarLandmarkContext);

  // these sidemarks only really have any impact if the course mixes units and modules at
  // the top level. I think the topPadding has no effect because this is not a container.
  useEffect(
    () => (expanded ? setSidemark('topPadding', linkRef.current) : void 0),
    [setSidemark, expanded, linkRef.current]
  );
  useEffect(
    () => (previousExpanded ? setSidemark('bottomPadding', linkRef.current) : void 0),
    [setSidemark, previousExpanded, linkRef.current]
  );
  const unitClick: MouseEventHandler = e => {
    if (expanded || collapsed) {
      e.preventDefault();
      e.stopPropagation();
    }
    setCollapsed(expanded);
  };

  return (
    <ListGroupItem
      className={classnames('unit-li', expanded && 'expanded', collapsed && 'collapsed')}
    >
      <LoLink
        className={classnames(
          'content-navigation-link unit-link',
          locked && 'locked',
          collapsed && 'active'
        )}
        to={nextUp ? ContentPlayerPageLink.toLink({ content: nextUp }) : CoursePageLink.toLink()}
        onClick={onClick ?? unitClick}
        linkRef={linkRef}
        {...linkProps}
      >
        <div
          className="d-flex  align-items-center w-100"
          style={{ pointerEvents }}
        >
          {expanded && (
            <IoTriangleSharp
              aria-hidden={true}
              className="back-triangle"
              title={translate('UNIT_BACK_TO_COURSE')}
            />
          )}
          <span className="clamp-title me-auto">
            {showKeyIcon && (
              <BsKey
                aria-hidden={true}
                fill="#2e4954"
                className="me-2"
              />
            )}
            {unit.name}
          </span>
          <ERNavItemIcon
            content={unit}
            bookmarked={bookmarked.has(unit.id) && !expanded}
            {...iconProps}
          />
        </div>
        {showLockIcon || showKeyIcon ? <GatingTooltip /> : null}
      </LoLink>
    </ListGroupItem>
  );
};

export default ERUnitNavItem;
